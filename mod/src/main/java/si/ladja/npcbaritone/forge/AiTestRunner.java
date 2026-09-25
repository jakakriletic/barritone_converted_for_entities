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

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * M6.9 / M6 A1, A4: vanilla AI z Baritonovim navigatorjem, brez igralca. Husk (vanilla zombi
 * AI, ne gori) je pripet <b>brez</b> {@code puppet}; vaščan z {@code NoAI} stoji za zidom z režo
 * 20 blokov stran. Husk ga mora sam najti ({@code EntityAINearestAttackableTarget}) in napasti
 * ({@code EntityAIAttackMelee} — {@code setPath}, nato {@code tryMoveToEntityLiving} vsakih
 * 4–11 tickov). Izid: tick prvega udarca, klici navigatorja in nova iskanja (A4).
 *
 * <p>Arena: zaprta proga 30 × 11 (stene višine 3), zid na x=10 z režo 1×2 na z=+3.
 */
public final class AiTestRunner {

    public static final AiTestRunner INSTANCE = new AiTestRunner();
    static final int TIMEOUT_TICKS = 600;

    private Run run;

    private AiTestRunner() {
    }

    public boolean isRunning() {
        return run != null;
    }

    private static final class Run {
        final WorldServer world;
        final ICommandSender sender;
        final EntityHusk husk;
        final EntityVillager villager;
        final BaritonePathNavigate nav;
        /** Kontrola: enak husk brez Baritona v sosednji areni (ali vanilla sploh napade). */
        final EntityHusk controlHusk;
        final EntityVillager controlVillager;
        int tick;
        int hitTick = -1;
        int controlHitTick = -1;
        final java.util.List<net.minecraftforge.common.ForgeChunkManager.Ticket> tickets = new java.util.ArrayList<>();

        Run(WorldServer world, ICommandSender sender, EntityHusk husk, EntityVillager villager, BaritonePathNavigate nav,
            EntityHusk controlHusk, EntityVillager controlVillager) {
            this.world = world;
            this.sender = sender;
            this.husk = husk;
            this.villager = villager;
            this.nav = nav;
            this.controlHusk = controlHusk;
            this.controlVillager = controlVillager;
        }
    }

    public void startAttack(WorldServer world, ICommandSender sender, BlockPos o) {
        if (run != null) {
            throw new IllegalStateException("aitest already running");
        }
        BlockPos c = o.add(0, 0, CONTROL_OFFSET_Z);
        // strežnik brez igralca razloži chunke izven spawn območja (128 blokov): obe areni prisilno naložimo
        java.util.List<net.minecraftforge.common.ForgeChunkManager.Ticket> tickets = forceChunks(world, o.add(-2, 0, -7), c.add(32, 0, 7));
        buildArena(world, o);
        buildArena(world, c);
        EntityVillager villager = villager(world, o);
        EntityHusk husk = husk(world, o);
        EntityVillager controlVillager = villager(world, c);
        EntityHusk controlHusk = husk(world, c);
        Attach.attach(husk, false, NpcBaritoneMod.config()); // vanilla AI ostane
        BaritonePathNavigate nav = (BaritonePathNavigate) husk.getNavigator();
        run = new Run(world, sender, husk, villager, nav, controlHusk, controlVillager);
        run.tickets.addAll(tickets);
        PathTrace.INSTANCE.start(java.util.Collections.singletonList(husk));
        PathTrace.INSTANCE.setTag("ai/attack");
        say(run, String.format(Locale.ROOT, "aitest attack: husk (vanilla AI + Baritone navigator) → vaščan za zidom z režo, 20 blokov; kontrola brez Baritona pri z+%d; spawnano: vaščan=%s husk=%s kontrola=%s/%s",
                CONTROL_OFFSET_Z, alive(villager), alive(husk), alive(controlVillager), alive(controlHusk)));
    }

    private static String alive(EntityLiving e) {
        return e.addedToChunk && !e.isDead ? "da" : "NE";
    }

