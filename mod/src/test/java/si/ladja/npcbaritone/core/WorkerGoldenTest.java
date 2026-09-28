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

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.NpcProfile;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.api.pathing.calc.IPath;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;
import si.ladja.npcbaritone.core.api.pathing.movement.IMovement;
import si.ladja.npcbaritone.core.api.utils.BetterBlockPos;
import si.ladja.npcbaritone.core.api.utils.PathCalculationResult;
import si.ladja.npcbaritone.core.api.work.IWorkContext;
import si.ladja.npcbaritone.core.pathing.calc.AStarPathFinder;
import si.ladja.npcbaritone.core.pathing.movement.CalculationContext;
import si.ladja.npcbaritone.core.utils.BlockStateInterface;
import si.ladja.npcbaritone.core.utils.pathing.Favoring;
import si.ladja.npcbaritone.harness.BootstrapOnce;
import si.ladja.npcbaritone.harness.SyntheticWorld;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * M11.9 golden (D-031, D-033): iskanje poti workerja obide, česar roke ne smejo porušiti ali
 * postaviti, in računa z orodji iz inventarja. Svet kot {@link GoldenPathTest} (arena z obročem).
 */
public class WorkerGoldenTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    static {
        BootstrapOnce.ensure();
    }

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    /** Posnetek workerja za test; pogoji po koordinatah. */
    private static final class Work implements IWorkContext {
        interface Pos {
            boolean test(int x, int y, int z);
        }

        final Pos mayBreak;
        final Pos mayPlace;
        final List<ItemStack> tools;
        final boolean throwaway;

        Work(Pos mayBreak, Pos mayPlace, List<ItemStack> tools, boolean throwaway) {
            this.mayBreak = mayBreak;
            this.mayPlace = mayPlace;
            this.tools = tools;
            this.throwaway = throwaway;
        }

        @Override
        public boolean mayBreak(int x, int y, int z, IBlockState state) {
            return mayBreak.test(x, y, z);
        }

        @Override
        public boolean mayPlace(int x, int y, int z) {
            return mayPlace.test(x, y, z);
        }

        @Override
        public List<ItemStack> tools() {
            return tools;
        }

        @Override
        public boolean hasThrowaway() {
            return throwaway;
        }
    }

    private static final Work.Pos ALL = (x, y, z) -> true;
    private static final Work.Pos NONE = (x, y, z) -> false;

    private static SyntheticWorld arena() {
        return SyntheticWorld.create().ensureChunks(-2, -2, 2, 2).floor(-32, -32, 47, 47, 63, STONE)
                .wall(-32, -32, -32, 47, 64, 3, STONE).wall(47, -32, 47, 47, 64, 3, STONE)
                .wall(-32, -32, 47, -32, 64, 3, STONE).wall(-32, 47, 47, 47, 64, 3, STONE);
    }

    /** Zid čez vso areno na x = 10 (brez reže). */
    private static SyntheticWorld wall() {
        return arena().wall(10, -31, 10, 46, 64, 3, STONE);
    }

    /** Jarek brez dna čez vso areno na x = 10 (padec v praznino). */
    private static SyntheticWorld trench() {
        return arena().fill(10, 63, -31, 10, 63, 46, AIR);
    }

    private static Settings worker() {
        Settings s = NpcProfile.create();
        s.allowBreak.value = true; // profil "worker" (D-031)
        s.allowPlace.value = true;
        return s;
    }

    private static PathCalculationResult search(SyntheticWorld w, Settings s, IWorkContext work) {
        CalculationContext ctx = CalculationContext.headlessWorker(new BlockStateInterface(w.chunks(), null, null, s), work);
        AStarPathFinder f = new AStarPathFinder(0, 64, 0, new GoalBlock(20, 64, 0), new Favoring(null, ctx), ctx);
        return f.calculate(2000, 5000);
    }

    private static IPath toGoal(PathCalculationResult r) {
        assertEquals(r.getType().toString(), PathCalculationResult.Type.SUCCESS_TO_GOAL, r.getType());
        IPath p = r.getPath().get();
        p.sanityCheck();
        return p;
    }

    private static double cost(IPath p) {
        return p.movements().stream().mapToDouble(IMovement::getCost).sum();
    }

    @Test
    public void w1_breaksThroughWallWhenAllowed() {
        IPath p = toGoal(search(wall(), worker(), new Work(ALL, ALL, Collections.emptyList(), false)));
        assertTrue(p.positions().stream().anyMatch(b -> b.x == 10));
    }

    @Test
    public void w2_detoursToTheOnlyBreakableSection() {
        // porabnik dovoli rušenje samo pri z = 30..32 (npr. vse ostalo je ladijski trup)
        Work w = new Work((x, y, z) -> x != 10 || (z >= 30 && z <= 32), ALL, Collections.emptyList(), false);
        IPath p = toGoal(search(wall(), worker(), w));
        for (BetterBlockPos b : p.positions()) {
            if (b.x == 10) {
                assertTrue("prehod skozi zid samo v dovoljenem delu: " + b, b.z >= 30 && b.z <= 32);
            }
        }
    }

    @Test
    public void w3_noBreakPermissionNoPath() {
        assertNotEquals(PathCalculationResult.Type.SUCCESS_TO_GOAL,
                search(wall(), worker(), new Work(NONE, ALL, Collections.emptyList(), false)).getType());
    }

    @Test
    public void w4_pickaxeFromInventoryMakesBreakingCheaper() {
        double hand = cost(toGoal(search(wall(), worker(), new Work(ALL, ALL, Collections.emptyList(), false))));
        double pick = cost(toGoal(search(wall(), worker(),
                new Work(ALL, ALL, Collections.singletonList(new ItemStack(Items.DIAMOND_PICKAXE)), false))));
        assertTrue("kramp " + pick + " < roka " + hand, pick < hand);
    }

    @Test
    public void w5_bridgesTrenchWithThrowaway() {
        IPath p = toGoal(search(trench(), worker(), new Work(ALL, ALL, Collections.emptyList(), true)));
        assertTrue(p.positions().stream().anyMatch(b -> b.x == 10 && b.y == 64));
    }

    @Test
    public void w6_placeForbiddenNoBridge() {
        assertNotEquals(PathCalculationResult.Type.SUCCESS_TO_GOAL,
                search(trench(), worker(), new Work(ALL, NONE, Collections.emptyList(), true)).getType());
    }

    @Test
    public void w7_noThrowawayNoBridge() {
        assertNotEquals(PathCalculationResult.Type.SUCCESS_TO_GOAL,
                search(trench(), worker(), new Work(ALL, ALL, Collections.emptyList(), false)).getType());
    }
}
