/*
 * This file is part of NPC Baritone.
 *
 * NPC Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NPC Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NPC Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package si.ladja.npcbaritone.forge.work;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import si.ladja.npcbaritone.api.work.IWorkerListener;
import si.ladja.npcbaritone.api.work.WorkerSpec;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.utils.Rotation;
import si.ladja.npcbaritone.core.api.utils.input.Input;
import si.ladja.npcbaritone.forge.NpcBaritoneMod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * M11.3 (D-032): roke workerja — izvedejo vhoda {@code CLICK_LEFT} (rušenje) in {@code CLICK_RIGHT}
 * (postavljanje), ki ju nastavijo Baritonovi premiki, kot {@code BlockBreakHelper} in
 * {@code BlockPlaceHelper} pri igralcu. Samo registrirani workerji; ne-worker nima rok.
 *
 * <p><b>Rušenje</b> kot vanilla {@code PlayerControllerMP}: napredek
 * {@code getPlayerRelativeBlockHardness} rok na tick, blok pade pri ≥ 1, po nehipnem rušenju
 * 5 tickov premora; razpoke {@code sendBlockBreakProgress} z ID-jem entitete; zaključek
 * {@code tryHarvestBlock} ({@code BreakEvent}, orodje, obraba, fortune/silk touch). Ob začetku
 * bloka {@code LeftClickBlock} (zaščitni modi). <b>Postavljanje</b> prek
 * {@code processRightClickBlock} s pritisnjenim sneakom (klik ne aktivira skrinje ipd.).
 *
 * <p>Pred vsakim dejanjem: {@link WorkGuards}, {@code isBlockModifiable} (spawn, meja sveta),
 * rokam sinhronizirani položaj oči, rotacija in {@code onGround} (RAZISKAVA §10). Predmet se vzame
 * iz inventarja za eno dejanje in se v istem ticku vrne; dropi gredo v inventar po vrnitvi orodja
 * (D-034). Objekt ne hrani entitete (R-25).
 */
public final class EntityHands {

    /** Vanilla {@code PlayerControllerMP.blockHitDelay} po nehipnem rušenju. */
    static final int HIT_DELAY = 5;

    @Nullable
    private BlockPos breakPos;
    private float progress;
    private int ticksOnBlock;
    private int lastStage = -1;
    private int hitDelay;
    private int placeCooldown;
    @Nullable
    private BlockPos refusedPos;
    @Nullable
    private String lastRefusal;
    private int broken;
    private int placed;

    public int brokenCount() {
        return broken;
    }

    public int placedCount() {
        return placed;
    }

    @Nullable
    public String lastRefusal() {
        return lastRefusal;
    }

    void tick(EntityLiving e, Baritone b, WorkerSpec spec, IHandsInventory inv, List<IWorkerListener> listeners, boolean doorUsed) {
        if (!(e.world instanceof WorldServer)) {
            return;
        }
        WorldServer world = (WorldServer) e.world;
        if (hitDelay > 0) {
            hitDelay--;
        }
        if (placeCooldown > 0) {
            placeCooldown--;
        }
        boolean left = b.getInputOverrideHandler().isInputForcedDown(Input.CLICK_LEFT);
        boolean right = !left && !doorUsed && b.getInputOverrideHandler().isInputForcedDown(Input.CLICK_RIGHT);
        RayTraceResult tr = left || right ? b.getEntityContext().objectMouseOver() : null;
        boolean blockHit = tr != null && tr.typeOfHit == RayTraceResult.Type.BLOCK && tr.getBlockPos() != null;
        if (left && blockHit) {
            breakTick(e, b, world, spec, inv, listeners, tr);
        } else {
            abort(e);
        }
        if (right && blockHit && placeCooldown == 0) {
            placeTick(e, b, world, spec, inv, listeners, tr);
        }
    }

