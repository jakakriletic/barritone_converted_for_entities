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

import net.minecraft.util.math.BlockPos;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.event.events.PathEvent;
import si.ladja.npcbaritone.core.api.event.listener.AbstractGameEventListener;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.behavior.PathingBehavior;

import java.util.Locale;

/**
 * M3: stanje navigacije ene pripete entitete, sestavljeno iz dogodkov poti
 * ({@link PathEvent}) in stanja {@link PathingBehavior}. Predhodnik javnega
 * {@code NavState} iz M6 (ARHITEKTURA §5); isti nabor stanj in kod razlogov.
 *
 * <p>Dogodke pošilja {@code PathingBehavior.dispatchEvents()} na strežniški niti, zato
 * polja niso sinhronizirana.
 */
public final class NavStatus implements AbstractGameEventListener {

    public enum State { IDLE, SEARCHING, MOVING, ARRIVED, FAILED }

    private final Baritone baritone;
    private PathEvent lastEvent;
    private long lastEventTick = -1;
    private final StringBuilder eventsInTick = new StringBuilder();
    private int replans;
    private int failures;
    private final Flags flags = new Flags();

    NavStatus(Baritone baritone) {
        this.baritone = baritone;
    }

    /** Pripne poslušalca na vodilo dogodkov instance. */
    static NavStatus install(Baritone baritone) {
        NavStatus status = new NavStatus(baritone);
        baritone.getGameEventHandler().registerEventListener(status);
        return status;
    }

    @Override
    public void onPathEvent(PathEvent event) {
        long now = worldTime();
        if (now != lastEventTick) {
            eventsInTick.setLength(0);
            lastEventTick = now;
        }
        if (eventsInTick.length() > 0) {
            eventsInTick.append('|');
        }
        eventsInTick.append(event.name());
        lastEvent = event;
        if (event == PathEvent.CALC_STARTED || event == PathEvent.NEXT_SEGMENT_CALC_STARTED) {
            replans++;
        }
        if (event == PathEvent.CALC_FAILED) {
            failures++;
        }
        flags.on(event, behavior().getGoal(), baritone.getEntityContext().feetPos(), behavior().lastSearchResult());
    }

    /**
     * Izid navigacije iz zaporedja dogodkov (čista logika, headless test).
     *
     * <p>Upstream posebnost (M5): prihod navadno opazi {@code CustomGoalProcess} (noge v cilju),
     * preden izvajalec zaključi pot — proces izgubi nadzor, tekoča pot se prekliče in namesto
     * {@code AT_GOAL} pride {@code CANCELED}. Zato se ob {@code CANCELED} preveri, ali so noge v
     * zadnjem znanem cilju. Enako {@code CANCELED} po {@code CALC_FAILED} ne pobriše neuspeha.
     * Ponastavi se samo ob novem poskusu ({@code CALC_STARTED}).
     */
    static final class Flags {
        boolean arrived;
        boolean failed;
        String failReason = "";
        /** Zadnji neprazen cilj (proces ga ob izgubi nadzora pobriše, preden pride CANCELED). */
        Goal trackedGoal;

        void on(PathEvent event, Goal goalNow, BlockPos feet, String searchResult) {
            if (goalNow != null) {
                trackedGoal = goalNow;
            }
            switch (event) {
                case CALC_STARTED:
                    arrived = false;
                    failed = false;
                    failReason = "";
                    break;
                case CALC_FINISHED_NOW_EXECUTING:
                    failed = false;
                    failReason = "";
                    break;
                case AT_GOAL:
                    arrived = true;
                    failed = false;
                    failReason = "";
                    break;
                case CALC_FAILED:
                    failed = true;
                    failReason = reasonFrom(searchResult);
                    break;
                case CANCELED:
                    if (!failed && trackedGoal != null && feet != null && trackedGoal.isInGoal(feet)) {
                        arrived = true;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private long worldTime() {
        return baritone.getEntityContext().entity().world.getTotalWorldTime();
    }

    public State state() {
        PathingBehavior p = behavior();
        if (p.getCurrent() != null) {
            return State.MOVING;
        }
        if (p.getInProgress().isPresent()) {
            return State.SEARCHING;
        }
        return decide(false, false, baritone.getCustomGoalProcess().isActive(), flags.failed, flags.arrived);
    }

    /**
     * Vrstni red je pomemben (M6): aktiven proces ima prednost pred izidom prejšnjega poskusa —
     * takoj po novem cilju zastavici še kažeta stari izid, dokler ne pride {@code CALC_STARTED};
     * {@code noPath()} bi sicer v istem ticku vrnil {@code true} in vanilla task bi obupal.
     */
    static State decide(boolean moving, boolean searching, boolean processActive, boolean failed, boolean arrived) {
        if (moving) {
            return State.MOVING;
        }
        if (searching || processActive) {
            return State.SEARCHING;
        }
        if (failed) {
            return State.FAILED;
        }
        if (arrived) {
            return State.ARRIVED;
        }
        return State.IDLE;
    }

    /** Koda razloga za {@link State#FAILED} ({@code no_path}, {@code queue_full}, ...); prazno sicer. */
    public String failReason() {
        return flags.failReason;
    }

    public int replans() {
        return replans;
    }

    public int failures() {
        return failures;
    }

    public PathEvent lastEvent() {
        return lastEvent;
    }

    /** Dogodki poti v danem ticku sveta, ločeni z {@code |} (stolpec {@code events} v tracu). */
    public String eventsAt(long worldTick) {
        return worldTick == lastEventTick ? eventsInTick.toString() : "";
    }

    public long lastSearchMicros() {
        return behavior().lastSearchMicros();
    }

    public Goal goal() {
        return behavior().getGoal();
    }

    private PathingBehavior behavior() {
        return baritone.getPathingBehavior();
    }

    /**
     * Izid iskanja → koda razloga (ARHITEKTURA §5). {@code failure} pomeni, da A* ni našel
     * poti do cilja ali segmenta v časovni omejitvi.
     */
    static String reasonFrom(String searchResult) {
        if (searchResult == null) {
            return "unknown";
        }
        switch (searchResult) {
            case "failure":
                return "no_path";
            case "queue_full":
                return "queue_full";
            case "exception":
                return "exception";
            default:
                return searchResult.toLowerCase(Locale.ROOT);
        }
    }
}
