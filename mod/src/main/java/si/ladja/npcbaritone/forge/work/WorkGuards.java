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

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import si.ladja.npcbaritone.api.work.WorkerSpec;

import javax.annotation.Nullable;

/**
 * M11.3 (D-031, D-033): preverba <b>ob izvedbi</b>, tik preden roke porušijo ali postavijo. Ista
 * pravila pri iskanju poti doda M11.5 ({@code CalculationContext}); tu so zadnja ovira, ker se je
 * svet med iskanjem in izvedbo lahko spremenil. Spawn protection, meja sveta in eventi
 * ({@code LeftClickBlock}, {@code BreakEvent}, {@code PlaceEvent}) pridejo v {@link EntityHands}.
 *
 * @return koda razloga ali {@code null}, če je dovoljeno
 */
public final class WorkGuards {

    public static final String OUTSIDE_AREA = "outside_area";
    public static final String NOT_SOLID = "not_solid";
    public static final String UNBREAKABLE = "unbreakable";
    /** D-031: {@code TileEntity}, postelje, vrata (tudi železna). */
    public static final String PROTECTED = "protected_block";
    public static final String PERMISSION = "permission";
    public static final String WORLD = "world_protected";
    public static final String EVENT = "event_canceled";

    private WorkGuards() {
    }

    @Nullable
    public static String refuseBreak(WorkerSpec spec, @Nullable World world, BlockPos pos, IBlockState state) {
        if (!spec.area().contains(pos)) {
            return OUTSIDE_AREA;
        }
        Material m = state.getMaterial();
        if (m == Material.AIR || m.isLiquid()) {
            return NOT_SOLID;
        }
        if (state.getBlockHardness(world, pos) < 0) {
            return UNBREAKABLE; // bedrock, portali, barrier, command block
        }
        if (isProtected(state)) {
            return PROTECTED;
        }
        if (!spec.permission().canBreak(pos, state)) {
            return PERMISSION;
        }
        return null;
    }

    @Nullable
    public static String refusePlace(WorkerSpec spec, BlockPos pos) {
        if (!spec.area().contains(pos)) {
            return OUTSIDE_AREA;
        }
        if (!spec.permission().canPlace(pos, null)) {
            return PERMISSION;
        }
        return null;
    }

    /** D-031: privzeti NPC {@code blocksToDisallowBreaking}; porabnik ga razširi prek {@code IWorkPermission}. */
    public static boolean isProtected(IBlockState state) {
        Block b = state.getBlock();
        return b.hasTileEntity(state) || b instanceof BlockBed || b instanceof BlockDoor;
    }
}
