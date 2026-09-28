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

import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import si.ladja.npcbaritone.forge.NpcBaritoneMod;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * M11.1: sonda {@code FakePlayer} v pravem strežniku ({@code /npcb probe hands [x y z]}).
 * Vse v enem ticku na glavni niti, v škatli 17 × 4 × 5 vzhodno od izhodišča (x+1 … x+17, y+1 … y+4,
 * z−2 … z+2), ki se pred in po sondi izprazni — samo na testnem svetu.
 * Izid gre v klepet, log ({@code NPCB-PROBE}) in {@code npcbaritone/runs/probe-hands-<čas>.csv}.
 *
 * <p>Vprašanja (M11 README, M11.1): (1) NPE pri preklicanem {@code BreakEvent} brez povezave,
 * (2) {@code tryHarvestBlock}, obraba orodja in {@code HarvestDropsEvent.getHarvester()},
 * (3) orientacija po rotaciji pri {@code processRightClickBlock}, (4) trdota z orodjem in
 * {@code onGround}. Sonda ni del knjižnice za porabnike; roke (M11.3) jo nadomestijo.
 */
public final class HandsProbe {

    public static final class Row {
        public final String id;
        public final boolean pass;
        public final String detail;

        Row(String id, boolean pass, String detail) {
            this.id = id;
            this.pass = pass;
            this.detail = detail;
        }
    }

    /** Poslušalec samo za čas sonde. */
    public static final class Listener {
        @Nullable EntityPlayerMP cancelFor;
        @Nullable EntityPlayerMP captureFor;
        @Nullable Object lastHarvester;
        final List<ItemStack> captured = new ArrayList<>();

        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent e) {
            if (cancelFor != null && e.getPlayer() == cancelFor) {
                e.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onDrops(BlockEvent.HarvestDropsEvent e) {
            if (captureFor == null) {
                return;
            }
            lastHarvester = e.getHarvester();
            if (e.getHarvester() == captureFor) {
                for (ItemStack s : e.getDrops()) {
                    captured.add(s.copy());
                }
                e.getDrops().clear(); // D-034: dropi v inventar, ne v svet
            }
        }
    }

    private HandsProbe() {
    }

    public static List<Row> run(WorldServer world, BlockPos origin, @Nullable GameProfile owner) {
        List<Row> rows = new ArrayList<>();
        BlockPos o = origin.add(2, 1, 0); // pošiljatelj ostane izven škatle
        clear(world, o);
        Listener l = new Listener();
        MinecraftForge.EVENT_BUS.register(l);
        try {
            GameProfile id = HandsIdentity.forOwner(owner);
            FakePlayer hands = FakePlayerFactory.get(world, id);
            boolean wasNull = hands.connection == null;
            rows.add(new Row("identiteta", owner == null || !owner.getId().equals(hands.getUniqueID()),
                    "lastnik=" + (owner == null ? "-" : owner.getId()) + " roke=" + hands.getUniqueID() + " ime=" + hands.getName()));
            HandsNetHandler.install(hands);
            rows.add(new Row("povezava", hands.connection instanceof HandsNetHandler, "prej null=" + wasNull));
            place(hands, o);

            // (1a) brez povezave: preklican BreakEvent → NPE?
            FakePlayer bare = FakePlayerFactory.get(world, new GameProfile(
                    java.util.UUID.nameUUIDFromBytes("NpcBaritone:probe-bare".getBytes(StandardCharsets.UTF_8)), "[NPCB]probe"));
            BlockPos p1 = o;
            world.setBlockState(p1, Blocks.STONE.getDefaultState());
            place(bare, p1.east(2));
            l.cancelFor = bare;
            String npe;
            boolean threw;
            try {
                boolean r = bare.connection == null && bare.interactionManager.tryHarvestBlock(p1);
                threw = false;
                npe = "brez izjeme, vrnil=" + r + " connection=" + bare.connection;
            } catch (NullPointerException ex) {
                threw = true;
                npe = "NPE (" + ex.getStackTrace()[0] + ")";
            }
            rows.add(new Row("preklic-brez-povezave", threw, (threw ? "POTRJENO " : "NI POTRJENO ") + npe
                    + "; blok=" + world.getBlockState(p1).getBlock().getRegistryName()));

            // (1b) z našo povezavo: preklic = false, blok ostane, brez izjeme
            l.cancelFor = hands;
            boolean r1;
            String ex1 = "";
            try {
                r1 = hands.interactionManager.tryHarvestBlock(p1);
            } catch (RuntimeException ex) {
                r1 = true;
                ex1 = " izjema=" + ex;
            }
            rows.add(new Row("preklic-s-povezavo", !r1 && world.getBlockState(p1).getBlock() == Blocks.STONE && ex1.isEmpty(),
                    "vrnil=" + r1 + " blok=" + world.getBlockState(p1).getBlock().getRegistryName() + ex1));
            l.cancelFor = null;

            // (2) rušenje s krampom: drop pri harvesterju, obraba, nič v svetu
            BlockPos p2 = o.east(1);
            world.setBlockState(p2, Blocks.STONE.getDefaultState());
            ItemStack pick = new ItemStack(Items.STONE_PICKAXE);
            hands.setHeldItem(EnumHand.MAIN_HAND, pick);
            l.captureFor = hands;
            boolean r2 = hands.interactionManager.tryHarvestBlock(p2);
            ItemStack held = hands.getHeldItemMainhand();
            List<EntityItem> inWorld = world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(p2).grow(1.5));
            boolean cobble = l.captured.size() == 1 && l.captured.get(0).getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.COBBLESTONE);
            rows.add(new Row("rusenje-kramp", r2 && world.isAirBlock(p2) && held.getItemDamage() == 1 && cobble && inWorld.isEmpty()
                            && l.lastHarvester == hands,
                    "vrnil=" + r2 + " zrak=" + world.isAirBlock(p2) + " obraba=" + held.getItemDamage() + " zajeto=" + l.captured
                            + " harvester=" + (l.lastHarvester == hands ? "roke" : String.valueOf(l.lastHarvester)) + " EntityItem=" + inWorld.size()));
            l.captureFor = null;
            hands.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);

