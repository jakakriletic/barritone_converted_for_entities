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

package si.ladja.npcbaritone.forge.work;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.api.work.WorkArea;
import si.ladja.npcbaritone.api.work.WorkerSpec;
import si.ladja.npcbaritone.harness.BootstrapOnce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * M11.3/M11.5 (D-031, D-033): preverba <b>ob izvedbi</b> — zadnja ovira, preden roke porušijo ali
 * postavijo. Napaka tu je tiha (M11 A2: spremembe izven območja ali v zaščitenih blokih).
 */
public class WorkGuardsTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    private static final WorkerSpec SPEC = WorkerSpec.builder()
            .inventory(new ItemStackHandler(1))
            .area(WorkArea.box(new BlockPos(0, 60, 0), new BlockPos(9, 69, 9)))
            .permission((pos, state) -> pos.getX() != 5) // porabnik prepove stolpec x = 5
            .build();

    @Test
    public void breakInsideAreaAllowed() {
        assertNull(WorkGuards.refuseBreak(SPEC, null, new BlockPos(1, 60, 1), Blocks.STONE.getDefaultState()));
        assertNull(WorkGuards.refuseBreak(SPEC, null, new BlockPos(9, 69, 9), Blocks.DIRT.getDefaultState()));
    }

    @Test
    public void breakOutsideAreaRefused() {
        assertEquals(WorkGuards.OUTSIDE_AREA, WorkGuards.refuseBreak(SPEC, null, new BlockPos(10, 60, 1), Blocks.STONE.getDefaultState()));
        assertEquals(WorkGuards.OUTSIDE_AREA, WorkGuards.refuseBreak(SPEC, null, new BlockPos(1, 59, 1), Blocks.STONE.getDefaultState()));
    }

    @Test
    public void consumerPermissionRefuses() {
        assertEquals(WorkGuards.PERMISSION, WorkGuards.refuseBreak(SPEC, null, new BlockPos(5, 60, 1), Blocks.STONE.getDefaultState()));
        assertEquals(WorkGuards.PERMISSION, WorkGuards.refusePlace(SPEC, new BlockPos(5, 61, 1)));
    }

    @Test
    public void d031ListRefusedEvenInsideArea() {
        BlockPos p = new BlockPos(2, 61, 2);
        assertEquals("skrinja (TileEntity)", WorkGuards.PROTECTED, WorkGuards.refuseBreak(SPEC, null, p, Blocks.CHEST.getDefaultState()));
        assertEquals("peč (TileEntity)", WorkGuards.PROTECTED, WorkGuards.refuseBreak(SPEC, null, p, Blocks.FURNACE.getDefaultState()));
        assertEquals("spawner", WorkGuards.PROTECTED, WorkGuards.refuseBreak(SPEC, null, p, Blocks.MOB_SPAWNER.getDefaultState()));
        assertEquals("postelja", WorkGuards.PROTECTED, WorkGuards.refuseBreak(SPEC, null, p, Blocks.BED.getDefaultState()));
        assertEquals("vrata", WorkGuards.PROTECTED, WorkGuards.refuseBreak(SPEC, null, p, Blocks.OAK_DOOR.getDefaultState()));
        assertEquals("železna vrata", WorkGuards.PROTECTED, WorkGuards.refuseBreak(SPEC, null, p, Blocks.IRON_DOOR.getDefaultState()));
        assertEquals("bedrock", WorkGuards.UNBREAKABLE, WorkGuards.refuseBreak(SPEC, null, p, Blocks.BEDROCK.getDefaultState()));
        assertEquals("portal", WorkGuards.UNBREAKABLE, WorkGuards.refuseBreak(SPEC, null, p, Blocks.PORTAL.getDefaultState()));
        assertEquals("voda", WorkGuards.NOT_SOLID, WorkGuards.refuseBreak(SPEC, null, p, Blocks.WATER.getDefaultState()));
        assertEquals("zrak", WorkGuards.NOT_SOLID, WorkGuards.refuseBreak(SPEC, null, p, Blocks.AIR.getDefaultState()));
    }

    @Test
    public void placeOutsideAreaRefused() {
        assertNull(WorkGuards.refusePlace(SPEC, new BlockPos(0, 60, 0)));
        assertEquals(WorkGuards.OUTSIDE_AREA, WorkGuards.refusePlace(SPEC, new BlockPos(0, 70, 0)));
        assertEquals(WorkGuards.OUTSIDE_AREA, WorkGuards.refusePlace(SPEC, new BlockPos(-1, 60, 0)));
    }
}
