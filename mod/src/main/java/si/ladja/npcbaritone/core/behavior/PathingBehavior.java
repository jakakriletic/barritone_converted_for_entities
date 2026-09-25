/*
 * This file is part of Baritone.
 * Modified for NPC Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package si.ladja.npcbaritone.core.behavior;

import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.behavior.IPathingBehavior;
import si.ladja.npcbaritone.core.api.event.events.*;
import si.ladja.npcbaritone.core.api.pathing.calc.IPath;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalXZ;
import si.ladja.npcbaritone.core.api.process.PathingCommand;
import si.ladja.npcbaritone.core.api.utils.BetterBlockPos;
import si.ladja.npcbaritone.core.api.utils.Helper;
import si.ladja.npcbaritone.core.api.utils.PathCalculationResult;
import si.ladja.npcbaritone.core.api.utils.interfaces.IGoalRenderPos;
import si.ladja.npcbaritone.core.pathing.calc.AStarPathFinder;
import si.ladja.npcbaritone.core.pathing.calc.AbstractNodeCostSearch;
import si.ladja.npcbaritone.core.pathing.movement.CalculationContext;
import si.ladja.npcbaritone.core.pathing.movement.MovementHelper;
import si.ladja.npcbaritone.core.pathing.path.PathExecutor;
import si.ladja.npcbaritone.core.utils.PathingCommandContext;
import si.ladja.npcbaritone.core.world.ChunkSnapshot;
import si.ladja.npcbaritone.core.utils.pathing.Favoring;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

public final class PathingBehavior extends Behavior implements IPathingBehavior, Helper {

    private PathExecutor current;
    private PathExecutor next;

    private Goal goal;
    private CalculationContext context;

    /*eta*/
    private int ticksElapsedSoFar;
    private BetterBlockPos startPosition;

    private boolean safeToCancel;
    private boolean pauseRequestedLastTick;
    private boolean unpausedLastTick;
    private boolean pausedThisTick;
    private boolean cancelRequested;
    private boolean calcFailedLastTick;

    private volatile AbstractNodeCostSearch inProgress;
    private final Object pathCalcLock = new Object();

    private final Object pathPlanLock = new Object();

    private boolean lastAutoJump;

    private BetterBlockPos expectedSegmentStart;

    private final LinkedBlockingQueue<PathEvent> toDispatch = new LinkedBlockingQueue<>();

    /**
     * M3: začetek in cilj zadnjega neuspelega načrtovanja naprej. Upstream po
     * {@code NEXT_CALC_FAILED} poskusi znova naslednji tick z istega začetka proti istemu cilju
     * (izmerjeno: 48 iskanj v 49 tickih na nedosegljivem cilju T1/10). Ponovi se šele, ko se
     * začetek ali cilj spremeni; ko se trenutni segment konča, se tako ali tako začne polno iskanje.
     */
    private BlockPos planAheadFailedFrom;
    private Goal planAheadFailedGoal;

    // M3.4: telemetrija zadnjega iskanja (pišejo iskalne niti, bere strežniška nit)
    private volatile long lastSearchMicros = -1;
    private volatile String lastSearchResult = "none";
    private volatile long searchesStarted;

    public PathingBehavior(Baritone baritone) {
        super(baritone);
    }

    private void queuePathEvent(PathEvent event) {
        toDispatch.add(event);
    }

    private void dispatchEvents() {
        ArrayList<PathEvent> curr = new ArrayList<>();
        toDispatch.drainTo(curr);
        calcFailedLastTick = curr.contains(PathEvent.CALC_FAILED);
        for (PathEvent event : curr) {
            baritone.getGameEventHandler().onPathEvent(event);
        }
    }

    @Override
    public void onTick(TickEvent event) {
        dispatchEvents();
        if (event.getType() == TickEvent.Type.OUT) {
            secretInternalSegmentCancel();
            baritone.getPathingControlManager().cancelEverything();
            return;
        }

        expectedSegmentStart = pathStart();
        baritone.getPathingControlManager().preTick();
        tickPath();
        ticksElapsedSoFar++;
        dispatchEvents();
    }

    private void tickPath() {
        pausedThisTick = false;
        if (pauseRequestedLastTick && safeToCancel) {
            pauseRequestedLastTick = false;
            if (unpausedLastTick) {
                baritone.getInputOverrideHandler().clearAllKeys();
            }
            unpausedLastTick = false;
            pausedThisTick = true;
            return;
        }
        unpausedLastTick = true;
        if (cancelRequested) {
            cancelRequested = false;
            baritone.getInputOverrideHandler().clearAllKeys();
        }
        synchronized (pathPlanLock) {
            synchronized (pathCalcLock) {
                if (inProgress != null) {
                    // we are calculating
                    // are we calculating the right thing though? 🤔
                    BetterBlockPos calcFrom = inProgress.getStart();
                    Optional<IPath> currentBest = inProgress.bestPathSoFar();
                    if ((current == null || !current.getPath().getDest().equals(calcFrom)) // if current ends in inProgress's start, then we're ok
                            && !calcFrom.equals(ctx.feetPos()) && !calcFrom.equals(expectedSegmentStart) // if current starts in our feetPos or pathStart, then we're ok
                            && (!currentBest.isPresent() || (!currentBest.get().positions().contains(ctx.feetPos()) && !currentBest.get().positions().contains(expectedSegmentStart))) // if
                    ) {
                        // when it was *just* started, currentBest will be empty so we need to also check calcFrom since that's always present
                        inProgress.cancel(); // cancellation doesn't dispatch any events
                    }
                }
            }
            if (current == null) {
                return;
            }
            safeToCancel = current.onTick();
            if (current.failed() || current.finished()) {
                current = null;
                if (goal == null || goal.isInGoal(ctx.feetPos())) {
                    logDebug("All done. At " + goal);
                    queuePathEvent(PathEvent.AT_GOAL);
                    next = null;
                    return;
                }
                if (next != null && !next.getPath().positions().contains(ctx.feetPos()) && !next.getPath().positions().contains(expectedSegmentStart)) { // can contain either one
                    // if the current path failed, we may not actually be on the next one, so make sure
                    logDebug("Discarding next path as it does not contain current position");
                    // for example if we had a nicely planned ahead path that starts where current ends
                    // that's all fine and good
                    // but if we fail in the middle of current
                    // we're nowhere close to our planned ahead path
                    // so need to discard it sadly.
                    queuePathEvent(PathEvent.DISCARD_NEXT);
                    next = null;
                }
                if (next != null) {
                    logDebug("Continuing on to planned next path");
                    queuePathEvent(PathEvent.CONTINUING_ONTO_PLANNED_NEXT);
                    current = next;
                    next = null;
                    current.onTick(); // don't waste a tick doing nothing, get started right away
                    return;
                }
                // at this point, current just ended, but we aren't in the goal and have no plan for the future
                synchronized (pathCalcLock) {
                    if (inProgress != null) {
                        queuePathEvent(PathEvent.PATH_FINISHED_NEXT_STILL_CALCULATING);
                        return;
                    }
                    // we aren't calculating
                    queuePathEvent(PathEvent.CALC_STARTED);
                    context = newSearchContext(expectedSegmentStart, goal);
                    findPathInNewThread(expectedSegmentStart, true, context);
                }
                return;
            }
            // at this point, we know current is in progress
            if (safeToCancel && next != null && next.snipsnapifpossible()) {
                // a movement just ended; jump directly onto the next path
                logDebug("Splicing into planned next path early...");
                queuePathEvent(PathEvent.SPLICING_ONTO_NEXT_EARLY);
                current = next;
                next = null;
                current.onTick();
                return;
            }
            if (baritone.getSettings().splicePath.value) {
                current = current.trySplice(next);
            }
            if (next != null && current.getPath().getDest().equals(next.getPath().getDest())) {
                next = null;
            }
            synchronized (pathCalcLock) {
                if (inProgress != null) {
                    // if we aren't calculating right now
                    return;
                }
                if (next != null) {
                    // and we have no plan for what to do next
                    return;
                }
                if (goal == null || goal.isInGoal(current.getPath().getDest())) {
                    // and this path doesn't get us all the way there
                    return;
                }
                if (current.getPath().getDest().equals(planAheadFailedFrom) && Objects.equals(goal, planAheadFailedGoal)) {
                    // enako načrtovanje je že spodletelo (M3)
                    return;
                }
                if (ticksRemainingInSegment(false).get() < baritone.getSettings().planningTickLookahead.value) {
                    // and this path has 7.5 seconds or less left
                    // don't include the current movement so a very long last movement (e.g. descend) doesn't trip it up
                    // if we actually included current, it wouldn't start planning ahead until the last movement was done, if the last movement took more than 7.5 seconds on its own
                    logDebug("Path almost over. Planning ahead...");
                    queuePathEvent(PathEvent.NEXT_SEGMENT_CALC_STARTED);
                    context = newSearchContext(current.getPath().getDest(), goal);
                    findPathInNewThread(current.getPath().getDest(), false, context);
                }
            }
        }
    }

    public void secretInternalSetGoal(Goal goal) {
        this.goal = goal;
    }

    public boolean secretInternalSetGoalAndPath(PathingCommand command) {
        secretInternalSetGoal(command.goal);
        // NPC Baritone (D-013): kontekst (posnetek chunkov) se naredi šele, ko se iskanje res
        // začne. Baritone ga je delal vsak tick, ko je proces vrnil SET_GOAL_AND_PATH —
        // na strežniku s 100+ NPC-ji je to 100+ kopij mape chunkov na tick.
        CalculationContext desired = command instanceof PathingCommandContext ? ((PathingCommandContext) command).desiredCalcContext : null;
        if (goal == null) {
            return false;
        }
        if (goal.isInGoal(ctx.feetPos()) || goal.isInGoal(expectedSegmentStart)) {
            return false;
        }
        synchronized (pathPlanLock) {
            if (current != null) {
                return false;
            }
            synchronized (pathCalcLock) {
                if (inProgress != null) {
                    return false;
                }
                context = desired != null ? desired : newSearchContext(expectedSegmentStart, goal);
                queuePathEvent(PathEvent.CALC_STARTED);
                findPathInNewThread(expectedSegmentStart, true, context);
                return true;
            }
        }
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    @Override
    public boolean isPathing() {
        return hasPath() && !pausedThisTick;
    }

    @Override
    public PathExecutor getCurrent() {
        return current;
    }

    @Override
    public PathExecutor getNext() {
        return next;
    }

    @Override
    public Optional<AbstractNodeCostSearch> getInProgress() {
        return Optional.ofNullable(inProgress);
    }

    public boolean isSafeToCancel() {
        if (current == null) {
            return true;
        }
        return safeToCancel;
    }

    public void requestPause() {
        pauseRequestedLastTick = true;
    }

    public boolean cancelSegmentIfSafe() {
        if (isSafeToCancel()) {
            secretInternalSegmentCancel();
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelEverything() {
        boolean doIt = isSafeToCancel();
        if (doIt) {
            segmentCancel(true);
        }
        baritone.getPathingControlManager().cancelEverything(); // regardless of if we can stop the current segment, we can still stop the processes
        return doIt;
    }

    /** M3.4: trajanje zadnjega končanega iskanja v µs (-1 = še nobenega). */
    public long lastSearchMicros() {
        return lastSearchMicros;
    }

    /**
     * M3.4: izid zadnjega iskanja: {@code none}, {@code success_to_goal}, {@code success_segment},
     * {@code failure}, {@code cancellation}, {@code exception} ali {@code queue_full}.
     */
    public String lastSearchResult() {
        return lastSearchResult;
    }

    /** M3.4: število začetih iskanj (vključno z načrtovanjem naprej). */
    public long searchesStarted() {
        return searchesStarted;
    }

    public boolean calcFailedLastTick() { // NOT exposed on public api
        return calcFailedLastTick;
    }

    public void softCancelIfSafe() {
        synchronized (pathPlanLock) {
            getInProgress().ifPresent(AbstractNodeCostSearch::cancel); // only cancel ours
            if (!isSafeToCancel()) {
                return;
            }
            current = null;
            next = null;
        }
        cancelRequested = true;
        // do everything BUT clear keys
    }

    // just cancel the current path
    public void secretInternalSegmentCancel() {
        segmentCancel(false);
    }

    /**
     * M3: {@code PathingControlManager.preTick} brez procesa vsak tick prekliče segment. Upstream
     * je vsakič oddal {@code CANCELED}, tudi ko ni bilo ničesar za preklicati — pri mirujočem NPC-ju
     * to pomeni dogodek na tick in izgubljeno stanje "prispel/neuspel" v poslušalcih. Dogodek se
     * zdaj odda samo, če je bila pot, naslednji segment ali iskanje, ali ob izrecnem preklicu
     * ({@link #cancelEverything()}).
     */
    private void segmentCancel(boolean explicit) {
        boolean hadSomething;
        synchronized (pathPlanLock) {
            AbstractNodeCostSearch search = inProgress;
            hadSomething = current != null || next != null || search != null;
            if (search != null) {
                search.cancel();
            }
            if (current != null) {
                current = null;
                next = null;
                baritone.getInputOverrideHandler().clearAllKeys();
            }
        }
        if (hadSomething || explicit) {
            queuePathEvent(PathEvent.CANCELED);
        }
    }

    @Override
    public void forceCancel() { // exposed on public api because :sob:
        cancelEverything();
        secretInternalSegmentCancel();
        synchronized (pathCalcLock) {
            inProgress = null;
        }
    }

    public CalculationContext secretInternalGetCalculationContext() {
        return context;
    }

    public Optional<Double> estimatedTicksToGoal() {
        BetterBlockPos currentPos = ctx.feetPos();
        if (goal == null || currentPos == null || startPosition == null) {
            return Optional.empty();
        }
        if (goal.isInGoal(ctx.feetPos())) {
            resetEstimatedTicksToGoal();
            return Optional.of(0.0);
        }
        if (ticksElapsedSoFar == 0) {
            return Optional.empty();
        }
        double current = goal.heuristic(currentPos.x, currentPos.y, currentPos.z);
        double start = goal.heuristic(startPosition.x, startPosition.y, startPosition.z);
        if (current == start) {// can't check above because current and start can be equal even if currentPos and startPosition are not
            return Optional.empty();
        }
        double eta = Math.abs(current - goal.heuristic()) * ticksElapsedSoFar / Math.abs(start - current);
        return Optional.of(eta);
    }

    private void resetEstimatedTicksToGoal() {
        resetEstimatedTicksToGoal(expectedSegmentStart);
    }

    private void resetEstimatedTicksToGoal(BlockPos start) {
        resetEstimatedTicksToGoal(new BetterBlockPos(start));
    }

    private void resetEstimatedTicksToGoal(BetterBlockPos start) {
        ticksElapsedSoFar = 0;
        startPosition = start;
    }

    /**
     * See issue #209
     *
     * @return The starting {@link BlockPos} for a new path
     */
    public BetterBlockPos pathStart() { // TODO move to a helper or util class
        BetterBlockPos feet = ctx.feetPos();
        if (!MovementHelper.canWalkOn(ctx, feet.down())) {
            if (ctx.entity().onGround) {
                double playerX = ctx.entity().posX;
                double playerZ = ctx.entity().posZ;
                ArrayList<BetterBlockPos> closest = new ArrayList<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        closest.add(new BetterBlockPos(feet.x + dx, feet.y, feet.z + dz));
                    }
                }
                closest.sort(Comparator.comparingDouble(pos -> ((pos.x + 0.5D) - playerX) * ((pos.x + 0.5D) - playerX) + ((pos.z + 0.5D) - playerZ) * ((pos.z + 0.5D) - playerZ)));
                for (int i = 0; i < 4; i++) {
                    BetterBlockPos possibleSupport = closest.get(i);
                    double xDist = Math.abs((possibleSupport.x + 0.5D) - playerX);
                    double zDist = Math.abs((possibleSupport.z + 0.5D) - playerZ);
                    if (xDist > 0.8 && zDist > 0.8) {
                        // can't possibly be sneaking off of this one, we're too far away
                        continue;
                    }
                    if (MovementHelper.canWalkOn(ctx, possibleSupport.down()) && MovementHelper.canWalkThrough(ctx, possibleSupport) && MovementHelper.canWalkThrough(ctx, possibleSupport.up())) {
                        // this is plausible
                        //logDebug("Faking path start assuming player is standing off the edge of a block");
                        return possibleSupport;
                    }
                }

            } else {
                // !onGround
                // we're in the middle of a jump
                if (MovementHelper.canWalkOn(ctx, feet.down().down())) {
                    //logDebug("Faking path start assuming player is midair and falling");
                    return feet.down();
                }
            }
        }
        return feet;
    }

    /**
     * In a new thread, pathfind to target blockpos
     *
     * @param start
     * @param talkAboutIt
     */
    private void findPathInNewThread(final BlockPos start, final boolean talkAboutIt, CalculationContext context) {
        // this must be called with synchronization on pathCalcLock!
        // actually, we can check this, muahaha
        if (!Thread.holdsLock(pathCalcLock)) {
            throw new IllegalStateException("Must be called with synchronization on pathCalcLock");
            // why do it this way? it's already indented so much that putting the whole thing in a synchronized(pathCalcLock) was just too much lol
        }
        if (inProgress != null) {
            throw new IllegalStateException("Already doing it"); // should have been checked by caller
        }
        if (!context.safeForThreadedUse) {
            throw new IllegalStateException("Improper context thread safety level");
        }
        Goal goal = this.goal;
        if (goal == null) {
            logDebug("no goal"); // TODO should this be an exception too? definitely should be checked by caller
            return;
        }
        long primaryTimeout;
        long failureTimeout;
        if (current == null) {
            primaryTimeout = baritone.getSettings().primaryTimeoutMS.value;
            failureTimeout = baritone.getSettings().failureTimeoutMS.value;
        } else {
            primaryTimeout = baritone.getSettings().planAheadPrimaryTimeoutMS.value;
            failureTimeout = baritone.getSettings().planAheadFailureTimeoutMS.value;
        }
        AbstractNodeCostSearch pathfinder = createPathfinder(start, goal, current == null ? null : current.getPath(), context);
        if (!Objects.equals(pathfinder.getGoal(), goal)) { // will return the exact same object if simplification didn't happen
            logDebug("Simplifying " + goal.getClass() + " to GoalXZ due to distance");
        }
        inProgress = pathfinder;
        searchesStarted++;
        try {
            Baritone.submitSearch(() -> runSearch(pathfinder, start, goal, talkAboutIt, primaryTimeout, failureTimeout), searchPriority());
        } catch (RejectedExecutionException ex) {
            // M1.11: vrsta iskanj je polna; iskanje se šteje kot neuspelo, naslednji tick poskusi znova
            inProgress = null;
            lastSearchMicros = 0;
            lastSearchResult = "queue_full";
            queuePathEvent(PathEvent.CALC_FAILED);
            logDebug("Search queue full, path calculation rejected");
        }
    }

    private void runSearch(AbstractNodeCostSearch pathfinder, BlockPos start, Goal goal, boolean talkAboutIt, long primaryTimeout, long failureTimeout) {
        {
            if (talkAboutIt) {
                logDebug("Starting to search for path from " + start + " to " + goal);
            }

            long t0 = System.nanoTime();
            PathCalculationResult calcResult = pathfinder.calculate(primaryTimeout, failureTimeout);
            lastSearchMicros = (System.nanoTime() - t0) / 1000L;
            lastSearchResult = calcResult.getType().name().toLowerCase(java.util.Locale.ROOT);
            if (calcResult.getType() == PathCalculationResult.Type.FAILURE || calcResult.getType() == PathCalculationResult.Type.EXCEPTION) {
                si.ladja.npcbaritone.core.SearchStats.FAILED.incrementAndGet();
            }
            synchronized (pathPlanLock) {
                Optional<PathExecutor> executor = calcResult.getPath().map(p -> new PathExecutor(PathingBehavior.this, p));
                if (current == null) {
                    if (executor.isPresent()) {
                        if (executor.get().getPath().positions().contains(expectedSegmentStart)) {
                            queuePathEvent(PathEvent.CALC_FINISHED_NOW_EXECUTING);
                            current = executor.get();
                            resetEstimatedTicksToGoal(start);
                        } else {
                            logDebug("Warning: discarding orphan path segment with incorrect start");
                        }
                    } else {
                        if (calcResult.getType() != PathCalculationResult.Type.CANCELLATION && calcResult.getType() != PathCalculationResult.Type.EXCEPTION) {
                            // don't dispatch CALC_FAILED on cancellation
                            queuePathEvent(PathEvent.CALC_FAILED);
                        }
                    }
                } else {
                    if (next == null) {
                        if (executor.isPresent()) {
                            if (executor.get().getPath().getSrc().equals(current.getPath().getDest())) {
                                queuePathEvent(PathEvent.NEXT_SEGMENT_CALC_FINISHED);
                                next = executor.get();
                            } else {
                                logDebug("Warning: discarding orphan next segment with incorrect start");
                            }
                        } else {
                            planAheadFailedFrom = start;
                            planAheadFailedGoal = goal;
                            queuePathEvent(PathEvent.NEXT_CALC_FAILED);
                        }
                    } else {
                        //throw new IllegalStateException("I have no idea what to do with this path");
                        // no point in throwing an exception here, and it gets it stuck with inProgress being not null
                        logDirect("Warning: PathingBehaivor illegal state! Discarding invalid path!");
                    }
                }
                if (talkAboutIt && current != null && current.getPath() != null) {
                    if (goal.isInGoal(current.getPath().getDest())) {
                        logDebug("Finished finding a path from " + start + " to " + goal + ". " + current.getPath().getNumNodesConsidered() + " nodes considered");
                    } else {
                        logDebug("Found path segment from " + start + " towards " + goal + ". " + current.getPath().getNumNodesConsidered() + " nodes considered");
                    }
                }
                synchronized (pathCalcLock) {
                    inProgress = null;
                }
            }
        }
    }

    /**
     * D-013: svež kontekst z omejenim posnetkom chunkov okoli začetka in cilja
     * (rob {@code npcSnapshotMarginChunks}). Cilji brez položaja dobijo celoten posnetek.
     */
    private CalculationContext newSearchContext(BlockPos start, Goal goal) {
        int margin = baritone.getSettings().npcSnapshotMarginChunks.value;
        ChunkSnapshot.Bounds bounds = ChunkSnapshot.Bounds.ALL;
        if (goal instanceof IGoalRenderPos) {
            BlockPos g = ((IGoalRenderPos) goal).getGoalPos();
            bounds = ChunkSnapshot.Bounds.around(start.getX(), start.getZ(), g.getX(), g.getZ(), margin);
        } else if (goal instanceof GoalXZ) {
            GoalXZ g = (GoalXZ) goal;
            bounds = ChunkSnapshot.Bounds.around(start.getX(), start.getZ(), g.getX(), g.getZ(), margin);
        }
        long t0 = System.nanoTime();
        CalculationContext c = new CalculationContext(baritone, bounds);
        si.ladja.npcbaritone.core.SearchStats.SNAPSHOT_NANOS.add(System.nanoTime() - t0);
        si.ladja.npcbaritone.core.SearchStats.SNAPSHOT_CHUNKS.add(c.bsi.loadedChunkCount());
        return c;
    }

    /**
     * M5.1: prednost iskanja = kvadrat razdalje do najbližjega igralca (bližji prej); brez
     * igralcev vsi enako (FIFO).
     */
    private long searchPriority() {
        net.minecraft.entity.EntityLiving e = ctx.entity();
        double best = Double.MAX_VALUE;
        for (net.minecraft.entity.player.EntityPlayer p : e.world.playerEntities) {
            best = Math.min(best, p.getDistanceSq(e));
        }
        return best == Double.MAX_VALUE ? Long.MAX_VALUE / 2 : (long) Math.min(best, Long.MAX_VALUE / 4);
    }

    private static AbstractNodeCostSearch createPathfinder(BlockPos start, Goal goal, IPath previous, CalculationContext context) {
        Goal transformed = goal;
        if (context.settings.simplifyUnloadedYCoord.value && goal instanceof IGoalRenderPos) {
            BlockPos pos = ((IGoalRenderPos) goal).getGoalPos();
            if (!context.bsi.worldContainsLoadedChunk(pos.getX(), pos.getZ())) {
                transformed = new GoalXZ(pos.getX(), pos.getZ());
            }
        }
        Favoring favoring = new Favoring(context.getBaritone().getEntityContext(), previous, context);
        return new AStarPathFinder(start.getX(), start.getY(), start.getZ(), transformed, favoring, context);
    }

}
