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

import si.ladja.npcbaritone.api.work.INpcWorker;
import si.ladja.npcbaritone.api.work.WorkerSpec;

import javax.annotation.Nullable;

/** Izvedba API (mod {@code npcbaritone}); porabnik jo dobi prek {@link NpcBaritone}. */
public interface INpcBaritoneProvider {

    @Nullable
    INpcNavigator attach(EntityLiving entity, String profile);

    @Nullable
    INpcNavigator get(EntityLiving entity);

    boolean detach(EntityLiving entity);

    /** D-019: ali Baritone to entiteto sploh vodi (širina ≤ 1, višina ≤ 2 do M8). */
    boolean supports(EntityLiving entity);

    /** API 3 (D-030). */
    @Nullable
    default INpcWorker worker(EntityLiving entity, WorkerSpec spec) {
        return null;
    }

    @Nullable
    default INpcWorker getWorker(EntityLiving entity) {
        return null;
    }

    default boolean release(EntityLiving entity) {
        return false;
    }
}
