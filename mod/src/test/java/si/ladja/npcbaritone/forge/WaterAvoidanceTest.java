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

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;
import si.ladja.npcbaritone.core.api.utils.BetterBlockPos;
import si.ladja.npcbaritone.core.api.utils.PathCalculationResult;
import si.ladja.npcbaritone.core.pathing.calc.AStarPathFinder;
import si.ladja.npcbaritone.core.pathing.movement.CalculationContext;
import si.ladja.npcbaritone.core.utils.BlockStateInterface;
import si.ladja.npcbaritone.core.utils.pathing.Favoring;
import si.ladja.npcbaritone.harness.BootstrapOnce;
import si.ladja.npcbaritone.harness.SyntheticWorld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * D-040 (CNPC U5): profil {@code avoid_water} mora vodo res obiti, kadar je suh obvoz kratek,
 * in jo prečkati, kadar je obvoz zelo dolg — kot vanilla {@code PathNodeType.WATER} z malusom 8
 * (CNPC {@code setAvoidsWater(true)}). Reka x = 10..14 čez celo območje, suh prehod pri z = {@code bridgeZ}.
 */
public class WaterAvoidanceTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    static {
        BootstrapOnce.ensure();
    }

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState WATER = Blocks.WATER.getDefaultState();

    private static SyntheticWorld river(int depth, int bridgeZ) {
        SyntheticWorld w = SyntheticWorld.create().ensureChunks(-2, -2, 2, 2).floor(-32, -32, 47, 47, 63, STONE)
                .fill(10, 64 - depth, -32, 14, 63, 47, WATER)
                .fill(10, 63 - depth, -32, 14, 63 - depth, 47, STONE);
        return w.fill(10, 63, bridgeZ, 14, 63, bridgeZ, STONE);
    }

    /** Število točk poti z nogami v vodi ali na vodni gladini. */
    private static long wetNodes(SyntheticWorld w, Settings s) {
        BlockStateInterface bsi = new BlockStateInterface(w.chunks(), null, null, s);
        CalculationContext ctx = CalculationContext.headless(bsi);
        PathCalculationResult r = new AStarPathFinder(0, 64, 0, new GoalBlock(20, 64, 0), new Favoring(null, ctx), ctx)
                .calculate(2000, 5000);
        assertEquals(PathCalculationResult.Type.SUCCESS_TO_GOAL, r.getType());
        long wet = 0;
        for (BetterBlockPos p : r.getPath().get().positions()) {
            BlockPos below = p.down();
            if (w.get(p).getBlock() == Blocks.WATER || w.get(below).getBlock() == Blocks.WATER) {
                wet++;
            }
        }
        return wet;
    }

    private static Settings profile(String name) {
        return Attach.profileFor(NpcbConfig.defaults(), name);
    }

    @Test
    public void defaultProfileWadesThrough() {
        assertTrue(wetNodes(river(1, 10), profile(NpcbConfig.DEFAULT_PROFILE)) > 0);
        assertTrue(wetNodes(river(2, 10), profile(NpcbConfig.DEFAULT_PROFILE)) > 0);
    }

    @Test
    public void avoidWaterTakesShortDryDetour() {
        assertEquals("plitva voda, obvoz 10", 0, wetNodes(river(1, 10), profile("avoid_water")));
        assertEquals("globoka voda, obvoz 10", 0, wetNodes(river(2, 10), profile("avoid_water")));
    }

    /** Malus, ne prepoved: pri zelo dolgem obvozu gre čez vodo (vanilla prioriteta 8, ne -1). */
    @Test
    public void avoidWaterStillCrossesWhenDetourIsVeryLong() {
        assertTrue(wetNodes(river(1, 45), profile("avoid_water")) > 0);
    }
}
