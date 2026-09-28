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

package si.ladja.npcbaritone.core;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.NpcProfile;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalComposite;
import si.ladja.npcbaritone.core.api.utils.BetterBlockPos;
import si.ladja.npcbaritone.core.api.utils.PathCalculationResult;
import si.ladja.npcbaritone.core.behavior.PathingBehavior;
import si.ladja.npcbaritone.core.pathing.calc.AStarPathFinder;
import si.ladja.npcbaritone.core.pathing.movement.CalculationContext;
import si.ladja.npcbaritone.core.utils.BlockStateInterface;
import si.ladja.npcbaritone.core.utils.pathing.Favoring;
import si.ladja.npcbaritone.core.world.ChunkSnapshot;
import si.ladja.npcbaritone.harness.BootstrapOnce;
import si.ladja.npcbaritone.harness.SyntheticWorld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * D-041 (CNPC M7.5 {@code NpcNavRange}): zahteve vanilla {@code PathNavigate} vidijo samo
 * entiteta ± ({@code FOLLOW_RANGE} + 8) blokov, kot vanilla {@code ChunkCache}. Daljši cilj da
 * segment do meje posnetka (D-014), ne neuspeha.
 */
public class SearchRangeTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    static {
        BootstrapOnce.ensure();
    }

    private static void assertBounds(int minX, int minZ, int maxX, int maxZ, ChunkSnapshot.Bounds b) {
        assertEquals(b.toString(), minX, b.minX);
        assertEquals(b.toString(), minZ, b.minZ);
        assertEquals(b.toString(), maxX, b.maxX);
        assertEquals(b.toString(), maxZ, b.maxZ);
    }

    @Test
    public void withoutRangeBoundsAreUnchanged() {
        BlockPos start = new BlockPos(0, 64, 0);
        ChunkSnapshot.Bounds b = PathingBehavior.searchBounds(start, new GoalBlock(100, 64, 0), 8, 0);
        assertBounds(-8, -8, 6 + 8, 8, b);
        assertTrue(PathingBehavior.searchBounds(start, new GoalComposite(new GoalBlock(1, 64, 1)), 8, 0).isAll());
    }

    @Test
    public void rangeClipsToStartPlusRangePlusEight() {
        BlockPos start = new BlockPos(0, 64, 0);
        // 16 + 8 = 24 blokov: chunki -2..1
        assertBounds(-2, -2, 1, 1, PathingBehavior.searchBounds(start, new GoalBlock(100, 64, 0), 8, 16));
        // cilj brez položaja: samo kvadrat dosega (prej celotna mapa)
        assertBounds(-2, -2, 1, 1, PathingBehavior.searchBounds(start, new GoalComposite(new GoalBlock(1, 64, 1)), 8, 16));
        // doseg večji od pravokotnika ga ne poveča
        assertBounds(-8, -8, 8, 8, PathingBehavior.searchBounds(start, new GoalBlock(5, 64, 5), 8, 200));
    }

    private static PathCalculationResult search(SyntheticWorld w, int rangeBlocks) {
        BlockPos start = new BlockPos(0, 64, 0);
        GoalBlock goal = new GoalBlock(40, 64, 0);
        ChunkSnapshot.Bounds bounds = PathingBehavior.searchBounds(start, goal, 8, rangeBlocks);
        Settings s = NpcProfile.create();
        BlockStateInterface bsi = new BlockStateInterface(ChunkSnapshot.copy(w.chunks(), bounds), null, null, s);
        CalculationContext ctx = CalculationContext.headless(bsi);
        return new AStarPathFinder(0, 64, 0, goal, new Favoring(null, ctx), ctx).calculate(2000, 5000);
    }

    @Test
    public void farGoalGivesSegmentToSnapshotEdge() {
        SyntheticWorld w = SyntheticWorld.create().ensureChunks(-2, -2, 2, 2)
                .floor(-32, -32, 47, 47, 63, Blocks.STONE.getDefaultState());
        assertEquals(PathCalculationResult.Type.SUCCESS_TO_GOAL, search(w, 0).getType());

        PathCalculationResult clipped = search(w, 1); // 1 + 8 = 9 blokov: chunka -1..0, x ≤ 15
        assertEquals(PathCalculationResult.Type.SUCCESS_SEGMENT, clipped.getType());
        BetterBlockPos end = clipped.getPath().get().getDest();
        assertTrue("segment konča na meji posnetka: " + end, end.x <= 15 && end.x >= 10);
    }
}
