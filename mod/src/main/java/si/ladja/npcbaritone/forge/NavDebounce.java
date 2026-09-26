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

/**
 * M6.1 (D-018): pravila, kdaj klic {@code tryMoveTo*}/{@code clearPath} iz vanilla AI taska
 * sproži novo iskanje. Taski kličejo navigator pogosto (vsak tick ali nekaj tickov) z istim
 * ali skoraj istim ciljem; brez tega bi vsak klic pomenil novo A* iskanje. Čista logika brez
 * sveta, zato headless test ({@code NavDebounceTest}).
 *
 * <ul>
 *     <li>isti cilj (Chebyshev ≤ tolerance) med iskanjem ali hojo → {@link Action#KEEP}</li>
 *     <li>isti cilj po prihodu, noge še v cilju → {@link Action#KEEP}</li>
 *     <li>isti cilj po neuspehu v zadnjih {@value #FAILED_RETRY_TICKS} tickih →
 *     {@link Action#REJECT} (vanilla vrne {@code false}, task izbere drug cilj)</li>
 *     <li>{@code clearPath} ne prekliče takoj: pot miruje (pavza); isti cilj v
 *     {@value #CLEAR_DEBOUNCE_TICKS} tickih → {@link Action#RESUME} brez iskanja, sicer pravi preklic</li>
 *     <li>vse ostalo → {@link Action#NEW}</li>
 * </ul>
 */
final class NavDebounce {

    static final int CLEAR_DEBOUNCE_TICKS = 10;
    static final int FAILED_RETRY_TICKS = 20;

    enum Action { NEW, KEEP, RESUME, REJECT }

    BlockPos goalPos;
    private int pausedAt = -1;
    private int failedAt = -1;

    boolean paused() {
        return pausedAt >= 0;
    }

    static boolean sameTarget(BlockPos a, BlockPos b, int tolerance) {
        return a != null && b != null
                && Math.abs(a.getX() - b.getX()) <= tolerance
                && Math.abs(a.getY() - b.getY()) <= tolerance
                && Math.abs(a.getZ() - b.getZ()) <= tolerance;
    }

    /**
     * @param state     trenutno stanje navigacije
     * @param inGoalNow ali so noge zdaj v cilju, ki bi ga zahteva postavila
     */
    Action request(BlockPos pos, int tolerance, int now, NavStatus.State state, boolean inGoalNow) {
        if (sameTarget(pos, goalPos, tolerance)) {
            if (paused()) {
                if (now - pausedAt <= CLEAR_DEBOUNCE_TICKS) {
                    pausedAt = -1;
                    return Action.RESUME;
                }
            } else {
                switch (state) {
                    case MOVING:
                    case SEARCHING:
                        return Action.KEEP;
                    case ARRIVED:
                        if (inGoalNow) {
                            return Action.KEEP;
                        }
                        break;
                    case FAILED:
                        if (failedAt >= 0 && now - failedAt < FAILED_RETRY_TICKS) {
                            return Action.REJECT;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        goalPos = pos;
        pausedAt = -1;
        failedAt = -1;
        return Action.NEW;
    }

    /** {@code clearPath}: pavza, če je cilj; preklic pride šele v {@link #tick}. */
    void clear(int now) {
        if (goalPos != null && pausedAt < 0) {
            pausedAt = now;
        }
    }

    /** Pozabi cilj brez pavze (izrecen ukaz, npr. {@code goTo} iz API). */
    void reset() {
        goalPos = null;
        pausedAt = -1;
        failedAt = -1;
    }

    /** @return true, ko je pavza potekla in je treba pot res preklicati */
    boolean tick(int now, NavStatus.State state) {
        if (state == NavStatus.State.FAILED) {
            if (failedAt < 0) {
                failedAt = now;
            }
        } else {
            failedAt = -1;
        }
        if (paused() && now - pausedAt > CLEAR_DEBOUNCE_TICKS) {
            pausedAt = -1;
            goalPos = null;
            return true;
        }
        return false;
    }
}
