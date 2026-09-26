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
import net.minecraft.util.math.BlockPos;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.NpcProfile;
import si.ladja.npcbaritone.core.api.pathing.calc.IPath;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;
import si.ladja.npcbaritone.core.api.pathing.movement.IMovement;
import si.ladja.npcbaritone.core.api.utils.BetterBlockPos;
import si.ladja.npcbaritone.core.api.utils.PathCalculationResult;
import si.ladja.npcbaritone.core.pathing.calc.AStarPathFinder;
import si.ladja.npcbaritone.core.pathing.movement.CalculationContext;
import si.ladja.npcbaritone.core.pathing.movement.EntitySize;
import si.ladja.npcbaritone.core.utils.BlockStateInterface;
import si.ladja.npcbaritone.core.utils.pathing.Favoring;
import si.ladja.npcbaritone.harness.BootstrapOnce;
import si.ladja.npcbaritone.harness.SyntheticWorld;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * M8.8: golden tereni G1–G12 (M1) in dva nova (G13 široka odprtina, G14 nizka luknja) za
 * vse kombinacije širin 0,3 / 0,6 / 1,2 / 2,0 in višin 0,9 / 1,8 / 2,6 / 3,6 (16 velikosti).
 * Pričakovani izid je zapisan kot pravilo nad {@code sideSpace}/{@code heightBlocks}, ne kot
 * seznam, da je razvidno, zakaj velikost gre ali ne gre.
 *
 * <p>Model (D-028): entiteta je okvir {@code (2·sideSpace+1) × heightBlocks} blokov na
 * sredini bloka nog; širina 1,2 in 2,0 potrebujeta 3 stolpce, 0,3 in 0,6 enega.
 * Poročilo: {@code build/reports/npcb/golden-sizes.txt}.
 */
