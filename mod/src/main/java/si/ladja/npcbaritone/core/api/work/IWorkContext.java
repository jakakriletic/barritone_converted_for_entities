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

package si.ladja.npcbaritone.core.api.work;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * M11.5 (D-031, D-033): kar iskanje poti ve o workerju. Posnetek nastane na glavni niti ob gradnji
 * {@code CalculationContext}; metode {@link #mayBreak}/{@link #mayPlace} se kličejo z iskalne niti
 * in berejo samo nespremenljive podatke. Entiteta brez posnetka (ne-worker) ne ruši in ne postavlja
 * ne glede na profil (D-030).
 */
public interface IWorkContext {

    /** Območje, D-031 seznam, dovoljenja porabnika, spawn protection. */
    boolean mayBreak(int x, int y, int z, IBlockState state);

    boolean mayPlace(int x, int y, int z);

    /** Kopije orodij iz inventarja (za {@code ToolSet}); nespremenljiv seznam. */
    List<ItemStack> tools();

    boolean hasThrowaway();
}