    /** Ob odjavi, premoru ali ko vhod izgine: razpoke izginejo, napredek se ponastavi. */
    void abort(EntityLiving e) {
        if (breakPos != null && e.world != null) {
            e.world.sendBlockBreakProgress(e.getEntityId(), breakPos, -1);
        }
        breakPos = null;
        progress = 0;
        ticksOnBlock = 0;
        lastStage = -1;
    }

    private void breakTick(EntityLiving e, Baritone b, WorldServer world, WorkerSpec spec, IHandsInventory inv,
                           List<IWorkerListener> listeners, RayTraceResult tr) {
        BlockPos pos = tr.getBlockPos().toImmutable();
        if (!pos.equals(breakPos)) {
            abort(e);
            breakPos = pos;
            refusedPos = null; // nov cilj: zavrnitev prejšnjega ne velja
        }
        if (hitDelay > 0 || pos.equals(refusedPos)) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        String refuse = WorkGuards.refuseBreak(spec, world, pos, state);
        FakePlayer hands = HandsPool.INSTANCE.get(world, spec.owner());
        sync(hands, e, b.getEntityContext().entityRotations());
        if (refuse == null && !world.isBlockModifiable(hands, pos)) {
            refuse = WorkGuards.WORLD;
        }
        if (refuse == null && ticksOnBlock == 0 && ForgeHooks.onLeftClickBlock(hands, pos, tr.sideHit, tr.hitVec).isCanceled()) {
            refuse = WorkGuards.EVENT;
        }
        if (refuse != null) {
            refused(e, pos, refuse);
            return;
        }
        Consumer<ItemStack> spill = s -> spill(world, pos, s);
        ItemStack tool = inv.takeTool(state);
        List<ItemStack> drops = null;
        boolean harvested = false;
        try {
            hands.setHeldItem(EnumHand.MAIN_HAND, tool);
            progress += state.getPlayerRelativeBlockHardness(hands, world, pos);
            ticksOnBlock++;
            e.swingArm(EnumHand.MAIN_HAND);
            if (progress >= 1.0F) {
                boolean instant = ticksOnBlock == 1;
                HandsPool.INSTANCE.beginCapture(hands);
                try {
                    harvested = hands.interactionManager.tryHarvestBlock(pos);
                } finally {
                    drops = HandsPool.INSTANCE.endCapture();
                }
                abort(e);
                if (!instant) {
                    hitDelay = HIT_DELAY;
                }
                if (!harvested) {
                    breakPos = pos;
                    refused(e, pos, WorkGuards.EVENT); // BreakEvent preklican: blok ostane
                }
            } else {
                int stage = (int) (progress * 10.0F) - 1;
                if (stage != lastStage) {
                    world.sendBlockBreakProgress(e.getEntityId(), pos, stage);
                    lastStage = stage;
                }
            }
        } finally {
            ItemStack back = hands.getHeldItemMainhand();
            hands.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            if (!tool.isEmpty() && back.isEmpty()) {
                fire(listeners, l -> l.onToolBroken(e, tool));
            }
            boolean fits = inv.putBack(back, spill);
            if (drops != null) {
                for (ItemStack d : drops) {
                    fits &= inv.putBack(d, spill);
                }
            }
            if (!fits) {
                fire(listeners, l -> l.onInventoryFull(e));
            }
        }
        if (harvested) {
            broken++;
            fire(listeners, l -> l.onBlockBroken(e, pos, state));
        }
    }

