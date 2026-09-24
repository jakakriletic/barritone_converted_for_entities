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

package si.ladja.npcbaritone.harness;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** M0.8 / merilo A4: headless harness deluje brez zagona igre. */
public class HarnessTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    static {
        BootstrapOnce.ensure(); // statična polja spodaj berejo Blocks.*
    }

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    @Test
    public void bootstrapRegistersVanillaBlocks() {
        assertNotNull(Blocks.STONE);
        assertTrue("block registry too small", Block.REGISTRY.getKeys().size() > 200);
        assertEquals("minecraft:stone", Block.REGISTRY.getNameForObject(Blocks.STONE).toString());
    }

    @Test
    public void setThenGetRoundTrip() {
        SyntheticWorld w = SyntheticWorld.create().set(1, 4, 1, STONE);
        assertSame(STONE, w.get(1, 4, 1));
        assertSame(AIR, w.get(1, 5, 1));
        assertSame(AIR, w.get(2, 4, 1));
    }

    @Test
    public void negativeCoordinatesLandInCorrectChunk() {
        SyntheticWorld w = SyntheticWorld.create().set(-17, 70, -1, STONE);
        Chunk c = w.chunks().get(ChunkPos.asLong(-2, -1));
        assertNotNull("chunk (-2,-1) missing", c);
        assertEquals(-2, c.x);
        assertEquals(-1, c.z);
        assertSame(STONE, w.get(-17, 70, -1));
        assertSame(AIR, w.get(-18, 70, -1));
        assertNull("x=-16 is chunk -1, not loaded", w.get(-16, 70, -1));
    }

    @Test
    public void unloadedIsNullAndOutOfHeightIsAir() {
        SyntheticWorld w = SyntheticWorld.create().ensureChunks(0, 0, 0, 0);
        assertNull(w.get(16, 64, 0));
        assertFalse(w.isLoaded(16, 0));
        assertTrue(w.isLoaded(15, 15));
        assertSame(AIR, w.get(0, -1, 0));
        assertSame(AIR, w.get(0, 256, 0));
        assertThrows(IllegalArgumentException.class, () -> w.set(0, 256, 0, STONE));
    }

    @Test
    public void chunkMapHasBaritoneShape() {
        SyntheticWorld w = SyntheticWorld.create().set(33, 10, 5, STONE);
        Chunk c = w.chunks().get(ChunkPos.asLong(33 >> 4, 5 >> 4));
        assertNotNull(c);
        assertTrue("BlockStateInterface skips chunks with isLoaded()==false", c.isLoaded());
        assertSame(STONE, c.getBlockStorageArray()[10 >> 4].get(33 & 15, 10 & 15, 5 & 15));
    }

    @Test
    public void fillWallAndStairs() {
        SyntheticWorld w = SyntheticWorld.create()
                .floor(-8, -8, 8, 8, 63, STONE)
                .wall(3, -8, 3, 8, 64, 3, Blocks.COBBLESTONE.getDefaultState())
                .stairs(-5, 64, 0, EnumFacing.WEST, 3, 2, Blocks.PLANKS.getDefaultState());
        assertSame(STONE, w.get(-8, 63, 8));
        assertSame(STONE, w.get(8, 63, -8));
        assertSame(AIR, w.get(0, 64, 0));
        assertEquals(Blocks.COBBLESTONE, w.get(3, 66, 8).getBlock());
        assertSame(AIR, w.get(3, 67, 0));
        // stopnica i=2 na x=-7: visoka (2+1)*2 = 6 blokov, y 64..69
        assertEquals(Blocks.PLANKS, w.get(-7, 69, 0).getBlock());
        assertSame(AIR, w.get(-7, 70, 0));
        assertEquals(Blocks.PLANKS, w.get(-5, 65, 0).getBlock());
        assertSame(AIR, w.get(-5, 66, 0));
        // 17 x 17 plošča + 17 x 3 zid + (2 + 4 + 6) stopnice, vse v chunkih -1..0
        assertEquals(4, w.chunks().size());
    }

    @Test
    public void doorPassabilityReadsThroughIBlockAccess() {
        BlockPos lower = new BlockPos(2, 64, 2);
        IBlockState closedLower = Blocks.OAK_DOOR.getDefaultState()
                .withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.LOWER)
                .withProperty(BlockDoor.OPEN, false);
        IBlockState upper = Blocks.OAK_DOOR.getDefaultState()
                .withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.UPPER);
        SyntheticWorld w = SyntheticWorld.create().set(lower, closedLower).set(lower.up(), upper);
        assertFalse("closed door must not be passable", Blocks.OAK_DOOR.isPassable(w.access(), lower));
        w.set(lower, closedLower.withProperty(BlockDoor.OPEN, true));
        assertTrue("open door must be passable", Blocks.OAK_DOOR.isPassable(w.access(), lower));
        assertTrue("upper half reads OPEN from lower half", Blocks.OAK_DOOR.isPassable(w.access(), lower.up()));
    }

    /**
     * M0.9 ugotovitev, pripeta kot test: {@code Chunk.getBlockState(x,y,z)} najprej prebere
     * {@code world.getWorldType()}, zato na chunku brez sveta vrže NPE. Baritonov
     * {@code BlockStateInterface.get0} kliče prav to metodo, torej M1 bere iz
     * {@code ExtendedBlockStorage} ali pa posnetek dobi svet. Če ta test kdaj pade,
     * se je vanilla vedenje spremenilo in je treba odločitev preveriti.
     */
    @Test
    public void chunkGetBlockStateNeedsWorld() {
        SyntheticWorld w = SyntheticWorld.create().set(1, 4, 1, STONE);
        Chunk c = w.chunks().get(ChunkPos.asLong(0, 0));
        assertThrows(NullPointerException.class, () -> c.getBlockState(1, 4, 1));
    }
}
