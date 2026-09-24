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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.NpcProfile;
import si.ladja.npcbaritone.core.api.pathing.calc.IPath;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * M2.9: geometrija tečaja T1 je pravilna, preden se požene v igri — isti {@link CourseT1#build}
 * v {@link SyntheticWorld}, A* z NPC profilom na vsakem odseku. Odsek 10 mora biti
 * nedosegljiv, odsek 7 ne sme iti skozi režo 1,5, odsek 4 ne sme skočiti 4 bloke.
 */
public class CourseT1PathTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static SyntheticWorld world;
    private static final List<String> report = new ArrayList<>();

    @BeforeClass
    public static void build() {
        BootstrapOnce.ensure();
        // x -6..46, z 0..108 (+-5): chunki -1..3 x -1..7; tla pod progami postavi build
        world = SyntheticWorld.create().ensureChunks(-2, -2, 4, 8);
        int n = CourseT1.build(new CourseT1.Sink() {
            @Override
            public IBlockState get(BlockPos pos) {
                IBlockState s = world.get(pos);
                return s == null ? Blocks.AIR.getDefaultState() : s;
            }

            @Override
            public void set(BlockPos pos, IBlockState state) {
                world.set(pos, state);
            }
        }, ORIGIN);
        assertTrue(n > 1000);
    }

    private static PathCalculationResult search(CourseT1.Segment s) {
        BlockStateInterface bsi = new BlockStateInterface(world.chunks(), null, null, NpcProfile.create());
        CalculationContext ctx = CalculationContext.headless(bsi);
        BlockPos a = s.start;
        AStarPathFinder f = new AStarPathFinder(a.getX(), a.getY(), a.getZ(), new GoalBlock(s.goal), new Favoring(null, ctx), ctx);
        PathCalculationResult r = f.calculate(2000, 5000);
        IPath p = r.getPath().orElse(null);
        report.add(String.format("T1 %2d %-30s %-16s len=%s %s", s.index, s.name, r.getType(),
                p == null ? "-" : p.movements().size(),
                p == null ? "" : p.movements().stream().map(m -> m.getClass().getSimpleName().replace("Movement", "")).distinct().collect(Collectors.joining(","))));
        return r;
    }

    @Test
    public void everySegmentBehavesAsExpected() {
        List<CourseT1.Segment> segs = CourseT1.segments(ORIGIN);
        assertEquals(10, segs.size());
        for (CourseT1.Segment s : segs) {
            PathCalculationResult r = search(s);
            if (s.expect == CourseT1.Expect.REACH) {
                assertEquals("segment " + s.index + " " + s.name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.getType());
                IPath p = r.getPath().get();
                if (s.forbiddenColumn != null) {
                    for (BetterBlockPos pos : p.positions()) {
                        assertFalse("segment " + s.index + " goes through forbidden gap " + pos,
                                pos.x == s.forbiddenColumn.getX() && pos.z == s.forbiddenColumn.getZ());
                    }
                }
                for (int i = 1; i < p.positions().size(); i++) {
                    int drop = p.positions().get(i - 1).y - p.positions().get(i).y;
                    assertTrue("segment " + s.index + " drops " + drop + " blocks", drop <= 3);
                }
            } else {
                assertNotEquals("segment " + s.index + " must be unreachable", PathCalculationResult.Type.SUCCESS_TO_GOAL, r.getType());
            }
        }
    }

    @Test
    public void segment4PlatformIsReallyFourHigh() {
        CourseT1.Segment s = CourseT1.segments(ORIGIN).get(3);
        assertEquals(Blocks.STONE, world.get(s.start.down()).getBlock());
        assertEquals(Blocks.AIR, world.get(s.start).getBlock());
        assertEquals(Blocks.STONE, world.get(s.start.down(4)).getBlock());
        assertEquals(Blocks.STONE, world.get(s.start.down(5)).getBlock()); // tla
    }

    @AfterClass
    public static void writeReport() throws IOException {
        File dir = new File(System.getProperty("npcb.reportDir", "build/reports/npcb"));
        dir.mkdirs();
        Files.write(new File(dir, "course-t1-headless.txt").toPath(), report, StandardCharsets.UTF_8);
    }
}
