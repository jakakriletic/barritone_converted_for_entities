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

package si.ladja.npcbaritone.api.work;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * M11.2 (D-030, D-033): delovno območje je obvezno in vključno na obeh koncih — napaka za en blok
 * bi workerju tiho dovolila rušenje ob meji območja (M11 A2).
 */
public class WorkerSpecTest {

    @Test
    public void boxIsInclusiveAndOrderIndependent() {
        WorkArea a = WorkArea.box(new BlockPos(5, 70, 5), new BlockPos(0, 60, 0));
        assertTrue(a.contains(new BlockPos(0, 60, 0)));
        assertTrue(a.contains(new BlockPos(5, 70, 5)));
        assertTrue(a.contains(new BlockPos(3, 65, 2)));
        assertFalse(a.contains(new BlockPos(6, 65, 2)));
        assertFalse(a.contains(new BlockPos(3, 59, 2)));
        assertFalse(a.contains(new BlockPos(-1, 65, 2)));
        assertFalse(a.contains(new BlockPos(3, 65, 6)));
    }

    @Test
    public void unionContainsEachBox() {
        WorkArea u = WorkArea.box(new BlockPos(0, 0, 0), new BlockPos(1, 1, 1))
                .union(WorkArea.box(new BlockPos(10, 0, 10), new BlockPos(10, 0, 10)));
        assertTrue(u.contains(new BlockPos(10, 0, 10)));
        assertTrue(u.contains(new BlockPos(1, 1, 1)));
        assertFalse(u.contains(new BlockPos(5, 0, 5)));
        assertEquals(2, u.boxes().size());
    }

    @Test
    public void specRequiresInventoryAndArea() {
        try {
            WorkerSpec.builder().area(WorkArea.box(BlockPos.ORIGIN, BlockPos.ORIGIN)).build();
            fail("brez inventarja");
        } catch (IllegalStateException expected) {
        }
        try {
            WorkerSpec.builder().inventory(new ItemStackHandler(1)).build();
            fail("brez območja (D-033)");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void specDefaults() {
        ItemStackHandler inv = new ItemStackHandler(3);
        WorkArea area = WorkArea.box(BlockPos.ORIGIN, new BlockPos(2, 2, 2));
        WorkerSpec s = WorkerSpec.builder().inventory(inv).area(area).build();
        assertSame(inv, s.inventory());
        assertSame(area, s.area());
        assertNull("brez lastnika → roke [NpcBaritone] (D-044)", s.owner());
        assertSame(IWorkPermission.AREA_ONLY, s.permission());
        assertTrue(s.permission().canBreak(BlockPos.ORIGIN, null));
        assertEquals("vgrajen profil D-031", "worker", s.profile());
    }
}
