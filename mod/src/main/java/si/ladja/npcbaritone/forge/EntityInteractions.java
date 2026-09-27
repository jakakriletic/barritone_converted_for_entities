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

package si.ladja.npcbaritone.forge;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.pathing.movement.MovementHelper;
import si.ladja.npcbaritone.core.api.pathing.movement.IMovement;
import si.ladja.npcbaritone.core.api.utils.input.Input;
import si.ladja.npcbaritone.core.pathing.path.PathExecutor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * M4.1: izvede vhod {@code CLICK_RIGHT} na entiteti (namesto {@code IPlayerController} pri
 * igralcu). Edino dovoljeno dejanje je odpiranje <b>lesenih vrat</b> in <b>ograjnih vrat</b>
 * (D-039: po nastavitvah instance, pri {@code ALL} tudi železnih) ob trenutnem premiku — nič se ne poruši ali postavi (D-015). Vrata, ki jih je odprla
 * entiteta, se zaprejo, ko jih zapusti (merilo M4 A4); vrata, ki so bila že odprta, ostanejo.
 *
 * <p>Kandidati so bloki premika ({@code dest}, {@code dest+1}, {@code src}, {@code src+1}),
 * ne blok v pogledu: NPC se obrača z omejitvijo (D-011), zato bi žarek pogleda zamujal.
 * Kliče ga {@link BaritonePathNavigate} po {@code baritone.tick()}, na strežniški niti.
 */
public final class EntityInteractions {

    /** Najmanj tickov med dvema klikoma (Baritonov {@code rightClickSpeed}). */
    private final Baritone baritone;
    private final EntityLiving entity;
    private final Set<BlockPos> openedByUs = new LinkedHashSet<>();
    private int cooldown;
    private int opened;
    private int closed;

    EntityInteractions(EntityLiving entity, Baritone baritone) {
        this.entity = entity;
        this.baritone = baritone;
    }

    public int openedCount() {
        return opened;
    }

    public int closedCount() {
        return closed;
    }

    void tick() {
        if (cooldown > 0) {
            cooldown--;
        }
        if (baritone.getInputOverrideHandler().isInputForcedDown(Input.CLICK_RIGHT) && cooldown == 0) {
            BlockPos target = findOpenable();
            if (target != null && open(entity.world, target)) {
                openedByUs.add(target);
                opened++;
                cooldown = Math.max(1, baritone.getSettings().rightClickSpeed.value);
            }
        }
        closeBehind(false);
    }

    /** Ob odpenjanju: zapri vse, kar je entiteta odprla in ni več v njej. */
    void release() {
        closeBehind(true);
        openedByUs.clear();
    }

    private BlockPos findOpenable() {
        PathExecutor current = baritone.getPathingBehavior().getCurrent();
        if (current == null) {
            return null;
        }
        int pos = current.getPosition();
        List<IMovement> movements = current.getPath().movements();
        if (pos < 0 || pos >= movements.size()) {
            return null;
        }
        IMovement m = movements.get(pos);
        List<BlockPos> candidates = new ArrayList<>(4);
        candidates.add(m.getDest());
        candidates.add(m.getDest().up());
        candidates.add(m.getSrc());
        candidates.add(m.getSrc().up());
        for (BlockPos p : candidates) {
            BlockPos lower = lowerHalf(entity.world, p);
            if (lower != null && isClosedOpenable(entity.world.getBlockState(lower), baritone.getSettings())) {
                return lower.toImmutable();
            }
        }
        return null;
    }

    /** Spodnja polovica vrat (FACING/OPEN sta samo tam) ali ograjna vrata sama; null sicer. */
    static BlockPos lowerHalf(World world, BlockPos p) {
        IBlockState s = world.getBlockState(p);
        if (s.getBlock() instanceof BlockDoor) {
            return s.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER ? p.down() : p;
        }
        if (s.getBlock() instanceof BlockFenceGate) {
            return p;
        }
        return null;
    }

    /**
     * Zaprta vrata ali ograjna vrata, ki jih instanca sme odpreti (D-015: nič drugega; D-039:
     * lesena in ograjna pri {@code npcOpenDoors}, ostala pri {@code npcOpenIronDoors}).
     */
    static boolean isClosedOpenable(IBlockState s, Settings settings) {
        Block b = s.getBlock();
        if (!MovementHelper.mayOpen(s, settings)) {
            return false;
        }
        if (b instanceof BlockDoor) {
            return !s.getValue(BlockDoor.OPEN);
        }
        return b instanceof BlockFenceGate && !s.getValue(BlockFenceGate.OPEN);
    }

    private boolean open(World world, BlockPos lower) {
        IBlockState s = world.getBlockState(lower);
        if (!isClosedOpenable(s, baritone.getSettings())) {
            return false;
        }
        setOpen(world, lower, s, true);
        return true;
    }

    private static void setOpen(World world, BlockPos lower, IBlockState s, boolean open) {
        if (s.getBlock() instanceof BlockDoor) {
            ((BlockDoor) s.getBlock()).toggleDoor(world, lower, open); // zvok in obe polovici
        } else if (s.getBlock() instanceof BlockFenceGate) {
            world.setBlockState(lower, s.withProperty(BlockFenceGate.OPEN, open), 10);
            world.playEvent(null, open ? 1008 : 1014, lower, 0);
        }
    }

    /**
     * Zapre vrata, ki jih je odprla entiteta, ko se je od njih odmaknila (škatla entitete ne
     * seka bloka ali zgornje polovice in je središče več kot 1,5 bloka stran). {@code force}
     * zapre vse, ki jih entiteta ne seka.
     */
    private void closeBehind(boolean force) {
        if (openedByUs.isEmpty()) {
            return;
        }
        AxisAlignedBB box = entity.getEntityBoundingBox().grow(0.05);
        for (Iterator<BlockPos> it = openedByUs.iterator(); it.hasNext(); ) {
            BlockPos p = it.next();
            IBlockState s = entity.world.getBlockState(p);
            boolean stillOurs = (s.getBlock() instanceof BlockDoor && s.getValue(BlockDoor.OPEN))
                    || (s.getBlock() instanceof BlockFenceGate && s.getValue(BlockFenceGate.OPEN));
            if (!stillOurs) {
                it.remove(); // nekdo drug jih je zaprl ali odstranil
                continue;
            }
            boolean inside = box.intersects(new AxisAlignedBB(p, p.add(1, 2, 1)));
            double dx = entity.posX - (p.getX() + 0.5);
            double dz = entity.posZ - (p.getZ() + 0.5);
            boolean away = dx * dx + dz * dz > 1.5 * 1.5;
            if (!inside && (away || force)) {
                setOpen(entity.world, p, s, false);
                closed++;
                it.remove();
            }
        }
    }
}
