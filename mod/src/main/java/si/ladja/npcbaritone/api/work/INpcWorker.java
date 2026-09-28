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

import net.minecraft.entity.EntityLiving;
import si.ladja.npcbaritone.api.INpcNavigator;

/**
 * API 3 (D-030): ročaj registriranega workerja. Navigacija gre prek {@link #navigator()} (API 1–2);
 * rušenje, postavljanje in procesi pridejo z M11.3–M15. Samo strežniška nit.
 */
public interface INpcWorker {

    EntityLiving entity();

    INpcNavigator navigator();

    WorkerSpec spec();

    /** D-036: začasno prepusti entiteto AI taskom porabnika (npr. boj); proces čaka. */
    void pause();

    void resume();

    boolean paused();

    /** false po {@code NpcBaritone.release} ali odklopu entitete. */
    boolean active();

    void addListener(IWorkerListener listener);

    void removeListener(IWorkerListener listener);
}
