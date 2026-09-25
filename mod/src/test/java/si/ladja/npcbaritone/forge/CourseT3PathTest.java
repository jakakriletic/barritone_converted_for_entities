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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * M8.9: geometrija in pričakovani izidi tečaja T3 headless — isti {@link CourseT3#build} na
 * sintetičnem "superflat" svetu (trava pod izhodiščem, kot v igri, zato so obhodi med
 * progami mogoči tudi tukaj), A* z NPC profilom in velikostjo odseka. Vsak odsek mora dati
 * izid iz tabele {@link CourseT3}; test izpiše vse neskladne odseke naenkrat.
 */
public class CourseT3PathTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static SyntheticWorld world;
    private static final List<String> report = new ArrayList<>();

    @BeforeClass
    public static void build() {
        BootstrapOnce.ensure();
        world = SyntheticWorld.create().ensureChunks(-2, -2, 8, 8);
        // superflat pod tečajem: bedrock, 2x zemlja, trava (vrh na y-1 izhodišča)
        world.fill(-32, 60, -32, 143, 60, 143, Blocks.BEDROCK.getDefaultState());
        world.fill(-32, 61, -32, 143, 62, 143, Blocks.DIRT.getDefaultState());
        world.fill(-32, 63, -32, 143, 63, 143, Blocks.GRASS.getDefaultState());
        int n = CourseT3.INSTANCE.build(new Course.Sink() {
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
        assertTrue(n > 2000);
    }

    @Test
    public void everySegmentMatchesItsSizeExpectation() {
        List<Course.Segment> segs = CourseT3.INSTANCE.segments(ORIGIN);
        assertEquals(CourseT3.SIZES.length * 20, segs.size());
        List<String> mismatches = new ArrayList<>();
        for (Course.Segment s : segs) {
            EntitySize size = CourseT3.npcSize(s.npcSize);
            BlockStateInterface bsi = new BlockStateInterface(world.chunks(), null, null, NpcProfile.create());
            CalculationContext ctx = CalculationContext.headless(bsi, size);
            AStarPathFinder f = new AStarPathFinder(s.start.getX(), s.start.getY(), s.start.getZ(), new GoalBlock(s.goal), new Favoring(null, ctx), ctx);
            PathCalculationResult r = f.calculate(2000, 5000);
            IPath p = r.getPath().orElse(null);
            boolean reached = r.getType() == PathCalculationResult.Type.SUCCESS_TO_GOAL;
            String problem = null;
            if (s.expect == Course.Expect.REACH && !reached) {
                problem = "expected REACH, got " + r.getType();
            } else if (s.expect == Course.Expect.FAIL && reached) {
                problem = "expected FAIL, got path";
            } else if (reached) {
                for (BetterBlockPos pos : p.positions()) {
                    if (s.forbiddenColumn != null && pos.x == s.forbiddenColumn.getX() && pos.z == s.forbiddenColumn.getZ()) {
                        problem = "goes through forbidden column " + pos;
                    }
                }
                for (int i = 1; i < p.positions().size(); i++) {
                    int drop = p.positions().get(i - 1).y - p.positions().get(i).y;
                    if (drop > 3 && !(s.name.contains("v vodo"))) {
                        problem = "drops " + drop + " blocks";
                    }
                }
            }
            String line = String.format(Locale.ROOT, "%3d %-44s %-8s %-6s %-16s len=%-3s %s%s", s.index, s.name, size, s.expect, r.getType(),
                    p == null ? "-" : p.movements().size(),
                    p == null ? "" : p.movements().stream().map(m -> m.getClass().getSimpleName().replace("Movement", "")).distinct().collect(Collectors.joining(",")),
                    problem == null ? "" : "   <-- " + problem);
            report.add(line);
            if (p != null && System.getProperty("npcb.t3positions") != null && s.npcSize == Integer.getInteger("npcb.t3positions")) {
                report.add("      " + p.positions().stream().map(q -> q.x + "," + q.y + "," + q.z).collect(Collectors.joining(" ")));
            }
            if (problem != null) {
                mismatches.add(line);
            }
        }
        assertTrue("T3 mismatches:\n" + String.join("\n", mismatches), mismatches.isEmpty());
    }

    @Test
    public void sizesAreCnpcScaled() {
        assertEquals(1, CourseT3.npcSize(1).heightBlocks);
        assertEquals(2, CourseT3.npcSize(3).heightBlocks);
        assertTrue(CourseT3.npcSize(5).isStandard());
        assertEquals(3, CourseT3.npcSize(7).heightBlocks);
        assertEquals(0, CourseT3.npcSize(7).sideSpace);
        assertEquals(4, CourseT3.npcSize(10).heightBlocks);
        assertEquals(1, CourseT3.npcSize(10).sideSpace);
    }

    @AfterClass
    public static void writeReport() throws IOException {
        File dir = new File(System.getProperty("npcb.reportDir", "build/reports/npcb"));
        dir.mkdirs();
        Files.write(new File(dir, "course-t3-headless.txt").toPath(), report, StandardCharsets.UTF_8);
    }
}
