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

import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.NpcProfile;
import si.ladja.npcbaritone.core.api.Settings;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * D-039 (CNPC U3/U4): vrata na instanco. Zid čez celo naloženo območje z enim prehodom na
 * (10, 64, 0); A* z NPC profilom in nastavitvama {@code npcOpenDoors}/{@code npcOpenIronDoors}.
 * Privzeto ({@code WOODEN}) je obnašanje M4 nespremenjeno.
 */
public class DoorModeTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    static {
        BootstrapOnce.ensure();
    }

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final BlockPos GAP = new BlockPos(10, 64, 0);
    private static final BlockPos START = new BlockPos(0, 64, 0);
    private static final BlockPos GOAL = new BlockPos(20, 64, 0);

    /** Ravnina z obročem in zidom x = 10 (3 visok), prehod GAP je prazen. */
    private static SyntheticWorld wallWithGap() {
        return SyntheticWorld.create().ensureChunks(-2, -2, 2, 2).floor(-32, -32, 47, 47, 63, STONE)
                .wall(-32, -32, -32, 47, 64, 3, STONE).wall(47, -32, 47, 47, 64, 3, STONE)
                .wall(-32, -32, 47, -32, 64, 3, STONE).wall(-32, 47, 47, 47, 64, 3, STONE)
                .fill(10, 64, -31, 10, 66, 46, STONE)
                .set(GAP, Blocks.AIR.getDefaultState()).set(GAP.up(), Blocks.AIR.getDefaultState());
    }

    /** Vrata v zidu x = 10, obrnjena proti vzhodu: zaprta zapirajo pot po osi X. */
    private static SyntheticWorld door(net.minecraft.block.Block block, boolean open) {
        IBlockState lower = block.getDefaultState().withProperty(BlockDoor.FACING, EnumFacing.EAST)
                .withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.LOWER).withProperty(BlockDoor.OPEN, open);
        IBlockState upper = block.getDefaultState().withProperty(BlockDoor.FACING, EnumFacing.EAST)
                .withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.UPPER);
        return wallWithGap().set(GAP, lower).set(GAP.up(), upper);
    }

    private static SyntheticWorld gate(boolean open) {
        return wallWithGap().set(GAP, Blocks.OAK_FENCE_GATE.getDefaultState()
                .withProperty(BlockFenceGate.FACING, EnumFacing.EAST).withProperty(BlockFenceGate.OPEN, open));
    }

    private static Settings mode(boolean wooden, boolean iron) {
        Settings s = NpcProfile.create();
        s.npcOpenDoors.value = wooden;
        s.npcOpenIronDoors.value = iron;
        return s;
    }

    private static PathCalculationResult search(SyntheticWorld w, Settings settings) {
        BlockStateInterface bsi = new BlockStateInterface(w.chunks(), null, null, settings);
        CalculationContext ctx = CalculationContext.headless(bsi);
        return new AStarPathFinder(START.getX(), START.getY(), START.getZ(), new GoalBlock(GOAL), new Favoring(null, ctx), ctx)
                .calculate(2000, 5000);
    }

    private static void assertThroughGap(String what, SyntheticWorld w, Settings s) {
        PathCalculationResult r = search(w, s);
        assertEquals(what, PathCalculationResult.Type.SUCCESS_TO_GOAL, r.getType());
        IPath path = r.getPath().get();
        assertTrue(what + ": pot gre skozi prehod", path.positions().contains(new BetterBlockPos(GAP)));
    }

    private static void assertBlocked(String what, SyntheticWorld w, Settings s) {
        assertNotEquals(what, PathCalculationResult.Type.SUCCESS_TO_GOAL, search(w, s).getType());
    }

    @Test
    public void defaultProfileIsWoodenAndUnchanged() {
        Settings s = NpcProfile.create();
        assertTrue(s.npcOpenDoors.value);
        assertEquals(Boolean.FALSE, s.npcOpenIronDoors.value);
        assertThroughGap("lesena zaprta, privzeto", door(Blocks.OAK_DOOR, false), s);
        assertBlocked("železna zaprta, privzeto", door(Blocks.IRON_DOOR, false), s);
        assertThroughGap("ograjna zaprta, privzeto", gate(false), s);
    }

    @Test
    public void noneOpensNothingButUsesOpenDoors() {
        Settings none = mode(false, false);
        assertBlocked("lesena zaprta, NONE", door(Blocks.OAK_DOOR, false), none);
        assertBlocked("ograjna zaprta, NONE", gate(false), none);
        assertThroughGap("lesena odprta, NONE", door(Blocks.OAK_DOOR, true), none);
        assertThroughGap("ograjna odprta, NONE", gate(true), none);
        assertThroughGap("železna odprta, NONE", door(Blocks.IRON_DOOR, true), none);
    }

    @Test
    public void allOpensIronDoors() {
        Settings all = mode(true, true);
        assertThroughGap("železna zaprta, ALL", door(Blocks.IRON_DOOR, false), all);
        assertThroughGap("lesena zaprta, ALL", door(Blocks.OAK_DOOR, false), all);
    }
}
