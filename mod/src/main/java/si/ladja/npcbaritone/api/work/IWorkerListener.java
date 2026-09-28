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

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * API 3: dogodki workerja (D-034, D-036, D-037). Vse metode so privzeto prazne; klici pridejo
 * na glavni niti strežnika. Dogodki procesov (napredek, konec, zastoj) pridejo z M12–M15.
 */
public interface IWorkerListener {

    /** D-034: inventar je poln; proces se ustavi, dokler porabnik ne naredi prostora. */
    default void onInventoryFull(EntityLiving entity) {
    }

    /** D-034: orodje iz inventarja se je zlomilo med delom. */
    default void onToolBroken(EntityLiving entity, ItemStack tool) {
    }

    default void onBlockBroken(EntityLiving entity, BlockPos pos, IBlockState state) {
    }

    default void onBlockPlaced(EntityLiving entity, BlockPos pos, IBlockState state) {
    }
}
