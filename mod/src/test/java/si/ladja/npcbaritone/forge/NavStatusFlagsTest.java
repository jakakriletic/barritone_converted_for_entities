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
import org.junit.Test;
import si.ladja.npcbaritone.core.api.event.events.PathEvent;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * M5: izid navigacije iz dogodkov. Zaporedja so iz sledi T1/T2/T4 — prihod pride kot
 * {@code CANCELED} (proces opazi cilj pred izvajalcem), ne kot {@code AT_GOAL}.
 */
public class NavStatusFlagsTest {

    private static final GoalBlock G = new GoalBlock(10, 64, 0);
    private static final BlockPos AT_G = new BlockPos(10, 64, 0);
    private static final BlockPos AWAY = new BlockPos(3, 64, 0);

    @Test
    public void arrivalReportedAsCanceledIsArrived() {
        NavStatus.Flags f = new NavStatus.Flags();
        f.on(PathEvent.CALC_STARTED, G, AWAY, "none");
        f.on(PathEvent.CALC_FINISHED_NOW_EXECUTING, G, AWAY, "success_to_goal");
        // proces izgubi nadzor: cilj je že null, ko pride CANCELED
        f.on(PathEvent.CANCELED, null, AT_G, "success_to_goal");
        assertTrue(f.arrived);
        assertFalse(f.failed);
    }

    @Test
    public void cancelAfterFailureKeepsFailure() {
        NavStatus.Flags f = new NavStatus.Flags();
        f.on(PathEvent.CALC_STARTED, G, AWAY, "none");
        f.on(PathEvent.CALC_FAILED, G, AWAY, "failure");
        f.on(PathEvent.CANCELED, null, AWAY, "failure");
        assertTrue(f.failed);
        assertEquals("no_path", f.failReason);
        assertFalse(f.arrived);
    }

    @Test
    public void cancelAwayFromGoalIsNeither() {
        NavStatus.Flags f = new NavStatus.Flags();
        f.on(PathEvent.CALC_STARTED, G, AWAY, "none");
        f.on(PathEvent.CALC_FINISHED_NOW_EXECUTING, G, AWAY, "success_to_goal");
        f.on(PathEvent.CANCELED, null, AWAY, "success_to_goal");
        assertFalse(f.arrived);
        assertFalse(f.failed);
    }

    @Test
    public void newAttemptResetsOutcome() {
        NavStatus.Flags f = new NavStatus.Flags();
        f.on(PathEvent.CALC_STARTED, G, AWAY, "none");
        f.on(PathEvent.AT_GOAL, G, AT_G, "success_to_goal");
        assertTrue(f.arrived);
        GoalBlock other = new GoalBlock(40, 64, 0);
        f.on(PathEvent.CALC_STARTED, other, AT_G, "success_to_goal");
        assertFalse(f.arrived);
        // prihod v stari cilj ne šteje več
        f.on(PathEvent.CANCELED, null, AT_G, "success_to_goal");
        assertFalse(f.arrived);
    }
}
