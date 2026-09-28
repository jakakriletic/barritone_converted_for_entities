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

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;

/**
 * M11.3/M11.4 (D-034): kar roke rabijo od inventarja. Predmet se vzame za <b>eno dejanje</b> in se
 * v istem ticku vrne ({@code finally}); roke med ticki ne hranijo ničesar.
 */
public interface IHandsInventory {

    /** Najboljše orodje za blok (odstranjeno iz inventarja) ali {@code EMPTY} = roka. */
    ItemStack takeTool(IBlockState state);

    /** En metni blok (D-031, {@code acceptableThrowawayItems}) ali {@code EMPTY}. */
    ItemStack takeThrowaway();

    boolean hasThrowaway();

    /**
     * Vrne predmet ali drop. Kar ne gre v inventar, gre {@code spill} (v svet kot {@code EntityItem}).
     *
     * @return true, če je šlo vse v inventar
     */
    boolean putBack(ItemStack stack, Consumer<ItemStack> spill);
}
