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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * M2.9: zažene tečaj T1 na pripeti entiteti — odsek za odsekom (teleport na start, cilj,
 * čakanje na izid) — in zapiše CSV v {@code <strežnik>/npcbaritone/runs/}. Konec teka je
 * v logu označen z vrstico {@code NPCB-COURSE-DONE}, ki jo bere {@code t1-run.ps1}.
 *
 * <p>Izid odseka: {@code REACHED} (noge v cilju), {@code FAILED} (Baritone je obupal:
 * ne išče, ne hodi, proces ni aktiven), {@code TIMEOUT} (30 s). Odsek je uspešen, če je izid
 * pričakovan in so izpolnjene dodatne omejitve (padec, prepovedan stolpec).
 */
public final class CourseRunner {

    public static final CourseRunner INSTANCE = new CourseRunner();

    static final int SETTLE_TICKS = 10;
    static final int MIN_TICKS_BEFORE_FAIL = 10;
    static final int TIMEOUT_TICKS = 600;

    private Run run;

    private CourseRunner() {
    }

    public boolean isRunning() {
        return run != null;
    }

    private static final class Run {
        final EntityLiving entity;
        final Baritone baritone;
        final ICommandSender sender;
        final List<CourseT1.Segment> segments;
        final List<String> csv = new ArrayList<>();
        int index = -1;
        int ticks;
        boolean started;
        double lastX, lastZ, meters, maxFall;
        long chunkLoadsAtStart;
        boolean enteredForbidden;
        int passed;
        final float[] yaw = new float[TIMEOUT_TICKS + 1];

        Run(EntityLiving entity, Baritone baritone, ICommandSender sender, List<CourseT1.Segment> segments) {
            this.entity = entity;
            this.baritone = baritone;
            this.sender = sender;
            this.segments = segments;
            csv.add("course,segment,name,expect,result,pass,ticks,meters,max_fall,chunk_loads,yaw_jitter,forbidden_entered");
        }
    }

    public void start(EntityLiving entity, Baritone baritone, ICommandSender sender, BlockPos origin) {
        if (run != null) {
            throw new IllegalStateException("course already running");
        }
        run = new Run(entity, baritone, sender, CourseT1.segments(origin));
        next();
    }

    public void abort() {
        if (run != null) {
            run.baritone.getPathingBehavior().cancelEverything();
            say(run, "tečaj prekinjen");
            run = null;
        }
    }