public class GoldenSizeTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    static {
        BootstrapOnce.ensure();
    }

    static final float[] WIDTHS = {0.3F, 0.6F, 1.2F, 2.0F};
    static final float[] HEIGHTS = {0.9F, 1.8F, 2.6F, 3.6F};

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState WATER = Blocks.WATER.getDefaultState();
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    private static final List<String> report = new ArrayList<>();

    private static List<EntitySize> sizes() {
        List<EntitySize> l = new ArrayList<>();
        for (float w : WIDTHS) {
            for (float h : HEIGHTS) {
                l.add(new EntitySize(w, h));
            }
        }
        return l;
    }

    @Test
    public void sizeModel() {
        assertEquals(0, new EntitySize(0.3F, 0.9F).sideSpace);
        assertEquals(0, new EntitySize(0.6F, 1.8F).sideSpace);
        assertEquals(0, new EntitySize(1.0F, 1.8F).sideSpace);
        assertEquals(1, new EntitySize(1.2F, 1.8F).sideSpace);
        assertEquals(1, new EntitySize(2.0F, 1.8F).sideSpace);
        assertEquals(1, new EntitySize(3.0F, 1.8F).sideSpace);
        assertEquals(2, new EntitySize(3.2F, 1.8F).sideSpace);
        assertEquals(1, new EntitySize(0.6F, 0.9F).heightBlocks);
        assertEquals(2, new EntitySize(0.6F, 1.95F).heightBlocks);
        assertEquals(2, new EntitySize(0.6F, 2.0F).heightBlocks);
        assertEquals(3, new EntitySize(0.6F, 2.6F).heightBlocks);
        assertEquals(4, new EntitySize(0.6F, 3.6F).heightBlocks);
        // CNPC: 0.6F / 5F * size, 1.8F / 5F * size
        assertEquals(4, new EntitySize(0.6F / 5F * 10, 1.8F / 5F * 10).heightBlocks);
        assertEquals(1, new EntitySize(0.6F / 5F * 10, 1.8F / 5F * 10).sideSpace);
        assertTrue(new EntitySize(0.6F / 5F * 3, 1.8F / 5F * 3).isStandard());
        assertTrue(new EntitySize(1.2F, 3.6F).canDescend());
        assertFalse(new EntitySize(2.0F, 1.8F).canDescend());
        assertTrue(new EntitySize(0.3F, 0.9F).canDescend());
        // hitra preverba izvajalca poti se ujema z modelom
        for (int w = 1; w <= 400; w++) {
            for (int h = 1; h <= 500; h++) {
                float fw = w / 100F;
                float fh = h / 100F;
                assertEquals(fw + "x" + fh, new EntitySize(fw, fh).isStandard(), EntitySize.isStandard(fw, fh));
            }
        }
    }

    // ------------------------------------------------------------------ tereni

    @Test
    public void g01_flat30() {
        for (EntitySize e : sizes()) {
            Run r = search("G1 ravnina 30", e, flat(), pos(0, 64, 0), new GoalBlock(30, 64, 0));
            r.assertToGoal();
            assertEquals(r.name + " straight line", 30, r.path.movements().size());
            for (String t : r.types()) {
                assertTrue(r.name + " movement " + t, t.equals("MovementTraverse") || (e.sideSpace == 0 && t.equals("MovementDiagonal")));
            }
        }
    }

    @Test
    public void g02_wallWithGap() {
        SyntheticWorld w = flat().wall(10, -20, 10, 20, 64, 3, STONE).set(10, 64, 3, AIR).set(10, 65, 3, AIR);
        for (EntitySize e : sizes()) {
            Run r = search("G2 zid z režo 1x2", e, w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
            r.assertToGoal();
            boolean fits = e.sideSpace == 0 && e.heightBlocks <= 2;
            assertEquals(r.name + " through gap", fits, r.path.positions().contains(pos(10, 64, 3)));
            if (!fits) {
                r.assertNoneInside(10, 10, 64, 66, -20, 20);
                assertTrue(r.name + " detour around wall end", r.path.positions().stream().anyMatch(p -> p.x == 10 && Math.abs(p.z - 0) > 20));
            }
        }
    }

    @Test
    public void g03_wallDetour() {
        SyntheticWorld w = flat().wall(10, -5, 10, 5, 64, 3, STONE);
        for (EntitySize e : sizes()) {
            Run r = search("G3 zid brez reže", e, w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
            r.assertToGoal();
            r.assertNoneInside(10 - e.sideSpace, 10 + e.sideSpace, 64, 66, -5 - e.sideSpace, 5 + e.sideSpace);
        }
    }

    @Test
    public void g04_step1() {
        SyntheticWorld w = flat().fill(5, 64, -32, 47, 64, 47, STONE);
        for (EntitySize e : sizes()) {
            Run r = search("G4 stopnica 1", e, w, pos(0, 64, 0), new GoalBlock(10, 65, 0));
            r.assertToGoal();
            assertEquals(r.name, 1, r.count("MovementAscend"));
        }
    }

    @Test
    public void g05_stairsUp3() {
        SyntheticWorld w = flat()
                .fill(5, 64, -32, 47, 64, 47, STONE)
                .fill(8, 65, -32, 47, 65, 47, STONE)
                .fill(11, 66, -32, 47, 66, 47, STONE);
        for (EntitySize e : sizes()) {
            Run r = search("G5 stopnice gor 3", e, w, pos(0, 64, 0), new GoalBlock(15, 67, 0));
            r.assertToGoal();
            assertEquals(r.name, 3, r.count("MovementAscend"));
        }
    }

    @Test
    public void g06_drop3() {
        SyntheticWorld w = flat().fill(-3, 64, -3, 3, 66, 3, STONE);
        for (EntitySize e : sizes()) {
            Run r = search("G6 padec 3", e, w, pos(0, 67, 0), new GoalBlock(10, 64, 0));
            if (e.canDescend()) {
                r.assertToGoal();
                assertTrue(r.name + " must descend with Descend/Fall", r.count("MovementDescend") + r.count("MovementFall") >= 1);
            } else {
                // širina 2,0: sredina bi po spustu padla izven ciljnega stolpca (D-028)
                assertNotEquals(r.name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
            }
        }
    }

    @Test
    public void g07_drop4NoWater() {
        SyntheticWorld w = flat().fill(-3, 64, -3, 3, 67, 3, STONE);
        for (EntitySize e : sizes()) {
            Run r = search("G7 padec 4 brez vode", e, w, pos(0, 68, 0), new GoalBlock(10, 64, 0));
            assertNotEquals(r.name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
            if (r.path != null) {
                for (BetterBlockPos p : r.path.positions()) {
                    assertEquals(r.name + " left the platform at " + p, 68, p.y);
                }
            }
        }
    }

    @Test
    public void g08_pool5() {
        // jarek širine 5 (x 10..14), globine 2, čez ves naložen svet: obhoda ni
        SyntheticWorld w = flat().fill(10, 62, -32, 14, 63, 47, WATER).fill(10, 61, -32, 14, 61, 47, STONE);
        for (EntitySize e : sizes()) {
            Run r = search("G8 bazen širine 5", e, w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
            Run flat = search("G8 referenca ravnina", e, flat(), pos(0, 64, 0), new GoalBlock(20, 64, 0));
            if (poolCrossable(e)) {
                r.assertToGoal();
                assertTrue(r.name + " water must cost more than flat: " + r.cost + " vs " + flat.cost, r.cost > flat.cost);
            } else {
                assertNotEquals(r.name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
            }
        }
    }

    /** G8: kdo pride čez bazen globine 2 (glej {@link #g08_pool5}); pravilo določi prvi tek. */
    static boolean poolCrossable(EntitySize e) {
        return e.canDescend();
    }

    @Test
    public void g09_corridor1x2() {
        SyntheticWorld w = flat()
                .fill(0, 64, -1, 12, 65, -1, STONE)
                .fill(0, 64, 1, 12, 65, 1, STONE)
                .fill(0, 66, -1, 12, 66, 1, STONE)
                .fill(-1, 64, -1, -1, 66, 1, STONE);
        for (EntitySize e : sizes()) {
            Run r = search("G9 hodnik 1x2", e, w, pos(0, 64, 0), new GoalBlock(11, 64, 0));
            if (e.sideSpace == 0 && e.heightBlocks <= 2) {
                r.assertToGoal();
                for (BetterBlockPos p : r.path.positions()) {
                    assertEquals(r.name + " stays in corridor z=0", 0, p.z);
                }
            } else {
                // entiteta, ki ne gre v hodnik, se iz njega tudi ne premakne (A2: FAILED, ne tavanje)
                assertNotEquals(r.name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
                assertTrue(r.name + " must not move", r.path == null || r.path.movements().isEmpty());
            }
        }
    }

    @Test
    public void g10_goalInClosedBox() {
        SyntheticWorld w = flat().fill(19, 64, -1, 21, 66, 1, STONE).set(20, 64, 0, AIR).set(20, 65, 0, AIR);
        for (EntitySize e : sizes()) {
            Run r = search("G10 cilj v zaprti škatli", e, w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
            assertNotEquals(r.name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
            assertNotEquals(r.name, PathCalculationResult.Type.EXCEPTION, r.result.getType());
            assertTrue(r.name + " must end within failure timeout, took " + r.millis + " ms", r.millis < FAILURE_TIMEOUT_MS + 1000);
        }
    }

    @Test
    public void g11_goalBeyondLoadedEdge() {
        for (EntitySize e : sizes()) {
            Run r = search("G11 cilj za robom", e, flat(), pos(0, 64, 0), new GoalBlock(200, 64, 0));
            assertEquals(r.name, PathCalculationResult.Type.SUCCESS_SEGMENT, r.result.getType());
            BetterBlockPos dest = r.path.getDest();
            assertTrue(r.name + " segment ends inside loaded area: " + dest, dest.x <= 47);
            assertTrue(r.name + " segment gets close to the edge: " + dest, dest.x >= 30);
        }
    }

    @Test
    public void g12_noBreakingWithAllowBreakFalse() {
        SyntheticWorld w = arena().wall(10, -31, 10, 46, 64, 3, STONE);
        for (EntitySize e : sizes()) {
            Run r = search("G12 zid, brez rušenja", e, w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
            assertNotEquals(r.name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
            if (r.path != null) {
                r.assertNoneInside(10, 10, 64, 66, -31, 46);
            }
        }
    }

    @Test
    public void g13_wideOpening() {
        // zid čez vso areno z odprtino 3 široko in 4 visoko pri z 2..4: gredo vse velikosti
        SyntheticWorld w = arena().wall(10, -31, 10, 46, 64, 5, STONE).fill(10, 64, 2, 10, 67, 4, AIR);
        for (EntitySize e : sizes()) {
            Run r = search("G13 odprtina 3x4", e, w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
            r.assertToGoal();
            assertTrue(r.name + " through the opening", r.path.positions().contains(pos(10, 64, 3))
                    || (e.sideSpace == 0 && r.path.positions().stream().anyMatch(p -> p.x == 10 && p.z >= 2 && p.z <= 4)));
        }
    }

    @Test
    public void g14_lowHole() {
        // zid čez vso areno z luknjo 1x1 pri z=3: gre samo entiteta z višino ≤ 1 in širino ≤ 1
        SyntheticWorld w = arena().wall(10, -31, 10, 46, 64, 3, STONE).set(10, 64, 3, AIR);
        for (EntitySize e : sizes()) {
            Run r = search("G14 luknja 1x1", e, w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
            if (e.sideSpace == 0 && e.heightBlocks == 1) {
                r.assertToGoal();
                assertTrue(r.name + " through the hole", r.path.positions().contains(pos(10, 64, 3)));
            } else {
                assertNotEquals(r.name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
                if (r.path != null) {
                    r.assertNoneInside(10, 10, 64, 66, -31, 46);
                }
            }
        }
    }

    // ------------------------------------------------------------------ orodja

    private static final long PRIMARY_TIMEOUT_MS = 2000;
    private static final long FAILURE_TIMEOUT_MS = 5000;

    private static SyntheticWorld flat() {
        return SyntheticWorld.create().ensureChunks(-2, -2, 2, 2).floor(-32, -32, 47, 47, 63, STONE);
    }

    private static SyntheticWorld arena() {
        return flat()
                .wall(-32, -32, -32, 47, 64, 3, STONE).wall(47, -32, 47, 47, 64, 3, STONE)
                .wall(-32, -32, 47, -32, 64, 3, STONE).wall(-32, 47, 47, 47, 64, 3, STONE);
    }

    private static BetterBlockPos pos(int x, int y, int z) {
        return new BetterBlockPos(x, y, z);
    }

    static Run search(String terrain, EntitySize size, SyntheticWorld w, BlockPos start, Goal goal) {
        BlockStateInterface bsi = new BlockStateInterface(w.chunks(), null, null, NpcProfile.create());
        CalculationContext ctx = CalculationContext.headless(bsi, size);
        AStarPathFinder finder = new AStarPathFinder(start.getX(), start.getY(), start.getZ(), goal, new Favoring(null, ctx), ctx);
        long t0 = System.nanoTime();
        PathCalculationResult result = finder.calculate(PRIMARY_TIMEOUT_MS, FAILURE_TIMEOUT_MS);
        Run r = new Run(terrain + " " + size, result, (System.nanoTime() - t0) / 1_000_000L);
        report.add(r.toString());
        return r;
    }

    static final class Run {
        final String name;
        final PathCalculationResult result;
        final IPath path;
        final long millis;
        final double cost;

        Run(String name, PathCalculationResult result, long millis) {
            this.name = name;
            this.result = result;
            this.path = result.getPath().orElse(null);
            this.millis = millis;
            this.cost = path == null ? Double.NaN : path.movements().stream().mapToDouble(IMovement::getCost).sum();
        }

        List<String> types() {
            return path.movements().stream().map(m -> m.getClass().getSimpleName()).collect(Collectors.toList());
        }

        long count(String type) {
            return types().stream().filter(type::equals).count();
        }

        void assertToGoal() {
            assertEquals(name + ": " + this, PathCalculationResult.Type.SUCCESS_TO_GOAL, result.getType());
            path.sanityCheck();
        }

        void assertNoneInside(int x1, int x2, int y1, int y2, int z1, int z2) {
            for (BetterBlockPos p : path.positions()) {
                boolean inside = p.x >= x1 && p.x <= x2 && p.y >= y1 && p.y <= y2 && p.z >= z1 && p.z <= z2;
                assertFalse(name + ": path goes through solid block " + p, inside);
            }
        }

        @Override
        public String toString() {
            LinkedHashSet<String> kinds = path == null ? new LinkedHashSet<>() : new LinkedHashSet<>(types());
            return String.format(Locale.ROOT, "%-44s %-16s %5d ms  len=%3s  cost=%8.2f  dest=%s  %s",
                    name, result.getType(), millis,
                    path == null ? "-" : String.valueOf(path.movements().size()), cost,
                    path == null ? "-" : path.getDest(),
                    kinds.stream().map(k -> k.replace("Movement", "")).collect(Collectors.joining(",")));
        }
    }

    @AfterClass
    public static void writeReport() throws IOException {
        File dir = new File(System.getProperty("npcb.reportDir", "build/reports/npcb"));
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        Files.write(new File(dir, "golden-sizes.txt").toPath(), report, StandardCharsets.UTF_8);
    }
}
