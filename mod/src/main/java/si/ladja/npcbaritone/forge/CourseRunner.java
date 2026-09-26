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
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import si.ladja.npcbaritone.core.pathing.movement.EntitySize;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.EntityEvent;
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
    /** Največ tickov čakanja na končno stanje navigacije po izidu odseka. */
    static final int NAV_SETTLE_TICKS = 20;

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
        final Course course;
        final BlockPos origin;
        final List<Course.Segment> segments;
        /** M4.9: stanja blokov območja tečaja ob začetku (vrata normalizirana na zaprta). */
        final int[] signature;
        double damage;
        final List<String> csv = new ArrayList<>();
        int index = -1;
        int ticks;
        int entityTicksAtStart;
        boolean started;
        double lastX, lastZ, meters, maxFall;
        long chunkLoadsAtStart;
        boolean enteredForbidden;
        int passed;
        /** Izid odseka je znan; čaka se, da se navigacija umiri (M5: preverba NavStatus). */
        String pendingResult;
        int settleTicks;
        final float[] yaw = new float[TIMEOUT_TICKS + 1];
        /** M8.9: prisiljeni chunki tečaja (T3 je večji od spawn območja strežnika brez igralca). */
        final List<net.minecraftforge.common.ForgeChunkManager.Ticket> tickets = new ArrayList<>();

        Run(EntityLiving entity, Baritone baritone, ICommandSender sender, Course course, BlockPos origin) {
            this.entity = entity;
            this.baritone = baritone;
            this.sender = sender;
            this.course = course;
            this.origin = origin;
            this.segments = course.segments(origin);
            this.signature = signature(entity.world, course.bounds(origin));
            csv.add("course,segment,name,expect,result,pass,ticks,meters,max_fall,chunk_loads,yaw_jitter,forbidden_entered,damage,openables_closed,nav_state,npc_size,width,height");
        }
    }

    /**
     * M8.9: velikost odseka, ki je navigator ne bi vodil (D-019 brez {@code movement.largeEntities}),
     * bi odsek tiho pokvarila: {@code BaritonePathNavigate.onUpdateNavigation} vsak tick prekliče
     * Baritonov cilj ({@code dropBaritone}) in odsek konča FAILED v 10 tickih (drugi tek T3,
     * 2026-09-26 14:10: size 7 in 10). Zato tečaj takega odseka ne začne.
     *
     * @return opis prve take velikosti ali {@code null}, če se vse velikosti vodijo
     */
    static String sizeNotLed(Course course, BlockPos origin, boolean largeEntities) {
        for (Course.Segment s : course.segments(origin)) {
            if (s.npcSize > 0) {
                EntitySize size = CourseT3.npcSize(s.npcSize);
                if (!BaritonePathNavigate.fits(size.width, size.height, largeEntities)) {
                    return course.id() + " size " + s.npcSize + " (" + size.width + " x " + size.height
                            + ") navigator ne vodi: vklopi movement.largeEntities=true (D-028)";
                }
            }
        }
        return null;
    }

    public void start(EntityLiving entity, Baritone baritone, ICommandSender sender, Course course, BlockPos origin) {
        if (run != null) {
            throw new IllegalStateException("course already running");
        }
        run = new Run(entity, baritone, sender, course, origin);
        if (course.forceChunks()) {
            forceChunks(run);
        }
        // M3.4 (A3): en tek T1 = ena datoteka sledi poleg CSV izidov
        PathTrace.INSTANCE.start(java.util.Collections.singletonList(entity));
        next();
    }

    public void abort() {
        if (run != null) {
            run.baritone.getPathingBehavior().cancelEverything();
            say(run, "tečaj prekinjen");
            PathTrace.INSTANCE.stop();
            releaseChunks(run);
            run = null;
        }
    }

    private static final String[] SET_SIZE = {"func_70105_a", "setSize"};
    private static final String[] ZOMBIE_MULTIPLY_SIZE = {"func_146069_a", "multiplySize"};
    private static java.lang.reflect.Method setSizeMethod;
    private static java.lang.reflect.Method zombieMultiplySize;

    /**
     * M8.9: nastavi velikost entitete kot CNPC ({@code Entity.setSize}, zaščitena).
     * {@code EntityZombie.setSize} jo prepiše: po prvem klicu (konstruktor) si velikost samo
     * zapomni, uveljavi pa jo šele {@code multiplySize(1)} — brez tega je prvi tek T3
     * (2026-09-26) vse velikosti prevozil s huskom 0,60 × 1,95.
     */
    static void resize(EntityLiving entity, float width, float height) {
        try {
            if (setSizeMethod == null) {
                setSizeMethod = findMethod(net.minecraft.entity.Entity.class, SET_SIZE, float.class, float.class);
            }
            setSizeMethod.invoke(entity, width, height);
            if (entity instanceof net.minecraft.entity.monster.EntityZombie
                    && (entity.width != width || entity.height != height)) {
                if (zombieMultiplySize == null) {
                    zombieMultiplySize = findMethod(net.minecraft.entity.monster.EntityZombie.class, ZOMBIE_MULTIPLY_SIZE, float.class);
                }
                zombieMultiplySize.invoke(entity, 1.0F);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot resize " + entity, e);
        }
        if (Math.abs(entity.width - width) > 1e-4 || Math.abs(entity.height - height) > 1e-4) {
            throw new IllegalStateException("cannot resize " + entity + ": " + width + " x " + height
                    + " requested, " + entity.width + " x " + entity.height + " applied");
        }
    }

    /**
     * Metoda po SRG (izdaja) ali MCP (razvoj) imenu; če ni nobenega, edina deklarirana metoda
     * s temi parametri (EntityZombie ima eno samo {@code (float)}), da napačno SRG ime ne
     * zlomi izdaje.
     */
    private static java.lang.reflect.Method findMethod(Class<?> owner, String[] names, Class<?>... params)
            throws NoSuchMethodException {
        for (String name : names) {
            try {
                java.lang.reflect.Method m = owner.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // naslednje ime (SRG v izdaji, MCP v razvoju)
            }
        }
        java.lang.reflect.Method only = null;
        for (java.lang.reflect.Method m : owner.getDeclaredMethods()) {
            if (m.getReturnType() == void.class && java.util.Arrays.equals(m.getParameterTypes(), params)) {
                if (only != null) {
                    throw new NoSuchMethodException(owner.getName() + "." + names[names.length - 1] + " is ambiguous");
                }
                only = m;
            }
        }
        if (only == null) {
            throw new NoSuchMethodException(owner.getName() + "." + names[names.length - 1]);
        }
        only.setAccessible(true);
        return only;
    }

    /** Dedicated tečaj brez igralca: vanilla sicer preskoči entiteto, če okolica ni naložena. */
    @SubscribeEvent
    public void onCanUpdate(EntityEvent.CanUpdate event) {
        if (run != null && event.getEntity() == run.entity) {
            event.setCanUpdate(true);
        }
    }

    private void next() {
        Run r = run;
        r.index++;
        if (r.index >= r.segments.size()) {
            finish(r);
            return;
        }
        Course.Segment s = r.segments.get(r.index);
        PathTrace.INSTANCE.setTag(r.course.id() + "/" + s.index);
        r.baritone.getPathingBehavior().cancelEverything();
        if (s.npcSize > 0) {
            // M8.9 (T3): velikost pred teleportom, da setPosition postavi okvir na sredino
            EntitySize size = CourseT3.npcSize(s.npcSize);
            resize(r.entity, size.width, size.height);
        }
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
        r.damage = 0;
        r.pendingResult = null;
        r.settleTicks = 0;
    }

    /** M4 A3: škoda, ki jo entiteta tečaja dobi med odsekom (padec, utopitev, lava, kaktus). */
    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        Run r = run;
        if (r != null && event.getEntity() == r.entity && r.started) {
            r.damage += event.getAmount();
            NpcBaritoneMod.LOG.info("{} {}: škoda {} ({})", r.course.id(), r.index + 1, event.getAmount(), event.getSource().getDamageType());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || run == null) {
            return;
        }
        Run r = run;
        if (r.entity.isDead) {
            say(r, "entiteta je umrla; tečaj prekinjen");
            PathTrace.INSTANCE.stop();
            releaseChunks(r);
            run = null;
            return;
        }
        keepTicking(r.entity);
        Course.Segment s = r.segments.get(r.index);
        if (r.pendingResult != null) {
            NavStatus st = Attach.status(r.entity);
            NavStatus.State state = st == null ? NavStatus.State.IDLE : st.state();
            boolean settled = state != NavStatus.State.MOVING && state != NavStatus.State.SEARCHING;
            if (settled || ++r.settleTicks >= NAV_SETTLE_TICKS) {
                record(r, s, r.pendingResult, state);
                next();
            }
            return;
        }
        if (!r.started) {
            if (++r.ticks < SETTLE_TICKS) {
                return;
            }
            r.started = true;
            r.ticks = 0;
            r.entityTicksAtStart = r.entity.ticksExisted;
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
            if ("TIMEOUT".equals(result)) {
                NpcBaritoneMod.LOG.warn("T1 timeout diagnostic: entityTicks={} active={} pathing={} inProgress={} loaded={}",
                        r.entity.ticksExisted - r.entityTicksAtStart,
                        r.baritone.getCustomGoalProcess().isActive(),
                        r.baritone.getPathingBehavior().isPathing(),
                        r.baritone.getPathingBehavior().getInProgress().isPresent(),
                        r.entity.world.isBlockLoaded(new BlockPos(r.entity)));
            }
            // izid velja zdaj (ticks, metri); zapis počaka, da NavStatus pove končno stanje
            r.pendingResult = result;
            r.settleTicks = 0;
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

    private void record(Run r, Course.Segment s, String result, NavStatus.State navState) {
        long loads = Telemetry.INSTANCE.chunkLoadsTotal() - r.chunkLoadsAtStart;
        int jitter = 0;
        for (int i = 6; i <= Math.min(r.ticks, r.yaw.length - 1); i++) {
            if (Math.abs(MathHelper.wrapDegrees(r.yaw[i] - r.yaw[i - 5])) > 90) {
                jitter++;
            }
        }
        boolean expected = s.expect == Course.Expect.REACH ? "REACHED".equals(result) : "FAILED".equals(result);
        boolean fallOk = Double.isNaN(s.maxFall) || r.maxFall <= s.maxFall;
        boolean closedOk = true;
        for (BlockPos p : s.openables) {
            closedOk &= !isOpen(r.entity.world.getBlockState(p));
        }
        boolean pass = expected && fallOk && !r.enteredForbidden && r.damage == 0 && closedOk;
        if (pass) {
            r.passed++;
        }
        r.baritone.getPathingBehavior().cancelEverything();
        r.csv.add(String.format(Locale.ROOT, "%s,%d,\"%s\",%s,%s,%s,%d,%.2f,%.2f,%d,%d,%s,%.1f,%s,%s,%d,%.2f,%.2f",
                r.course.id(), s.index, s.name.replace("\"", "\"\""), s.expect, result, pass, r.ticks, r.meters, r.maxFall, loads, jitter,
                r.enteredForbidden, r.damage, s.openables.isEmpty() ? "" : String.valueOf(closedOk), navState,
                s.npcSize, r.entity.width, r.entity.height));
        say(r, String.format(Locale.ROOT, "%s %d/%d %-28s %-8s %s  %d t  %.1f m  padec %.1f  chunki %d  škoda %.1f%s",
                r.course.id(), s.index, r.segments.size(), s.name, result, pass ? "OK" : "NAPAKA", r.ticks, r.meters, r.maxFall, loads,
                r.damage, s.openables.isEmpty() ? "" : (closedOk ? "  vrata zaprta" : "  VRATA ODPRTA")));
    }

    private void finish(Run r) {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File dir = new File("npcbaritone/runs");
        String prefix = r.course.id().toLowerCase(Locale.ROOT) + "-" + stamp;
        File out = new File(dir, prefix + ".csv");
        File trace = new File(dir, prefix + "-trace.csv");
        List<BlockPos> changed = diff(r.entity.world, r.course.bounds(r.origin), r.signature);
        PathTrace.INSTANCE.stop();
        int traceRows = -1;
        try {
            Files.createDirectories(dir.toPath());
            Files.write(out.toPath(), r.csv, StandardCharsets.UTF_8);
            traceRows = PathTrace.INSTANCE.dump(trace);
        } catch (IOException e) {
            NpcBaritoneMod.LOG.error("cannot write {} / {}", out, trace, e);
        }
        say(r, r.course.id() + " končan: " + r.passed + "/" + r.segments.size() + " odsekov OK; CSV " + out.getPath()
                + ", sled " + trace.getPath() + " (" + traceRows + " vrstic); spremenjenih blokov " + changed.size()
                + (changed.isEmpty() ? "" : " npr. " + changed.subList(0, Math.min(3, changed.size()))));
        NpcBaritoneMod.LOG.info("NPCB-COURSE-DONE course={} passed={} total={} blocks_changed={} csv={} trace={} trace_rows={}",
                r.course.id(), r.passed, r.segments.size(), changed.size(), out.getAbsolutePath(), trace.getAbsolutePath(), traceRows);
        releaseChunks(r);
        run = null;
    }

    // ------------------------------------------------------------------ M8.9 chunki

    private static void forceChunks(Run r) {
        if (!(r.entity.world instanceof WorldServer)) {
            return;
        }
        WorldServer w = (WorldServer) r.entity.world;
        BlockPos[] b = r.course.bounds(r.origin);
        int perTicket = Math.max(1, net.minecraftforge.common.ForgeChunkManager.getMaxChunkDepthFor(NpcBaritoneMod.MODID));
        net.minecraftforge.common.ForgeChunkManager.Ticket ticket = null;
        int inTicket = 0;
        for (int cx = (b[0].getX() >> 4) - 1; cx <= (b[1].getX() >> 4) + 1; cx++) {
            for (int cz = (b[0].getZ() >> 4) - 1; cz <= (b[1].getZ() >> 4) + 1; cz++) {
                if (ticket == null || inTicket >= perTicket) {
                    ticket = net.minecraftforge.common.ForgeChunkManager.requestTicket(NpcBaritoneMod.INSTANCE, w, net.minecraftforge.common.ForgeChunkManager.Type.NORMAL);
                    if (ticket == null) {
                        NpcBaritoneMod.LOG.warn("{}: ni več Forge chunk vozovnic; tečaj teče brez prisiljenih chunkov", r.course.id());
                        return;
                    }
                    r.tickets.add(ticket);
                    inTicket = 0;
                }
                net.minecraftforge.common.ForgeChunkManager.forceChunk(ticket, new net.minecraft.util.math.ChunkPos(cx, cz));
                inTicket++;
            }
        }
    }

    private static void releaseChunks(Run r) {
        for (net.minecraftforge.common.ForgeChunkManager.Ticket t : r.tickets) {
            net.minecraftforge.common.ForgeChunkManager.releaseTicket(t);
        }
        r.tickets.clear();
    }

    // ------------------------------------------------------------------ M4.9 podpis blokov

    /** Odprta vrata/ograjna vrata (spodnja polovica). */
    static boolean isOpen(IBlockState s) {
        if (s.getBlock() instanceof BlockDoor) {
            return s.getPropertyKeys().contains(BlockDoor.OPEN) && s.getValue(BlockDoor.OPEN);
        }
        return s.getBlock() instanceof BlockFenceGate && s.getValue(BlockFenceGate.OPEN);
    }

    /** Stanje za primerjavo: odpiranje/zapiranje vrat ni sprememba (D-015 ga dovoli). */
    static int normalized(IBlockState s) {
        if (s.getBlock() instanceof BlockDoor) {
            s = s.withProperty(BlockDoor.OPEN, false).withProperty(BlockDoor.POWERED, false);
        } else if (s.getBlock() instanceof BlockFenceGate) {
            s = s.withProperty(BlockFenceGate.OPEN, false).withProperty(BlockFenceGate.POWERED, false);
        }
        return Block.getStateId(s);
    }

    static int[] signature(World w, BlockPos[] bounds) {
        List<Integer> ids = new ArrayList<>();
        for (BlockPos p : BlockPos.getAllInBoxMutable(bounds[0], bounds[1])) {
            ids.add(normalized(w.getBlockState(p)));
        }
        int[] out = new int[ids.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = ids.get(i);
        }
        return out;
    }

    static List<BlockPos> diff(World w, BlockPos[] bounds, int[] before) {
        List<BlockPos> changed = new ArrayList<>();
        int i = 0;
        for (BlockPos p : BlockPos.getAllInBoxMutable(bounds[0], bounds[1])) {
            if (i < before.length && normalized(w.getBlockState(p)) != before[i]) {
                changed.add(p.toImmutable());
            }
            i++;
        }
        return changed;
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
