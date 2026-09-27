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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.pathing.movement.EntitySize;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalNear;
import si.ladja.npcbaritone.core.pathing.path.PathExecutor;

import java.util.List;

/**
 * M6.1–M6.6 (D-018, D-019): navigator, ki vanilla AI taskom daje pogodbo {@code PathNavigate},
 * pot pa išče in izvaja Baritone. Obstoječi taski (napad, tavanje, sledenje) delujejo
 * nespremenjeni.
 *
 * <ul>
 *     <li>{@code tryMoveToXYZ}: optimističen — vrne {@code true}, če je cilj naložen; iskanje
 *     teče v ozadju. Isti cilj (±1 blok) ne sproži novega iskanja ({@link NavDebounce}).</li>
 *     <li>{@code tryMoveToEntityLiving}: sledi entiteti; nov cilj, ko se premakne več kot
 *     {@value #FOLLOW_REGOAL_DISTANCE} bloka (največ vsakih {@value #FOLLOW_REGOAL_INTERVAL} tickov).</li>
 *     <li>{@code noPath}: {@code false} med iskanjem in hojo, {@code true} po prihodu,
 *     neuspehu, v mirovanju in med pavzo po {@code clearPath}.</li>
 *     <li>{@code clearPath}: pavza; isti cilj v 10 tickih nadaljuje brez iskanja.</li>
 *     <li>{@code setPath(vanilla)}: cilj = zadnja točka ({@code GoalNear} r=1).</li>
 *     <li>{@code getPath}: vanilla {@link Path} iz Baritonove poti (samo branje točk).</li>
 *     <li>{@code getPathToPos}/{@code getPathToEntityLiving}: vanilla (sinhrono, hibrid).</li>
 *     <li>{@code setSpeed(s)}: {@code s > 1,0} dovoli sprint, {@code 0 < s ≤ 1,0} samo hoja;
 *     {@code s ≤ 0} (ukazi, API {@link #goTo}) sprint po profilu. Hitrost sama ostane "kot
 *     igralec" (D-010) — množitelj taska se ne uporablja.</li>
 *     <li>D-019/D-028: entiteta širša od 1,0 ali višja od 2,0 dobi vanilla navigacijo (vse
 *     metode gredo na {@code super}), razen če je v configu vklopljen {@code largeEntities}
 *     (M8); takrat Baritone vodi do širine 3,0 in višine 4,0.</li>
 * </ul>
 */
public class BaritonePathNavigate extends PathNavigateGround {

    static final double FOLLOW_REGOAL_DISTANCE = 2.0;
    static final int FOLLOW_REGOAL_INTERVAL = 10;
    static final float MAX_WIDTH = 1.0F;
    static final float MAX_HEIGHT = 2.0F;
    /** D-028: meje, ki jih M8 dokaže (golden testi do 2,0 × 3,6, T3 do CNPC size 10). */
    static final int MAX_SIDE_SPACE = 1;
    static final int MAX_HEIGHT_BLOCKS = 4;

    private final Baritone baritone;
    private final EntityInteractions interactions;
    private final NavStatus status;
    private final NavDebounce debounce = new NavDebounce();
    private Entity followTarget;
    private int lastFollowRegoal = Integer.MIN_VALUE / 2;
    private PathExecutor cachedFor;
    private Path cachedPath;
    /** M6 A4: klici tryMoveTo in setPath ter koliko jih je začelo novo iskanje. */
    private int requests;
    private int newSearches;
    /** M6.7: razdalja sledenja (GoalNear), privzeto 1 za vanilla {@code tryMoveToEntityLiving}. */
    private int followRange = 1;
    /** D-041: zadnji cilj je prišel prek vanilla {@code PathNavigate} (doseg velja), ne prek API. */
    private boolean vanillaRange;
    private final java.util.List<si.ladja.npcbaritone.api.NavListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private NavStatus.State lastState = NavStatus.State.IDLE;

    public BaritonePathNavigate(EntityLiving entity, World world, Baritone baritone) {
        this(entity, world, baritone, new EntityInteractions(entity, baritone), NavStatus.install(baritone));
    }

