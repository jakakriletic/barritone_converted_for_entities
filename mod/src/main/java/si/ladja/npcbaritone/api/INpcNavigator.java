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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;

/**
 * Baritonov navigator enega NPC-ja (API 1). Vanilla AI taski entitete delujejo naprej prek
 * običajnega {@code getNavigator()}; ta vmesnik je za porabnike, ki hočejo cilje neposredno
 * (skripte, ukazi, "pojdi do točke") in povratne klice. Samo strežniška nit.
 */
public interface INpcNavigator {

    EntityLiving entity();

    /** Pojdi v blok (noge v {@code pos}). @return false, če cilj ni naložen */
    boolean goTo(BlockPos pos);

    /** Poljuben Baritonov cilj ({@code GoalNear}, {@code GoalXZ}, {@code GoalComposite} …). */
    boolean goTo(Goal goal);

    /**
     * Sledi entiteti na razdalji do {@code range} blokov; cilj se posodobi, ko se tarča
     * premakne. Konča se z {@link #stop()} ali novim ciljem.
     */
    boolean follow(Entity target, int range);

    /** Ustavi takoj (brez pavze, ki jo imajo vanilla taski). */
    void stop();

    NavState state();

    /** Koda razloga pri {@link NavState#FAILED}; prazno sicer. */
    String failReason();

    /** Ime profila nastavitev (D-016). */
    String profile();

    /** @return false pri neznanem profilu */
    boolean setProfile(String profile);

    void addListener(NavListener listener);

    void removeListener(NavListener listener);
}