            // (4) trdota kamna: roka / lesen kramp; na tleh / v zraku
            BlockPos p4 = o.east(3);
            world.setBlockState(p4, Blocks.STONE.getDefaultState());
            IBlockState stone = world.getBlockState(p4);
            float hand = hardness(hands, stone, world, p4, ItemStack.EMPTY, true);
            float wood = hardness(hands, stone, world, p4, new ItemStack(Items.WOODEN_PICKAXE), true);
            float woodAir = hardness(hands, stone, world, p4, new ItemStack(Items.WOODEN_PICKAXE), false);
            float expHand = 1.0F / 1.5F / 100.0F, expWood = 2.0F / 1.5F / 30.0F, expWoodAir = 2.0F / 5.0F / 1.5F / 30.0F;
            rows.add(new Row("trdota", near(hand, expHand) && near(wood, expWood) && near(woodAir, expWoodAir),
                    String.format(java.util.Locale.ROOT, "roka=%.5f (%d t, pric. %.5f) lesen=%.5f (%d t, pric. %.5f) lesen-zrak=%.5f (%d t, pric. %.5f)",
                            hand, ticks(hand), expHand, wood, ticks(wood), expWood, woodAir, ticks(woodAir), expWoodAir)));

            // (3) stopnice: FACING = vodoravna smer pogleda
            StringBuilder st = new StringBuilder();
            boolean stairsOk = true;
            float[] yaws = {0F, 90F, 180F, 270F};
            for (int i = 0; i < yaws.length; i++) {
                BlockPos base = o.east(5 + i);
                world.setBlockState(base, Blocks.STONE.getDefaultState());
                hands.setPositionAndRotation(base.getX() + 0.5, base.getY() + 1, base.getZ() - 1.5, yaws[i], 0F);
                ItemStack stairs = new ItemStack(Blocks.OAK_STAIRS);
                hands.setHeldItem(EnumHand.MAIN_HAND, stairs);
                hands.interactionManager.processRightClickBlock(hands, world, stairs, EnumHand.MAIN_HAND, base, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
                IBlockState s = world.getBlockState(base.up());
                EnumFacing want = EnumFacing.fromAngle(yaws[i]);
                EnumFacing got = s.getBlock() == Blocks.OAK_STAIRS ? s.getValue(BlockStairs.FACING) : null;
                boolean ok = want == got && hands.getHeldItemMainhand().isEmpty();
                stairsOk &= ok;
                st.append("yaw").append((int) yaws[i]).append('=').append(got).append("/pric.").append(want)
                        .append(" porabljeno=").append(hands.getHeldItemMainhand().isEmpty()).append("; ");
            }
            rows.add(new Row("stopnice", stairsOk, st.toString()));

            // (3b) hlod na vzhodno ploskev: os X (odvisno od ploskve, ne od pogleda)
            BlockPos lb = o.east(10);
            world.setBlockState(lb, Blocks.STONE.getDefaultState());
            ItemStack log = new ItemStack(Blocks.LOG);
            hands.setHeldItem(EnumHand.MAIN_HAND, log);
            hands.interactionManager.processRightClickBlock(hands, world, log, EnumHand.MAIN_HAND, lb, EnumFacing.EAST, 1.0F, 0.5F, 0.5F);
            IBlockState ls = world.getBlockState(lb.east());
            Object axis = ls.getBlock() == Blocks.LOG ? ls.getValue(BlockLog.LOG_AXIS) : null;
            rows.add(new Row("hlod", axis == BlockLog.EnumAxis.X, "os=" + axis));
            hands.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        } catch (RuntimeException ex) {
            rows.add(new Row("izjema", false, ex.toString()));
            NpcBaritoneMod.LOG.error("NPCB-PROBE izjema", ex);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(l);
            clear(world, o);
        }
        return rows;
    }

    private static float hardness(FakePlayer p, IBlockState s, WorldServer w, BlockPos pos, ItemStack tool, boolean onGround) {
        p.setHeldItem(EnumHand.MAIN_HAND, tool);
        p.onGround = onGround;
        float h = s.getPlayerRelativeBlockHardness(p, w, pos);
        p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        p.onGround = false;
        return h;
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 1e-6F;
    }

    /** Vanilla: napredek se prišteje vsak tick, blok se poruši pri ≥ 1. */
    private static int ticks(float perTick) {
        return perTick <= 0 ? -1 : (int) Math.ceil(1.0 / perTick);
    }

    private static void place(FakePlayer p, BlockPos at) {
        p.setPositionAndRotation(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
    }

    private static void clear(WorldServer w, BlockPos o) {
        for (BlockPos p : BlockPos.getAllInBox(o.add(-1, 0, -2), o.add(15, 3, 2))) {
            w.setBlockToAir(p);
        }
    }

    public static File writeCsv(List<Row> rows) throws IOException {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File out = new File("npcbaritone/runs", "probe-hands-" + stamp + ".csv");
        List<String> lines = new ArrayList<>();
        lines.add("korak,izid,podrobnosti");
        for (Row r : rows) {
            lines.add(r.id + "," + (r.pass ? "OK" : "NAPAKA") + ",\"" + r.detail.replace("\"", "'") + "\"");
        }
        Files.createDirectories(out.getAbsoluteFile().getParentFile().toPath());
        Files.write(out.toPath(), lines, StandardCharsets.UTF_8);
        return out;
    }
}
