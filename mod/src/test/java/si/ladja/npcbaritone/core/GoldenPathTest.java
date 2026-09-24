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
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.api.pathing.calc.IPath;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;
import si.ladja.npcbaritone.core.api.pathing.movement.IMovement;
import si.ladja.npcbaritone.core.api.utils.BetterBlockPos;
import si.ladja.npcbaritone.core.api.utils.PathCalculationResult;
import si.ladja.npcbaritone.core.pathing.calc.AStarPathFinder;
import si.ladja.npcbaritone.core.pathing.movement.CalculationContext;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * M1.12 golden testi (D-022): A* z NPC profilom na sintetičnih terenih, brez igre in brez
 * entitete ({@link CalculationContext#headless}). Tereni G1–G12 iz README milestona M1.
 *
 * <p>Svet: kamnita ravnina na y=63 čez chunke -2..2 (x, z od -32 do 47); entiteta stoji na
 * y=64. Časovne meje so namenoma velike (2 s / 5 s), da je izid odvisen od terena, ne od
 * hitrosti stroja. Poročilo: {@code build/reports/npcb/golden.txt}.
 */
public class GoldenPathTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    static {
        BootstrapOnce.ensure();
    }

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState WATER = Blocks.WATER.getDefaultState();
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    private static final List<String> report = new ArrayList<>();

    // ------------------------------------------------------------------ tereni

    @Test
    public void g01_flat30() {
        Run r = search("G1 ravnina 30", flat(), pos(0, 64, 0), new GoalBlock(30, 64, 0));
        r.assertToGoal();
        for (String t : r.types()) {
            assertTrue("G1 unexpected movement " + t, t.equals("MovementTraverse") || t.equals("MovementDiagonal"));
        }
        assertEquals("straight line: 30 traverses", 30, r.path.movements().size());
    }

    @Test
    public void g02_wallWithGap() {
        SyntheticWorld w = flat().wall(10, -20, 10, 20, 64, 3, STONE).set(10, 64, 3, AIR).set(10, 65, 3, AIR);
        Run r = search("G2 zid z režo", w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
        r.assertToGoal();
        assertTrue("G2 must pass through the gap", r.path.positions().contains(pos(10, 64, 3)));
    }

    @Test
    public void g03_wallDetour() {
        SyntheticWorld w = flat().wall(10, -5, 10, 5, 64, 3, STONE);
        Run r = search("G3 zid brez reže, obhod", w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
        r.assertToGoal();
        assertTrue("G3 must go around the wall end (|z| > 5)", r.path.positions().stream()
                .anyMatch(p -> p.x == 10 && Math.abs(p.z) > 5));
        r.assertNoneInside(10, 10, 64, 66, -5, 5);
    }

    @Test
    public void g04_step1() {
        SyntheticWorld w = flat().fill(5, 64, -32, 47, 64, 47, STONE);
        Run r = search("G4 stopnica 1", w, pos(0, 64, 0), new GoalBlock(10, 65, 0));
        r.assertToGoal();
        assertEquals(1, r.count("MovementAscend"));
    }

    @Test
    public void g05_stairsUp3() {
        SyntheticWorld w = flat()
                .fill(5, 64, -32, 47, 64, 47, STONE)
                .fill(8, 65, -32, 47, 65, 47, STONE)
                .fill(11, 66, -32, 47, 66, 47, STONE);
        Run r = search("G5 stopnice gor 3", w, pos(0, 64, 0), new GoalBlock(15, 67, 0));
        r.assertToGoal();
        assertEquals(3, r.count("MovementAscend"));
    }

    @Test
    public void g06_drop3() {
        // plošča na y=64..66 (vrh 66), noge na 67; tla noge 64: padec 3
        SyntheticWorld w = flat().fill(-3, 64, -3, 3, 66, 3, STONE);
        Run r = search("G6 padec 3", w, pos(0, 67, 0), new GoalBlock(10, 64, 0));
        r.assertToGoal();
        assertTrue("G6 must descend with Descend/Fall", r.count("MovementDescend") + r.count("MovementFall") >= 1);
    }

    @Test
    public void g07_drop4NoWater() {
        // plošča na y=64..67, noge na 68: vsak spust je 4 bloke, maxFallHeightNoWater=3
        SyntheticWorld w = flat().fill(-3, 64, -3, 3, 67, 3, STONE);
        Run r = search("G7 padec 4 brez vode", w, pos(0, 68, 0), new GoalBlock(10, 64, 0));
        assertNotEquals("G7 must not reach the ground", PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
        if (r.path != null) {
            for (BetterBlockPos p : r.path.positions()) {
                assertEquals("G7 path left the platform at " + p, 68, p.y);
            }
        }
    }

    @Test
    public void g08_pool5() {
        // jarek širine 5 (x 10..14), globine 2, čez ves naložen svet: obhoda ni
        SyntheticWorld w = flat().fill(10, 62, -32, 14, 63, 47, WATER).fill(10, 61, -32, 14, 61, 47, STONE);
        Run r = search("G8 bazen širine 5", w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
        r.assertToGoal();
        Run flat = search("G8 referenca ravnina", flat(), pos(0, 64, 0), new GoalBlock(20, 64, 0));
        assertTrue("G8 water must cost more than flat: " + r.cost + " vs " + flat.cost, r.cost > flat.cost);
    }

    @Test
    public void g09_corridor1x2() {
        SyntheticWorld w = flat()
                .fill(0, 64, -1, 12, 65, -1, STONE)   // levi zid
                .fill(0, 64, 1, 12, 65, 1, STONE)     // desni zid
                .fill(0, 66, -1, 12, 66, 1, STONE)    // strop
                .fill(-1, 64, -1, -1, 66, 1, STONE);  // zadnja stena
        Run r = search("G9 hodnik 1x2 dolg 10", w, pos(0, 64, 0), new GoalBlock(11, 64, 0));
        r.assertToGoal();
        for (BetterBlockPos p : r.path.positions()) {
            assertEquals("G9 stays in corridor z=0", 0, p.z);
        }
    }

    @Test
    public void g10_goalInClosedBox() {
        SyntheticWorld w = flat().fill(19, 64, -1, 21, 66, 1, STONE).set(20, 64, 0, AIR).set(20, 65, 0, AIR);
        Run r = search("G10 cilj v zaprti škatli", w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
        assertNotEquals(PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
        assertNotEquals(PathCalculationResult.Type.EXCEPTION, r.result.getType());
        assertTrue("G10 search must end within failure timeout, took " + r.millis + " ms", r.millis < FAILURE_TIMEOUT_MS + 1000);
    }

    @Test
    public void g11_goalBeyondLoadedEdge() {
        Run r = search("G11 cilj za robom posnetka", flat(), pos(0, 64, 0), new GoalBlock(200, 64, 0));
        assertEquals(PathCalculationResult.Type.SUCCESS_SEGMENT, r.result.getType());
        BetterBlockPos dest = r.path.getDest();
        assertTrue("G11 segment ends inside loaded area: " + dest, dest.x <= 47);
        assertTrue("G11 segment gets close to the edge: " + dest, dest.x >= 30);
    }

    @Test
    public void g12_noBreakingWithAllowBreakFalse() {
        // zid čez ves naložen svet: brez rušenja ni poti. Arena je obdana z obročem, ker A*
        // sicer po pathingMaxChunkBorderFetch (50) poskusih vstopa v nenaložen chunk obupa,
        // preden bi se mu splačalo rušiti (drago: 2 x ~150 tickov z roko).
        SyntheticWorld w = arena().wall(10, -31, 10, 46, 64, 3, STONE);
        Run r = search("G12 allowBreak=false, zid", w, pos(0, 64, 0), new GoalBlock(20, 64, 0));
        assertNotEquals(PathCalculationResult.Type.SUCCESS_TO_GOAL, r.result.getType());
        if (r.path != null) {
            r.assertNoneInside(10, 10, 64, 66, -31, 46);
        }
        // kontrola: isti teren z allowBreak=true gre skozi zid (dokaz, da test vidi razliko in
        // da je profil res na instanco: obe iskanji tečeta v istem JVM z različnima profiloma, D-016)
        Settings breaking = NpcProfile.create();
        breaking.allowBreak.value = true;
        Run control = search("G12 kontrola allowBreak=true", w, pos(0, 64, 0), new GoalBlock(20, 64, 0), breaking);
        control.assertToGoal();
    }

    @Test
    public void npcProfileDefaults() {
        Settings s = NpcProfile.create();
        assertFalse(s.allowBreak.value);
        assertFalse(s.allowPlace.value);
        assertFalse(s.allowParkour.value);
        assertFalse(s.allowWaterBucketFall.value);
        assertEquals(3, (int) s.maxFallHeightNoWater.value);
        assertEquals(150L, (long) s.primaryTimeoutMS.value);
        assertFalse(s.chunkCaching.value);
    }

    // ------------------------------------------------------------------ orodja

    private static final long PRIMARY_TIMEOUT_MS = 2000;
    private static final long FAILURE_TIMEOUT_MS = 5000;

    private static SyntheticWorld flat() {
        return SyntheticWorld.create().ensureChunks(-2, -2, 2, 2).floor(-32, -32, 47, 47, 63, STONE);
    }

    /** Ravnina, obdana z 3 visokim obročem na robu naloženega območja. */
    private static SyntheticWorld arena() {
        return flat()
                .wall(-32, -32, -32, 47, 64, 3, STONE).wall(47, -32, 47, 47, 64, 3, STONE)
                .wall(-32, -32, 47, -32, 64, 3, STONE).wall(-32, 47, 47, 47, 64, 3, STONE);
    }

    private static BetterBlockPos pos(int x, int y, int z) {
        return new BetterBlockPos(x, y, z);
    }

    private static Run search(String name, SyntheticWorld w, BlockPos start, Goal goal) {
        return search(name, w, start, goal, NpcProfile.create());
    }

    private static Run search(String name, SyntheticWorld w, BlockPos start, Goal goal, Settings settings) {
        BlockStateInterface bsi = new BlockStateInterface(w.chunks(), null, null, settings);
        CalculationContext ctx = CalculationContext.headless(bsi);
        AStarPathFinder finder = new AStarPathFinder(start.getX(), start.getY(), start.getZ(), goal, new Favoring(null, ctx), ctx);
        long t0 = System.nanoTime();
        PathCalculationResult result = finder.calculate(PRIMARY_TIMEOUT_MS, FAILURE_TIMEOUT_MS);
        Run r = new Run(name, result, (System.nanoTime() - t0) / 1_000_000L);
        report.add(r.toString());
        return r;
    }

    private static final class Run {
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
            assertNotNull(path);
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
            Set<String> kinds = path == null ? new LinkedHashSet<>() : new LinkedHashSet<>(types());
            return String.format("%-32s %-16s %5d ms  len=%3s  cost=%8.2f  nodes=%6s  dest=%s  %s",
                    name, result.getType(), millis,
                    path == null ? "-" : String.valueOf(path.movements().size()), cost,
                    path == null ? "-" : String.valueOf(path.getNumNodesConsidered()),
                    path == null ? "-" : path.getDest(), kinds);
        }
    }

    @AfterClass
    public static void writeReport() throws IOException {
        File dir = new File(System.getProperty("npcb.reportDir", "build/reports/npcb"));
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        Files.write(new File(dir, "golden.txt").toPath(), report, StandardCharsets.UTF_8);
    }
}