    BaritonePathNavigate(EntityLiving entity, World world, Baritone baritone, EntityInteractions interactions, NavStatus status) {
        super(entity, world);
        this.baritone = baritone;
        this.interactions = interactions;
        this.status = status;
    }

    public EntityInteractions interactions() {
        return interactions;
    }

    public Baritone baritone() {
        return baritone;
    }

    public NavStatus status() {
        return status;
    }

    // stopnja A: reže profila (PerfProfile) za odseke onUpdateNavigation
    static final int PROF_DEBOUNCE = 0;
    static final int PROF_FOLLOW = 1;
    static final int PROF_BARITONE_TICK = 2;
    static final int PROF_INTERACTIONS = 3;

    static {
        si.ladja.npcbaritone.core.PerfProfile.label(BaritonePathNavigate.class,
                "debounce", "follow", "baritone.tick (skupaj)", "interactions");
    }

    /** D-019/D-028: ali Baritone vodi to entiteto (sicer vanilla). */
    public boolean fits() {
        return fits(entity.width, entity.height);
    }

    static boolean fits(float width, float height) {
        return fits(width, height, NpcBaritoneMod.config().largeEntities);
    }

    /**
     * @param largeEntities config {@code movement.largeEntities} (M8): false = D-019 (en stolpec
     *                      1×2), true = D-028 (do 3 stolpce, 4 bloke)
     */
    static boolean fits(float width, float height, boolean largeEntities) {
        if (!largeEntities) {
            return width <= MAX_WIDTH && height <= MAX_HEIGHT;
        }
        if (!(width > 0) || !(height > 0)) {
            return false;
        }
        EntitySize size = new EntitySize(width, height);
        return size.sideSpace <= MAX_SIDE_SPACE && size.heightBlocks <= MAX_HEIGHT_BLOCKS;
    }

    /** M6.5: ali sme izvajalec poti šprintati (bere {@code BaritoneMoveHelper}). */
    public boolean allowsSprint() {
        return allowsSprint(speed);
    }

    static boolean allowsSprint(double speed) {
        return speed <= 0 || speed > 1.0;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void onUpdateNavigation() {
        ++this.totalTicks;
        if (!fits()) {
            dropBaritone();
            super.onUpdateNavigation();
            return;
        }
        long t0 = System.nanoTime();
        long p = si.ladja.npcbaritone.core.PerfProfile.start();
        if (debounce.tick(totalTicks, status.state())) {
            baritone.getPathingBehavior().cancelEverything();
            followTarget = null;
        }
        if (debounce.paused()) {
            baritone.getInputOverrideHandler().clearAllKeys(); // stoj, pot ostane
            si.ladja.npcbaritone.core.PerfProfile.add(BaritonePathNavigate.class, PROF_DEBOUNCE, p);
        } else {
            si.ladja.npcbaritone.core.PerfProfile.add(BaritonePathNavigate.class, PROF_DEBOUNCE, p);
            p = si.ladja.npcbaritone.core.PerfProfile.start();
            updateFollow();
            si.ladja.npcbaritone.core.PerfProfile.add(BaritonePathNavigate.class, PROF_FOLLOW, p);
            p = si.ladja.npcbaritone.core.PerfProfile.start();
            baritone.tick();
            si.ladja.npcbaritone.core.PerfProfile.add(BaritonePathNavigate.class, PROF_BARITONE_TICK, p);
            p = si.ladja.npcbaritone.core.PerfProfile.start();
            interactions.tick(); // M4.1: CLICK_RIGHT → vrata
            si.ladja.npcbaritone.core.PerfProfile.add(BaritonePathNavigate.class, PROF_INTERACTIONS, p);
        }
        PerfMeter.INSTANCE.add(System.nanoTime() - t0); // M5.6
        fireTransitions();
    }

    /** M6.7: povratni klic enkrat na prehod v ARRIVED ali FAILED. */
    private void fireTransitions() {
        NavStatus.State now = status.state();
        if (now == lastState) {
            return;
        }
        lastState = now;
        if (listeners.isEmpty()) {
            return;
        }
        for (si.ladja.npcbaritone.api.NavListener l : listeners) {
            try {
                if (now == NavStatus.State.ARRIVED) {
                    l.onArrived(entity);
                } else if (now == NavStatus.State.FAILED) {
                    l.onFailed(entity, status.failReason());
                }
            } catch (RuntimeException ex) {
                NpcBaritoneMod.LOG.error("NavListener {} failed for {}", l, entity, ex);
            }
        }
    }

    public void addListener(si.ladja.npcbaritone.api.NavListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeListener(si.ladja.npcbaritone.api.NavListener l) {
        listeners.remove(l);
    }

    /** M6.7: sledi entiteti na razdalji {@code range} (API; hitrost po profilu). */
    public boolean follow(Entity target, int range) {
        if (!fits() || target == null) {
            return false;
        }
        BlockPos pos = new BlockPos(target);
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        speed = 0;
        vanillaRange = false;
        followRange = Math.max(1, range);
        debounce.reset();
        debounce.goalPos = pos;
        followTarget = target;
        lastFollowRegoal = totalTicks;
        newSearches++;
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(pos, followRange));
        return true;
    }

    /** M6.7: takojšen preklic (API {@code stop}); brez pavze, ki velja za vanilla {@code clearPath}. */
    public void stopNow() {
        debounce.reset();
        followTarget = null;
        baritone.getPathingBehavior().cancelEverything();
    }

    private void updateFollow() {
        Entity t = followTarget;
        if (t == null || debounce.goalPos == null) {
            return;
        }
        if (t.isDead || t.world != entity.world) {
            followTarget = null;
            return;
        }
        if (totalTicks - lastFollowRegoal < FOLLOW_REGOAL_INTERVAL) {
            return;
        }
        BlockPos now = new BlockPos(t);
        if (!NavDebounce.sameTarget(now, debounce.goalPos, (int) FOLLOW_REGOAL_DISTANCE) && world.isBlockLoaded(now)) {
            debounce.goalPos = now;
            newSearches++;
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalNear(now, followRange));
            applyVanillaRange();
            lastFollowRegoal = totalTicks;
        }
    }

