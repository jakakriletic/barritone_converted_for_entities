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

import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalNear;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalXZ;
import si.ladja.npcbaritone.harness.BootstrapOnce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * M7.1: vanilla pogodba "!noPath() ⇒ getPath() != null" med Baritonovim iskanjem. CNPC
 * {@code EntityLivingWrapper.getNavigationPath} kliče {@code getPath().getFinalPathPoint()},
 * kadar {@code noPath()} vrne false.
 */
public class NavPathContractTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    @Test
    public void placeholderPointsAtGoalAndIsNotFinished() {
        BlockPos goal = new BlockPos(12, 64, -7);
        Path p = BaritonePathNavigate.placeholder(goal, null);
        assertEquals(1, p.getCurrentPathLength());
        assertFalse(p.isFinished());
        PathPoint f = p.getFinalPathPoint();
        assertEquals(goal, new BlockPos(f.x, f.y, f.z));
    }

    @Test
    public void goalPositionFromBlockAndNearGoals() {
        assertEquals(new BlockPos(1, 2, 3), BaritonePathNavigate.goalPos(new GoalBlock(1, 2, 3)));
        assertEquals(new BlockPos(4, 5, 6), BaritonePathNavigate.goalPos(new GoalNear(new BlockPos(4, 5, 6), 2)));
        assertNull(BaritonePathNavigate.goalPos(new GoalXZ(7, 8)));
        assertNull(BaritonePathNavigate.goalPos(null));
    }
}