    private void next() {
        Run r = run;
        r.index++;
        if (r.index >= r.segments.size()) {
            finish(r);
            return;
        }
        CourseT1.Segment s = r.segments.get(r.index);
        r.baritone.getPathingBehavior().cancelEverything();
        r.entity.setPositionAndUpdate(s.start.getX() + 0.5, s.start.getY(), s.start.getZ() + 0.5);
        r.entity.rotationYaw = s.startYaw;
        r.entity.renderYawOffset = s.startYaw;
        r.entity.motionX = r.entity.motionY = r.entity.motionZ = 0;
        r.entity.fallDistance = 0;
        r.ticks = 0;
        r.started = false;
        r.meters = 0;
        r.maxFall = 0;
        r.enteredForbidden = false;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || run == null) {
            return;
        }
        Run r = run;
        if (r.entity.isDead) {
            say(r, "entiteta je umrla; tečaj prekinjen");
            run = null;
            return;
        }
        keepTicking(r.entity);
        CourseT1.Segment s = r.segments.get(r.index);
        if (!r.started) {
            if (++r.ticks < SETTLE_TICKS) {
                return;
            }
            r.started = true;
            r.ticks = 0;
            r.lastX = r.entity.posX;
            r.lastZ = r.entity.posZ;
            r.chunkLoadsAtStart = Telemetry.INSTANCE.chunkLoadsTotal();
            r.baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(s.goal));
            return;
        }
        r.ticks++;
        double dx = r.entity.posX - r.lastX;
        double dz = r.entity.posZ - r.lastZ;
        r.meters += Math.sqrt(dx * dx + dz * dz);
        r.lastX = r.entity.posX;
        r.lastZ = r.entity.posZ;
        r.maxFall = Math.max(r.maxFall, r.entity.fallDistance);
        if (r.ticks < r.yaw.length) {
            r.yaw[r.ticks] = r.entity.rotationYaw;
        }
        if (s.forbiddenColumn != null) {
            BlockPos feet = new BlockPos(r.entity);
            if (feet.getX() == s.forbiddenColumn.getX() && feet.getZ() == s.forbiddenColumn.getZ()) {
                r.enteredForbidden = true;
            }
        }
        String result = null;
        if (new GoalBlock(s.goal).isInGoal(new BlockPos(r.entity))) {
            result = "REACHED";
        } else if (r.ticks >= MIN_TICKS_BEFORE_FAIL && idle(r.baritone)) {
            result = "FAILED";
        } else if (r.ticks >= TIMEOUT_TICKS) {
            result = "TIMEOUT";
        }
        if (result != null) {
            record(r, s, result);
            next();
        }
    }

    /**
     * Vanilla {@code WorldServer} po 300 tickih brez igralcev neha posodabljati entitete;
     * skriptiran tek na dedicated strežniku nima igralca. Velja samo med tekom.
     */
    static void keepTicking(EntityLiving entity) {
        if (entity.world instanceof WorldServer) {
            ((WorldServer) entity.world).resetUpdateEntityTick();
        }
    }

    private static boolean idle(Baritone b) {
        return !b.getCustomGoalProcess().isActive()
                && !b.getPathingBehavior().isPathing()
                && !b.getPathingBehavior().getInProgress().isPresent();
    }

    private void record(Run r, CourseT1.Segment s, String result) {
        long loads = Telemetry.INSTANCE.chunkLoadsTotal() - r.chunkLoadsAtStart;
        int jitter = 0;
        for (int i = 6; i <= Math.min(r.ticks, r.yaw.length - 1); i++) {
            if (Math.abs(MathHelper.wrapDegrees(r.yaw[i] - r.yaw[i - 5])) > 90) {
                jitter++;
            }
        }
        boolean expected = s.expect == CourseT1.Expect.REACH ? "REACHED".equals(result) : "FAILED".equals(result);
        boolean fallOk = Double.isNaN(s.maxFall) || r.maxFall <= s.maxFall;
        boolean pass = expected && fallOk && !r.enteredForbidden;
        if (pass) {
            r.passed++;
        }
        r.baritone.getPathingBehavior().cancelEverything();
        r.csv.add(String.format(Locale.ROOT, "T1,%d,%s,%s,%s,%s,%d,%.2f,%.2f,%d,%d,%s",
                s.index, s.name, s.expect, result, pass, r.ticks, r.meters, r.maxFall, loads, jitter, r.enteredForbidden));
        say(r, String.format(Locale.ROOT, "T1 %d/10 %-28s %-8s %s  %d t  %.1f m  padec %.1f  chunki %d",
                s.index, s.name, result, pass ? "OK" : "NAPAKA", r.ticks, r.meters, r.maxFall, loads));
    }

    private void finish(Run r) {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File dir = new File("npcbaritone/runs");
        File out = new File(dir, "t1-" + stamp + ".csv");
        try {
            Files.createDirectories(dir.toPath());
            Files.write(out.toPath(), r.csv, StandardCharsets.UTF_8);
        } catch (IOException e) {
            NpcBaritoneMod.LOG.error("cannot write {}", out, e);
        }
        say(r, "T1 končan: " + r.passed + "/" + r.segments.size() + " odsekov OK; CSV " + out.getPath());
        NpcBaritoneMod.LOG.info("NPCB-COURSE-DONE course=T1 passed={} total={} csv={}", r.passed, r.segments.size(), out.getAbsolutePath());
        run = null;
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