    // ------------------------------------------------------------------ cilji

    /** API/ukazi: izrecen cilj, sprint po profilu, brez debounca. */
    public boolean goTo(Goal goal) {
        speed = 0;
        vanillaRange = false;
        followTarget = null;
        followRange = 1;
        debounce.reset();
        baritone.getCustomGoalProcess().setGoalAndPath(goal);
        return true;
    }

    @Override
    public boolean tryMoveToXYZ(double x, double y, double z, double speedIn) {
        if (!fits()) {
            dropBaritone();
            return super.tryMoveToXYZ(x, y, z, speedIn);
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        setSpeed(speedIn);
        followTarget = null;
        return request(pos, 1, new GoalBlock(pos));
    }

    @Override
    public boolean tryMoveToEntityLiving(Entity target, double speedIn) {
        if (!fits()) {
            dropBaritone();
            return super.tryMoveToEntityLiving(target, speedIn);
        }
        BlockPos pos = new BlockPos(target);
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        setSpeed(speedIn);
        followRange = 1;
        boolean ok = request(pos, (int) FOLLOW_REGOAL_DISTANCE, new GoalNear(pos, 1));
        if (ok) {
            if (followTarget != target) {
                lastFollowRegoal = totalTicks;
            }
            followTarget = target;
        }
        return ok;
    }

    @Override
    public boolean setPath(Path pathIn, double speedIn) {
        if (!fits()) {
            dropBaritone();
            return super.setPath(pathIn, speedIn);
        }
        if (pathIn == null) {
            clearPath();
            return false;
        }
        PathPoint last = pathIn.getFinalPathPoint();
        if (last == null) {
            return false;
        }
        BlockPos pos = new BlockPos(last.x, last.y, last.z);
        setSpeed(speedIn);
        return request(pos, 1, new GoalNear(pos, 1));
    }

    /**
     * D-041 (CNPC M7.5 NpcNavRange): vanilla pogodba — iskanje za AI taske vidi entiteta ±
     * ({@code FOLLOW_RANGE} + 8). Kliče se za vsakim {@code setGoalAndPath}, ki doseg ponastavi.
     */
    private void applyVanillaRange() {
        if (vanillaRange && baritone.getSettings().npcRespectFollowRange.value) {
            baritone.getPathingBehavior().setSearchRange(searchRangeBlocks(getPathSearchRange()));
        }
    }

    /** D-041: {@code FOLLOW_RANGE} → doseg v blokih (navzgor, vsaj 1). */
    static int searchRangeBlocks(float followRange) {
        return Math.max(1, (int) Math.ceil(followRange));
    }

    public int requests() {
        return requests;
    }

    public int newSearches() {
        return newSearches;
    }

    private boolean request(BlockPos pos, int tolerance, Goal goal) {
        requests++;
        switch (debounce.request(pos, tolerance, totalTicks, status.state(), goal.isInGoal(new BlockPos(entity)))) {
            case NEW:
                newSearches++;
                baritone.getCustomGoalProcess().setGoalAndPath(goal);
                vanillaRange = true;
                applyVanillaRange();
                return true;
            case KEEP:
            case RESUME:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void clearPath() {
        super.clearPath();
        if (baritone == null) {
            return; // super konstruktor lahko pokliče clearPath pred nastavitvijo polj
        }
        if (!fits()) {
            dropBaritone();
            return;
        }
        followTarget = null;
        debounce.clear(totalTicks);
    }

    @Override
    public boolean noPath() {
        if (baritone == null) {
            return true;
        }
        if (!fits()) {
            return super.noPath();
        }
        if (debounce.paused()) {
            return true;
        }
        NavStatus.State s = status.state();
        return s == NavStatus.State.IDLE || s == NavStatus.State.ARRIVED || s == NavStatus.State.FAILED;
    }

    /**
     * M7.1: pot med iskanjem (Baritone še nima poti, {@code noPath()} je false) — ena točka na
     * cilju (ali pri entiteti, če cilj nima položaja), nezaključena, da jo bralci vanilla
     * {@link Path} razumejo kot "na poti proti".
     */
    static Path placeholder(BlockPos goal, Entity entity) {
        BlockPos p = goal != null ? goal : new BlockPos(entity);
        return new Path(new PathPoint[]{new PathPoint(p.getX(), p.getY(), p.getZ())});
    }

    static BlockPos goalPos(Goal goal) {
        if (goal instanceof si.ladja.npcbaritone.core.api.utils.interfaces.IGoalRenderPos) {
            return ((si.ladja.npcbaritone.core.api.utils.interfaces.IGoalRenderPos) goal).getGoalPos();
        }
        return null;
    }

    /** M6.4: vanilla pogled na Baritonovo pot (točke blokov, indeks = trenutni premik). */
    @Override
    public Path getPath() {
        if (baritone == null || !fits()) {
            return super.getPath();
        }
        PathExecutor cur = baritone.getPathingBehavior().getCurrent();
        if (cur == null) {
            // M7.1: vanilla velja "!noPath() ⇒ getPath() != null"; CNPC skriptni API
            // (EntityLivingWrapper.getNavigationPath) to predpostavi in bi med iskanjem padel z NPE.
            return noPath() ? null : placeholder(goalPos(status.goal()), entity);
        }
        if (cur != cachedFor) {
            List<? extends BlockPos> pos = cur.getPath().positions();
            PathPoint[] points = new PathPoint[pos.size()];
            for (int i = 0; i < points.length; i++) {
                BlockPos p = pos.get(i);
                points[i] = new PathPoint(p.getX(), p.getY(), p.getZ());
            }
            cachedPath = new Path(points);
            cachedFor = cur;
        }
        int idx = Math.max(0, Math.min(cur.getPosition(), cachedPath.getCurrentPathLength() - 1));
        cachedPath.setCurrentPathIndex(idx);
        return cachedPath;
    }

    /** Za prevelike entitete: Baritone ne sme hkrati voditi. */
    private void dropBaritone() {
        if (debounce.goalPos != null || baritone.getCustomGoalProcess().isActive()) {
            baritone.getPathingBehavior().cancelEverything();
            debounce.reset();
            followTarget = null;
        }
    }
}
