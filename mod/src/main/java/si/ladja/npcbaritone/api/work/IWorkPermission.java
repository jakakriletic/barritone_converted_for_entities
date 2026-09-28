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
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/**
 * API 3 (D-033): porabnikova dovoljenja za rušenje in postavljanje <b>znotraj</b> delovnega
 * območja (npr. ladja_mod prepove ladijske bloke). Območje, D-031 seznam, spawn in meja sveta se
 * preverijo pred tem, zato jih tu ni treba ponavljati.
 *
 * <p><b>Pogodba:</b> samo branje lastnih nespremenljivih podatkov, varno za niti — klic pride tudi
 * z iskalne niti ({@code CalculationContext}), ne samo z glavne. Ne bere sveta in ne proži eventov.
 */
@FunctionalInterface
public interface IWorkPermission {

    /** Samo območje in privzeta varovala, brez dodatnih omejitev porabnika. */
    IWorkPermission AREA_ONLY = (pos, state) -> true;

    /** @param state stanje bloka, kot ga vidi iskanje (lahko {@code null}, če ni znano) */
    boolean canBreak(BlockPos pos, @Nullable IBlockState state);

    /** @param state blok, ki bi ga worker postavil (lahko {@code null}: katerikoli throwaway) */
    default boolean canPlace(BlockPos pos, @Nullable IBlockState state) {
        return canBreak(pos, state);
    }
}
