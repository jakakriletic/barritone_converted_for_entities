/*
 * This file is part of Baritone.
 * Modified for NPC Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package si.ladja.npcbaritone.core.api;

import net.minecraft.entity.EntityLiving;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Register instanc po entiteti (šibke reference, M1.10). Na strežniku ni "primarnega"
 * Baritona; vsaka entiteta z Baritonom ima svojo instanco.
 *
 * @author Leijurv
 */
public interface IBaritoneProvider {

    /**
     * @return Vse žive instance (posnetek seznama).
     */
    List<IBaritone> getAllBaritones();

    /**
     * @return Instanca za entiteto ali null, če je nima.
     */
    @Nullable
    IBaritone getBaritone(EntityLiving entity);

    /**
     * Vrne obstoječo ali ustvari novo instanco za entiteto. Klicati na strežniški niti.
     */
    IBaritone createBaritone(EntityLiving entity);

    /**
     * Odstrani instanco; nadaljnje uporabe niso definirane.
     *
     * @return true, če je bila instanca v registru
     */
    boolean destroyBaritone(IBaritone baritone);
}
