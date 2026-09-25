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

package si.ladja.npcbaritone.api;

import net.minecraft.entity.EntityLiving;

/**
 * Povratni klici navigacije (API 1). Kličejo se na strežniški niti, enkrat na prehod stanja,
 * med tickom entitete — ne spreminjaj navigatorja rekurzivno iz povratnega klica drugega NPC-ja.
 */
public interface NavListener {

    /** Noge so v cilju. */
    default void onArrived(EntityLiving entity) {
    }

    /**
     * Cilj ni dosegljiv.
     *
     * @param reason {@code no_path}, {@code queue_full}, {@code exception}, {@code unloaded_goal}
     */
    default void onFailed(EntityLiving entity, String reason) {
    }
}
