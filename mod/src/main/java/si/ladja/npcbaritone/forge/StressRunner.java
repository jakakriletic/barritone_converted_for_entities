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

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.SearchExecutor;
import si.ladja.npcbaritone.core.SearchStats;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;
import si.ladja.npcbaritone.core.pathing.path.PathExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * M5.7/M5.8: stresni tečaj T4. Na ravnem svetu okoli izhodišča postavi ovire (fiksno seme),
 * prikliče {@code count} huskov kot puppet in jim ves čas daje naključne cilje v polmeru
 * {@code radius}. Po {@value #WARMUP_TICKS} tickih ogrevanja (JIT, prva iskanja) se števci
 * ponastavijo in meri {@code seconds} sekund. Če je {@code breakEvery > 0}, vsakih toliko
 * sekund postavi kamen na prihodnjo točko poti naključnega NPC-ja (in prejšnjega odstrani),
 * kar sproži ponovna iskanja (M5.8, D-013).
 *
 * <p>Izid: ena vrstica CSV v {@code npcbaritone/runs/t4-<n>-<čas>.csv} in vrstica
 * {@code NPCB-STRESS-DONE} v logu. Po koncu se NPC-ji odstranijo, vozovnice sprostijo.
 */
public final class StressRunner {

    public static final StressRunner INSTANCE = new StressRunner();

    static final int WARMUP_TICKS = 200;
    static final int REGOAL_IDLE_TICKS = 10;
    static final int GOAL_TIMEOUT_TICKS = 1200;
    static final long SEED = 20260925L;

    public static final String HEADER = "mobs,seconds,threads,goals,reached,failed,timeouts,searches,searches_per_s,rejected,shared,"
            + "search_us_p50,search_us_p95,queue_us_p50,queue_us_p95,snapshot_us_p50,snapshot_us_p95,snapshot_chunks_p50,"
            + "main_us_p50,main_us_p95,main_us_p99,main_us_max,mspt_p50,mspt_p95,threads_created,block_toggles";

    private Run run;

    private StressRunner() {
    }

    public boolean isRunning() {
        return run != null;
    }

    private static final class Npc {
        final EntityLiving entity;
        final Baritone baritone;
        final NavStatus status;
        int goalTick = -GOAL_TIMEOUT_TICKS;
        NavStatus.State lastState = NavStatus.State.IDLE;

        Npc(EntityLiving entity, Baritone baritone, NavStatus status) {
            this.entity = entity;
            this.baritone = baritone;
            this.status = status;
        }
    }

    private static final class Run {
        final WorldServer world;
        final ICommandSender sender;
        final BlockPos origin;
        final int count;
        final int radius;
        final int seconds;
        final int breakEvery;
        final Random random = new Random(SEED);
        final List<Npc> npcs = new ArrayList<>();
        final Set<EntityLiving> entities = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        final List<ForgeChunkManager.Ticket> tickets = new ArrayList<>();
        int tick;
        int goals, reached, failed, timeouts, toggles;
        BlockPos placed;

        Run(WorldServer world, ICommandSender sender, BlockPos origin, int count, int radius, int seconds, int breakEvery) {
            this.world = world;
            this.sender = sender;
            this.origin = origin;
            this.count = count;
            this.radius = radius;
            this.seconds = seconds;
            this.breakEvery = breakEvery;
        }

        boolean measuring() {
            return tick >= WARMUP_TICKS;
        }
    }

    /** @return število priklicanih NPC-jev */
    public int start(WorldServer world, ICommandSender sender, BlockPos origin, int count, int radius, int seconds, int breakEvery) {
        if (run != null) {
            throw new IllegalStateException("stress already running");
        }
        Run r = new Run(world, sender, origin, count, radius, seconds, breakEvery);
        forceChunks(r);
        int obstacles = buildObstacles(r);
        NpcbConfig config = NpcBaritoneMod.config();
        for (int i = 0; i < count; i++) {
            BlockPos p = freeSpot(r);
            if (p == null) {
                continue;
            }
            EntityHusk husk = new EntityHusk(world);
            husk.setLocationAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.random.nextFloat() * 360f, 0f);
            husk.enablePersistence();
            if (!world.spawnEntity(husk)) {
                continue;
            }
            Baritone b = Attach.attach(husk, true, config);
            r.npcs.add(new Npc(husk, b, Attach.status(husk)));
            r.entities.add(husk);
        }
        run = r;
        say(r, String.format(Locale.ROOT, "T4 začet: %d NPC-jev, polmer %d, ovir %d, ogrevanje %d s, meritev %d s, rušenje vsakih %d s, niti %d",
                r.npcs.size(), radius, obstacles, WARMUP_TICKS / 20, seconds, breakEvery, SearchExecutor.threads()));
        return r.npcs.size();
    }

    public void abort(String why) {
        Run r = run;
        if (r != null) {
            say(r, "T4 prekinjen: " + why);
            cleanup(r);
            run = null;
        }
    }

    @SubscribeEvent
    public void onCanUpdate(EntityEvent.CanUpdate event) {
        Run r = run;
        if (r != null && r.entities.contains(event.getEntity())) {
            event.setCanUpdate(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || run == null) {
            return;
        }
        Run r = run;
        r.world.resetUpdateEntityTick();
        if (r.tick == WARMUP_TICKS) {
            SearchStats.reset();
            PerfMeter.INSTANCE.reset();
            si.ladja.npcbaritone.core.PerfProfile.reset();
            r.goals = r.reached = r.failed = r.timeouts = r.toggles = 0;
            say(r, "T4 ogrevanje končano, meritev teče");
        }
        for (Npc n : r.npcs) {
            if (n.entity.isDead) {
                continue;
            }
            NavStatus.State s = n.status.state();
            if (s != n.lastState) {
                if (s == NavStatus.State.ARRIVED) {
                    r.reached++;
                } else if (s == NavStatus.State.FAILED) {
                    r.failed++;
                }
                n.lastState = s;
            }
            boolean idle = s == NavStatus.State.IDLE || s == NavStatus.State.ARRIVED || s == NavStatus.State.FAILED;
            boolean stale = r.tick - n.goalTick > GOAL_TIMEOUT_TICKS;
            if ((idle && r.tick - n.goalTick >= REGOAL_IDLE_TICKS) || stale) {
                if (stale && !idle) {
                    r.timeouts++;
                }
                int x = r.origin.getX() + r.random.nextInt(2 * r.radius + 1) - r.radius;
                int z = r.origin.getZ() + r.random.nextInt(2 * r.radius + 1) - r.radius;
                n.baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(x, r.origin.getY(), z));
                n.goalTick = r.tick;
                r.goals++;
            }
        }
        if (r.breakEvery > 0 && r.tick > 0 && r.tick % (r.breakEvery * 20) == 0) {
            toggleBlock(r);
        }
        r.tick++;
        if (r.tick >= WARMUP_TICKS + r.seconds * 20) {
            finish(r);
            run = null;
        }
    }

    /** M5.8: prejšnji kamen stran, nov kamen na točko 3–8 naprej po poti naključnega NPC-ja. */
    private static void toggleBlock(Run r) {
        if (r.placed != null) {
            r.world.setBlockToAir(r.placed);
            r.placed = null;
            r.toggles++;
        }
        for (int attempt = 0; attempt < 10 && !r.npcs.isEmpty(); attempt++) {
            Npc n = r.npcs.get(r.random.nextInt(r.npcs.size()));
            PathExecutor cur = n.baritone.getPathingBehavior().getCurrent();
            if (cur == null) {
                continue;
            }
            List<? extends BlockPos> pos = cur.getPath().positions();
            int i = cur.getPosition() + 3 + r.random.nextInt(6);
            if (i >= pos.size()) {
                continue;
            }
            BlockPos p = pos.get(i);
            if (r.world.isAirBlock(p) && r.world.getEntitiesWithinAABBExcludingEntity(null, new net.minecraft.util.math.AxisAlignedBB(p)).isEmpty()) {
                r.world.setBlockState(p, Blocks.STONE.getDefaultState());
                r.placed = p.toImmutable();
                r.toggles++;
                return;
            }
        }
    }

    private void finish(Run r) {
        long[] search = SearchStats.SEARCH_NANOS.sorted();
        long[] wait = SearchStats.QUEUE_WAIT_NANOS.sorted();
        long[] snap = SearchStats.SNAPSHOT_NANOS.sorted();
        long[] main = PerfMeter.INSTANCE.mainNanosPerTick.sorted();
        long[] mspt = PerfMeter.INSTANCE.serverTickNanos.sorted();
        long searches = SearchStats.SUBMITTED.get();
        String row = String.format(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%d,%d,%.1f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.2f,%.2f,%d,%d",
                r.npcs.size(), r.seconds, SearchExecutor.threads(), r.goals, r.reached, r.failed, r.timeouts, searches,
                searches / (double) r.seconds, SearchStats.REJECTED.get(), SearchStats.SHARED.get(),
                us(search, 50), us(search, 95), us(wait, 50), us(wait, 95), us(snap, 50), us(snap, 95),
                SearchStats.SNAPSHOT_CHUNKS.percentile(50),
                us(main, 50), us(main, 95), us(main, 99), PerfMeter.INSTANCE.maxNanos() / 1000,
                SearchStats.Samples.percentile(mspt, 50) / 1e6, SearchStats.Samples.percentile(mspt, 95) / 1e6,
                SearchExecutor.threadsCreated(), r.toggles);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File out = new File("npcbaritone/runs", "t4-" + r.npcs.size() + "-" + stamp + ".csv");
        try {
            Files.createDirectories(out.getAbsoluteFile().getParentFile().toPath());
            Files.write(out.toPath(), Arrays.asList(HEADER, row), StandardCharsets.UTF_8);
        } catch (IOException e) {
            NpcBaritoneMod.LOG.error("cannot write {}", out, e);
        }
        if (si.ladja.npcbaritone.core.PerfProfile.enabled) {
            long mainNs = 0;
            for (long v : main) {
                mainNs += v;
            }
            java.util.List<String> prof = si.ladja.npcbaritone.core.PerfProfile.report(PerfMeter.INSTANCE.ticks(), mainNs);
            File pf = new File("npcbaritone/runs", "profile-" + r.npcs.size() + "-" + stamp + ".csv");
            try {
                Files.write(pf.toPath(), prof, StandardCharsets.UTF_8);
            } catch (IOException e) {
                NpcBaritoneMod.LOG.error("cannot write {}", pf, e);
            }
            for (int i = 1; i < Math.min(prof.size(), 16); i++) {
                NpcBaritoneMod.LOG.info("NPCB-PROFILE {}", prof.get(i));
            }
        }
        say(r, "T4 končan: " + SearchStats.summary());
        say(r, "T4 " + PerfMeter.INSTANCE.summary());
        NpcBaritoneMod.LOG.info("NPCB-STRESS-DONE mobs={} main_us_p95={} main_us_p50={} snapshot_us_p95={} csv={}",
                r.npcs.size(), us(main, 95), us(main, 50), us(snap, 95), out.getAbsolutePath());
        cleanup(r);
    }

    private static long us(long[] sorted, double p) {
        return SearchStats.Samples.percentile(sorted, p) / 1000;
    }

    private static void cleanup(Run r) {
        for (Npc n : r.npcs) {
            Attach.detach(n.entity);
            n.entity.setDead();
        }
        if (r.placed != null) {
            r.world.setBlockToAir(r.placed);
        }
        for (ForgeChunkManager.Ticket t : r.tickets) {
            ForgeChunkManager.releaseTicket(t);
        }
        r.tickets.clear();
    }

    /** Vsi chunki območja + rob posnetka ostanejo naloženi (strežnik brez igralca). */
    private static void forceChunks(Run r) {
        int margin = NpcBaritoneMod.config().snapshotMarginChunks;
        int cx0 = ((r.origin.getX() - r.radius) >> 4) - 1;
        int cx1 = ((r.origin.getX() + r.radius) >> 4) + 1;
        int cz0 = ((r.origin.getZ() - r.radius) >> 4) - 1;
        int cz1 = ((r.origin.getZ() + r.radius) >> 4) + 1;
        int perTicket = Math.max(1, ForgeChunkManager.getMaxChunkDepthFor(NpcBaritoneMod.MODID));
        ForgeChunkManager.Ticket ticket = null;
        int inTicket = 0;
        for (int cx = cx0 - margin; cx <= cx1 + margin; cx++) {
            for (int cz = cz0 - margin; cz <= cz1 + margin; cz++) {
                r.world.getChunkProvider().provideChunk(cx, cz);
                if (cx < cx0 || cx > cx1 || cz < cz0 || cz > cz1) {
                    continue; // rob: naložen, ne prisiljen
                }
                if (ticket == null || inTicket >= perTicket) {
                    ticket = ForgeChunkManager.requestTicket(NpcBaritoneMod.INSTANCE, r.world, ForgeChunkManager.Type.NORMAL);
                    if (ticket == null) {
                        throw new IllegalStateException("T4: ni več Forge chunk vozovnic");
                    }
                    r.tickets.add(ticket);
                    inTicket = 0;
                }
                ForgeChunkManager.forceChunk(ticket, new ChunkPos(cx, cz));
                inTicket++;
            }
        }
    }

    /** Stebri 1×1×(2–3) na ~4 % površine in zidovi dolžine 5–12, višine 3 (fiksno seme). */
    private static int buildObstacles(Run r) {
        Random rnd = new Random(SEED ^ r.radius);
        int n = 0;
        int area = (2 * r.radius + 1) * (2 * r.radius + 1);
        for (int i = 0; i < area / 25; i++) {
            BlockPos p = r.origin.add(rnd.nextInt(2 * r.radius + 1) - r.radius, 0, rnd.nextInt(2 * r.radius + 1) - r.radius);
            int h = 2 + rnd.nextInt(2);
            for (int y = 0; y < h; y++) {
                r.world.setBlockState(p.up(y), Blocks.STONE.getDefaultState(), 2);
            }
            n++;
        }
        for (int i = 0; i < r.radius / 4; i++) {
            BlockPos p = r.origin.add(rnd.nextInt(2 * r.radius + 1) - r.radius, 0, rnd.nextInt(2 * r.radius + 1) - r.radius);
            int len = 5 + rnd.nextInt(8);
            boolean alongX = rnd.nextBoolean();
            for (int k = 0; k < len; k++) {
                BlockPos q = alongX ? p.add(k, 0, 0) : p.add(0, 0, k);
                for (int y = 0; y < 3; y++) {
                    r.world.setBlockState(q.up(y), Blocks.STONE.getDefaultState(), 2);
                }
            }
            n++;
        }
        return n;
    }

    private static BlockPos freeSpot(Run r) {
        for (int attempt = 0; attempt < 30; attempt++) {
            int rr = Math.max(1, (int) (r.radius * 0.8));
            BlockPos p = r.origin.add(r.random.nextInt(2 * rr + 1) - rr, 0, r.random.nextInt(2 * rr + 1) - rr);
            if (r.world.isAirBlock(p) && r.world.isAirBlock(p.up()) && !r.world.isAirBlock(p.down())) {
                return p;
            }
        }
        return null;
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
