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

import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
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
 * M4.8: geometrija tečaja T2 pred zagonom v igri — A* z NPC profilom na vsakem odseku in
 * preverbe, da je ovira res edina pot (vrata, lestev, voda). Plus čiste funkcije
 * {@link EntityInteractions} in podpisa blokov (M4.1, M4.9).
 */
public class CourseT2PathTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);
    private static SyntheticWorld world;
    private static final List<String> report = new ArrayList<>();

    @BeforeClass
    public static void build() {
        BootstrapOnce.ensure();
        world = SyntheticWorld.create().ensureChunks(-2, -2, 4, 8);
        int n = CourseT2.INSTANCE.build(new Course.Sink() {
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
        assertTrue(n > 5000);
    }

    private static PathCalculationResult search(Course.Segment s) {
        BlockStateInterface bsi = new BlockStateInterface(world.chunks(), null, null, NpcProfile.create());
        CalculationContext ctx = CalculationContext.headless(bsi);
        BlockPos a = s.start;
        AStarPathFinder f = new AStarPathFinder(a.getX(), a.getY(), a.getZ(), new GoalBlock(s.goal), new Favoring(null, ctx), ctx);
        PathCalculationResult r = f.calculate(2000, 5000);
        IPath p = r.getPath().orElse(null);
        report.add(String.format("T2 %2d %-28s %-16s len=%s %s", s.index, s.name, r.getType(),
                p == null ? "-" : p.movements().size(),
                p == null ? "" : p.movements().stream().map(m -> m.getClass().getSimpleName().replace("Movement", "")).distinct().collect(Collectors.joining(","))));
        return r;
    }

    private static Course.Segment seg(int index) {
        return CourseT2.INSTANCE.segments(ORIGIN).get(index - 1);
    }

    private static List<BetterBlockPos> path(int index) {
        PathCalculationResult r = search(seg(index));
        assertEquals("T2/" + index + " " + seg(index).name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.getType());
        return r.getPath().get().positions();
    }

    @Test
    public void everySegmentHasExpectedOutcome() {
        List<Course.Segment> segs = CourseT2.INSTANCE.segments(ORIGIN);
        assertEquals(10, segs.size());
        for (Course.Segment s : segs) {
            PathCalculationResult r = search(s);
            if (s.expect == Course.Expect.REACH) {
                assertEquals("T2/" + s.index + " " + s.name, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.getType());
            } else {
                assertNotEquals("T2/" + s.index + " mora biti nedosegljiv", PathCalculationResult.Type.SUCCESS_TO_GOAL, r.getType());
            }
        }
    }

    @Test
    public void doorsAndGateAreTheOnlyWay() {
        BlockPos door = seg(1).openables.get(0);
        assertTrue("T2/1 gre skozi vrata", path(1).contains(new BetterBlockPos(door)));
        BlockPos gate = seg(3).openables.get(0);
        assertTrue("T2/3 gre skozi ograjna vrata", path(3).contains(new BetterBlockPos(gate)));
        // vrata so res zaprta in obrnjena čez prehod
        IBlockState d = world.get(door);
        assertEquals(Blocks.OAK_DOOR, d.getBlock());
        assertFalse(d.getValue(BlockDoor.OPEN));
        assertEquals(EnumFacing.EAST, d.getValue(BlockDoor.FACING));
        assertFalse(world.get(gate).getValue(BlockFenceGate.OPEN));
    }

    @Test
    public void laddersAreUsedAndNoDeepDrops() {
        BlockPos ladderUp = CourseT2.lane(ORIGIN, 3).add(9, 2, 0);
        assertTrue("T2/4 pleza po lestvi", path(4).contains(new BetterBlockPos(ladderUp)));
        List<BetterBlockPos> down = path(5);
        assertTrue("T2/5 gre po lestvi", down.contains(new BetterBlockPos(CourseT2.lane(ORIGIN, 4).add(9, 2, 0))));
        for (int i = 1; i < down.size(); i++) {
            assertTrue("T2/5 pade " + (down.get(i - 1).y - down.get(i).y), down.get(i - 1).y - down.get(i).y <= 3);
        }
    }

    @Test
    public void lavaAndCactiAreAvoided() {
        for (BetterBlockPos p : path(8)) {
            IBlockState s = world.get(p);
            assertFalse("T2/8 skozi lavo " + p, s != null && s.getBlock() == Blocks.LAVA);
            IBlockState below = world.get(p.down());
            assertFalse("T2/8 nad lavo " + p, below != null && below.getBlock() == Blocks.LAVA);
        }
        for (BetterBlockPos p : path(9)) {
            IBlockState s = world.get(p);
            assertFalse("T2/9 skozi kaktus " + p, s != null && s.getBlock() == Blocks.CACTUS);
        }
    }

    @Test
    public void tenBlockFallLandsInWater() {
        List<BetterBlockPos> p = path(10);
        boolean bigDropIntoWater = false;
        for (int i = 1; i < p.size(); i++) {
            int drop = p.get(i - 1).y - p.get(i).y;
            if (drop > 3) {
                IBlockState at = world.get(p.get(i));
                assertTrue("T2/10 velik padec ne sme pristati na kamnu: " + p.get(i),
                        at != null && at.getBlock() == Blocks.WATER);
                bigDropIntoWater = true;
            }
        }
        assertTrue("T2/10 mora pasti v vodo", bigDropIntoWater);
    }

    // ------------------------------------------------------------ M4.1 in M4.9 čiste funkcije

    @Test
    public void onlyClosedWoodenDoorsAndGatesAreOpenable() {
        si.ladja.npcbaritone.core.api.Settings def = NpcProfile.create();
        IBlockState oak = Blocks.OAK_DOOR.getDefaultState();
        assertTrue(EntityInteractions.isClosedOpenable(oak.withProperty(BlockDoor.OPEN, false), def));
        assertFalse(EntityInteractions.isClosedOpenable(oak.withProperty(BlockDoor.OPEN, true), def));
        assertFalse(EntityInteractions.isClosedOpenable(Blocks.IRON_DOOR.getDefaultState(), def));
        assertTrue(EntityInteractions.isClosedOpenable(Blocks.OAK_FENCE_GATE.getDefaultState(), def));
        assertFalse(EntityInteractions.isClosedOpenable(Blocks.OAK_FENCE_GATE.getDefaultState().withProperty(BlockFenceGate.OPEN, true), def));
        assertFalse(EntityInteractions.isClosedOpenable(Blocks.TRAPDOOR.getDefaultState(), def));
        assertFalse(EntityInteractions.isClosedOpenable(Blocks.STONE.getDefaultState(), def));
    }

    /** D-039 (CNPC U3/U4): NONE ne odpre ničesar, ALL tudi železna vrata. */
    @Test
    public void openableFollowsInstanceDoorMode() {
        si.ladja.npcbaritone.core.api.Settings none = NpcProfile.create();
        none.npcOpenDoors.value = false;
        assertFalse(EntityInteractions.isClosedOpenable(Blocks.OAK_DOOR.getDefaultState(), none));
        assertFalse(EntityInteractions.isClosedOpenable(Blocks.OAK_FENCE_GATE.getDefaultState(), none));
        assertFalse(EntityInteractions.isClosedOpenable(Blocks.IRON_DOOR.getDefaultState(), none));
        si.ladja.npcbaritone.core.api.Settings all = NpcProfile.create();
        all.npcOpenIronDoors.value = true;
        assertTrue(EntityInteractions.isClosedOpenable(Blocks.IRON_DOOR.getDefaultState(), all));
        assertFalse(EntityInteractions.isClosedOpenable(Blocks.IRON_DOOR.getDefaultState().withProperty(BlockDoor.OPEN, true), all));
        assertTrue(EntityInteractions.isClosedOpenable(Blocks.OAK_DOOR.getDefaultState(), all));
    }

    @Test
    public void signatureIgnoresOnlyDoorOpening() {
        IBlockState closed = Blocks.OAK_DOOR.getDefaultState().withProperty(BlockDoor.OPEN, false);
        assertEquals(CourseRunner.normalized(closed), CourseRunner.normalized(closed.withProperty(BlockDoor.OPEN, true)));
        IBlockState gate = Blocks.OAK_FENCE_GATE.getDefaultState();
        assertEquals(CourseRunner.normalized(gate), CourseRunner.normalized(gate.withProperty(BlockFenceGate.OPEN, true)));
        assertNotEquals(CourseRunner.normalized(closed), CourseRunner.normalized(Blocks.AIR.getDefaultState()));
        assertNotEquals(CourseRunner.normalized(closed),
                CourseRunner.normalized(closed.withProperty(BlockDoor.FACING, EnumFacing.SOUTH)));
        assertTrue(CourseRunner.isOpen(gate.withProperty(BlockFenceGate.OPEN, true)));
        assertFalse(CourseRunner.isOpen(closed));
    }

    @AfterClass
    public static void writeReport() throws IOException {
        File dir = new File(System.getProperty("npcb.reportDir", "build/reports/npcb"));
        dir.mkdirs();
        Files.write(new File(dir, "course-t2-headless.txt").toPath(), report, StandardCharsets.UTF_8);
    }
}
