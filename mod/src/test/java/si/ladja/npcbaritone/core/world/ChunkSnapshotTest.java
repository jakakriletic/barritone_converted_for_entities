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

package si.ladja.npcbaritone.core.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.harness.BootstrapOnce;
import si.ladja.npcbaritone.harness.SyntheticWorld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** M1.5 / D-013: omejena kopija mape chunkov. */
public class ChunkSnapshotTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    private static Long2ObjectMap<Chunk> live() {
        return SyntheticWorld.create().ensureChunks(-10, -10, 10, 10).chunks(); // 441 chunkov
    }

    @Test
    public void boundsAroundCoversBothPointsPlusMargin() {
        ChunkSnapshot.Bounds b = ChunkSnapshot.Bounds.around(-1, 5, 40, -20, 2);
        // x: -1>>4 = -1, 40>>4 = 2; z: -20>>4 = -2, 5>>4 = 0
        assertEquals(-3, b.minX);
        assertEquals(4, b.maxX);
        assertEquals(-4, b.minZ);
        assertEquals(2, b.maxZ);
        assertEquals(8L * 7L, b.area());
        assertTrue(b.contains(-3, 2));
        assertFalse(b.contains(5, 0));
    }

    @Test
    public void smallBoundsIterateRectangle() {
        Long2ObjectMap<Chunk> live = live();
        ChunkSnapshot.Bounds b = new ChunkSnapshot.Bounds(-1, -1, 1, 1);
        Long2ObjectMap<Chunk> copy = ChunkSnapshot.copy(live, b);
        assertEquals(9, copy.size());
        assertSame("snapshot shares chunk objects", live.get(ChunkPos.asLong(1, 1)), copy.get(ChunkPos.asLong(1, 1)));
        assertFalse(copy.containsKey(ChunkPos.asLong(2, 0)));
    }

    @Test
    public void largeBoundsIterateLoadedMap() {
        Long2ObjectMap<Chunk> live = live();
        ChunkSnapshot.Bounds b = new ChunkSnapshot.Bounds(-5, -1000, 1000, 1000); // površina >> 441
        Long2ObjectMap<Chunk> copy = ChunkSnapshot.copy(live, b);
        assertEquals(16 * 21, copy.size()); // x -5..10 (16) × z -10..10 (21)
        assertTrue(copy.containsKey(ChunkPos.asLong(-5, -10)));
        assertFalse(copy.containsKey(ChunkPos.asLong(-6, 0)));
    }

    @Test
    public void allCopiesEverythingIntoNewMap() {
        Long2ObjectMap<Chunk> live = live();
        Long2ObjectMap<Chunk> copy = ChunkSnapshot.copy(live, ChunkSnapshot.Bounds.ALL);
        assertEquals(live.size(), copy.size());
        assertNotSame(live, copy);
        live.clear();
        assertEquals("copy is independent of later changes to the live map", 441, copy.size());
    }
}