    private void placeTick(EntityLiving e, Baritone b, WorldServer world, WorkerSpec spec, IHandsInventory inv,
                           List<IWorkerListener> listeners, RayTraceResult tr) {
        BlockPos clicked = tr.getBlockPos().toImmutable();
        IBlockState cs = world.getBlockState(clicked);
        if (cs.getBlock() instanceof BlockDoor || cs.getBlock() instanceof BlockFenceGate || cs.getBlock() instanceof BlockTrapDoor) {
            return; // vrata so stvar EntityInteractions (M4.1); nič se ne postavi ob njih
        }
        BlockPos target = cs.getBlock().isReplaceable(world, clicked) ? clicked : clicked.offset(tr.sideHit);
        String refuse = WorkGuards.refusePlace(spec, target);
        FakePlayer hands = HandsPool.INSTANCE.get(world, spec.owner());
        sync(hands, e, b.getEntityContext().entityRotations());
        if (refuse == null && !world.isBlockModifiable(hands, target)) {
            refuse = WorkGuards.WORLD;
        }
        if (refuse != null) {
            if (!reasonLogged(target, refuse)) {
                NpcBaritoneMod.LOG.debug("NPCB-HANDS {} postavljanje zavrnjeno {} {}", e.getName(), target, refuse);
            }
            lastRefusal = refuse;
            return;
        }
        ItemStack block = inv.takeThrowaway();
        if (block.isEmpty()) {
            return; // premik bo potekel ali ga iskanje (M11.5) ne bo več izbralo
        }
        EnumActionResult r;
        try {
            hands.setHeldItem(EnumHand.MAIN_HAND, block);
            hands.setSneaking(true);
            Vec3d hv = tr.hitVec;
            r = hands.interactionManager.processRightClickBlock(hands, world, block, EnumHand.MAIN_HAND, clicked, tr.sideHit,
                    (float) (hv.x - clicked.getX()), (float) (hv.y - clicked.getY()), (float) (hv.z - clicked.getZ()));
        } finally {
            ItemStack back = hands.getHeldItemMainhand();
            hands.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            hands.setSneaking(false);
            if (!inv.putBack(back, s -> spill(world, e.getPosition(), s))) {
                fire(listeners, l -> l.onInventoryFull(e));
            }
        }
        placeCooldown = Math.max(1, b.getSettings().rightClickSpeed.value);
        if (r == EnumActionResult.SUCCESS) {
            placed++;
            e.swingArm(EnumHand.MAIN_HAND);
            IBlockState now = world.getBlockState(target);
            fire(listeners, l -> l.onBlockPlaced(e, target, now));
        }
    }

    /** Zavrnjen blok ostane zavrnjen, dokler roke ciljajo nanj (brez ponovnih eventov vsak tick). */
    private void refused(EntityLiving e, BlockPos pos, String reason) {
        if (lastStage >= 0) {
            e.world.sendBlockBreakProgress(e.getEntityId(), pos, -1);
        }
        progress = 0;
        ticksOnBlock = 0;
        lastStage = -1;
        if (!reasonLogged(pos, reason)) {
            NpcBaritoneMod.LOG.debug("NPCB-HANDS {} rušenje zavrnjeno {} {}", e.getName(), pos, reason);
        }
        refusedPos = pos;
        lastRefusal = reason;
    }

    @Nullable
    private BlockPos loggedPos;

    private boolean reasonLogged(BlockPos pos, String reason) {
        boolean same = pos.equals(loggedPos) && reason.equals(lastRefusal);
        loggedPos = pos;
        return same;
    }

    /** RAZISKAVA §10/3–4: oči rok = oči entitete, rotacija pogleda, {@code onGround}. */
    static void sync(FakePlayer hands, EntityLiving e, Rotation rot) {
        double eyeY = e.posY + e.getEyeHeight() - hands.getEyeHeight();
        hands.setLocationAndAngles(e.posX, eyeY, e.posZ, rot.getYaw(), rot.getPitch());
        hands.rotationYawHead = rot.getYaw();
        hands.onGround = e.onGround;
    }

    private static void spill(WorldServer world, BlockPos at, ItemStack s) {
        EntityItem item = new EntityItem(world, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, s);
        item.setDefaultPickupDelay();
        world.spawnEntity(item);
    }

    private static void fire(List<IWorkerListener> listeners, Consumer<IWorkerListener> call) {
        for (IWorkerListener l : listeners) {
            try {
                call.accept(l);
            } catch (RuntimeException ex) {
                NpcBaritoneMod.LOG.error("IWorkerListener {} failed", l, ex);
            }
        }
    }
}