    static java.util.List<net.minecraftforge.common.ForgeChunkManager.Ticket> forceChunks(WorldServer w, BlockPos a, BlockPos b) {
        java.util.List<net.minecraftforge.common.ForgeChunkManager.Ticket> out = new java.util.ArrayList<>();
        int per = Math.max(1, net.minecraftforge.common.ForgeChunkManager.getMaxChunkDepthFor(NpcBaritoneMod.MODID));
        net.minecraftforge.common.ForgeChunkManager.Ticket t = null;
        int n = 0;
        for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
            for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) {
                if (t == null || n >= per) {
                    t = net.minecraftforge.common.ForgeChunkManager.requestTicket(NpcBaritoneMod.INSTANCE, w, net.minecraftforge.common.ForgeChunkManager.Type.NORMAL);
                    if (t == null) {
                        return out;
                    }
                    out.add(t);
                    n = 0;
                }
                w.getChunkProvider().provideChunk(cx, cz);
                net.minecraftforge.common.ForgeChunkManager.forceChunk(t, new net.minecraft.util.math.ChunkPos(cx, cz));
                n++;
            }
        }
        return out;
    }

    static final int CONTROL_OFFSET_Z = 16;

    private static EntityVillager villager(WorldServer world, BlockPos o) {
        EntityVillager v = new EntityVillager(world);
        v.setLocationAndAngles(o.getX() + 20.5, o.getY(), o.getZ() + 0.5, 90f, 0f);
        v.setNoAI(true);
        v.enablePersistence();
        world.spawnEntity(v);
        return v;
    }

    private static EntityHusk husk(WorldServer world, BlockPos o) {
        EntityHusk h = new EntityHusk(world);
        h.setLocationAndAngles(o.getX() + 0.5, o.getY(), o.getZ() + 0.5, -90f, 0f);
        h.enablePersistence();
        world.spawnEntity(h);
        return h;
    }

    @SubscribeEvent
    public void onCanUpdate(EntityEvent.CanUpdate event) {
        Run r = run;
        if (r != null && (event.getEntity() == r.husk || event.getEntity() == r.villager
                || event.getEntity() == r.controlHusk || event.getEntity() == r.controlVillager)) {
            event.setCanUpdate(true);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        Run r = run;
        if (r == null) {
            return;
        }
        if (event.getEntity() == r.villager && event.getSource().getTrueSource() == r.husk && r.hitTick < 0) {
            r.hitTick = r.tick;
        }
        if (event.getEntity() == r.controlVillager && event.getSource().getTrueSource() == r.controlHusk && r.controlHitTick < 0) {
            r.controlHitTick = r.tick;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || run == null) {
            return;
        }
        Run r = run;
        r.world.resetUpdateEntityTick();
        r.tick++;
        if (r.tick % 20 == 0) {
            NpcBaritoneMod.LOG.info("NPCB-AITEST-DBG t={} baritone[{}] kontrola[{}]", r.tick, describe(r.husk), describe(r.controlHusk));
        }
        boolean done = r.hitTick >= 0 && (r.controlHitTick >= 0 || r.tick >= TIMEOUT_TICKS);
        if (done || r.tick >= TIMEOUT_TICKS || r.husk.isDead) {
            finish(r);
            run = null;
        }
    }

    /** Diagnostika: tarča, taski, ki tečejo, položaj, tla, navigator. */
    static String describe(EntityLiving e) {
        StringBuilder running = new StringBuilder();
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry t : e.tasks.taskEntries) {
            if (t.using) {
                running.append(t.action.getClass().getSimpleName()).append(' ');
            }
        }
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry t : e.targetTasks.taskEntries) {
            if (t.using) {
                running.append("T:").append(t.action.getClass().getSimpleName()).append(' ');
            }
        }
        String nav = "vanilla";
        if (e.getNavigator() instanceof BaritonePathNavigate) {
            BaritonePathNavigate b = (BaritonePathNavigate) e.getNavigator();
            si.ladja.npcbaritone.core.pathing.path.PathExecutor cur = b.baritone().getPathingBehavior().getCurrent();
            nav = "baritone req=" + b.requests() + " new=" + b.newSearches() + " state=" + b.status().state()
                    + (b.status().failReason().isEmpty() ? "" : "(" + b.status().failReason() + ")")
                    + " goal=" + b.status().goal()
                    + " path=" + (cur == null ? "-" : cur.getPosition() + "/" + cur.getPath().length())
                    + " inControl=" + b.baritone().getInputOverrideHandler().isInControl()
                    + " moveHelper=" + e.getMoveHelper().getClass().getSimpleName();
        }
        return String.format(Locale.ROOT, "pos=%.1f,%.1f,%.1f ground=%s dead=%s target=%s tasks=[%s] noPath=%s nav=%s",
                e.posX, e.posY, e.posZ, e.onGround, e.isDead,
                e.getAttackTarget() == null ? "-" : e.getAttackTarget().getName(), running.toString().trim(),
                e.getNavigator().noPath(), nav);
    }

    private void finish(Run r) {
        PathTrace.INSTANCE.stop();
        String result = r.hitTick >= 0 ? "HIT" : (r.husk.isDead ? "DEAD" : "TIMEOUT");
        long searches = r.nav.baritone().getPathingBehavior().searchesStarted();
        String control = r.controlHitTick >= 0 ? "HIT" : "TIMEOUT";
        String row = String.format(Locale.ROOT, "attack,%s,%d,%d,%d,%d,%.2f,%s,%d",
                result, r.hitTick, r.nav.requests(), r.nav.newSearches(), searches,
                r.husk.getDistance(r.villager), control, r.controlHitTick);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File out = new File("npcbaritone/runs", "ai-attack-" + stamp + ".csv");
        try {
            PathTrace.INSTANCE.dump(new File("npcbaritone/runs", "ai-attack-" + stamp + "-trace.csv"));
        } catch (IOException e) {
            NpcBaritoneMod.LOG.error("aitest trace", e);
        }
        try {
            Files.createDirectories(out.getAbsoluteFile().getParentFile().toPath());
            Files.write(out.toPath(), Arrays.asList("scenario,result,hit_tick,nav_requests,nav_new_searches,searches,final_distance,control_result,control_hit_tick", row),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            NpcBaritoneMod.LOG.error("cannot write {}", out, e);
        }
        say(r, String.format(Locale.ROOT, "aitest attack: %s po %d tickih; klicev navigatorja %d, novih iskanj %d, iskanj skupaj %d; kontrola (vanilla): %s po %d tickih",
                result, r.hitTick, r.nav.requests(), r.nav.newSearches(), searches, control, r.controlHitTick));
        NpcBaritoneMod.LOG.info("NPCB-AITEST-DONE scenario=attack result={} hit_tick={} nav_requests={} nav_new_searches={} searches={} control={} control_hit_tick={} csv={}",
                result, r.hitTick, r.nav.requests(), r.nav.newSearches(), searches, control, r.controlHitTick, out.getAbsolutePath());
        Attach.detach(r.husk);
        r.husk.setDead();
        r.villager.setDead();
        r.controlHusk.setDead();
        r.controlVillager.setDead();
        for (net.minecraftforge.common.ForgeChunkManager.Ticket t : r.tickets) {
            net.minecraftforge.common.ForgeChunkManager.releaseTicket(t);
        }
    }

    /** Zaprta proga x -1..31, z -6..6; tla y-1; zid x=10 z režo 1×2 na z=+3. */
    static void buildArena(WorldServer w, BlockPos o) {
        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState air = Blocks.AIR.getDefaultState();
        for (BlockPos p : BlockPos.getAllInBoxMutable(o.add(-1, -1, -6), o.add(31, 4, 6))) {
            int dx = p.getX() - o.getX();
            int dy = p.getY() - o.getY();
            int dz = p.getZ() - o.getZ();
            boolean floor = dy == -1;
            boolean wall = (dx == -1 || dx == 31 || dz == -6 || dz == 6) && dy <= 2;
            boolean mid = dx == 10 && dy <= 2 && !(dz == 3 && dy <= 1);
            w.setBlockState(p.toImmutable(), floor || wall || mid ? stone : air, 2);
        }
    }

    private static void say(Run r, String msg) {
        NpcBaritoneMod.LOG.info(msg);
        try {
            r.sender.sendMessage(new TextComponentString("[npcb] " + msg));
        } catch (RuntimeException ignored) {
            // pošiljatelj je lahko odšel
        }
    }
}
