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

package si.ladja.npcbaritone.core.behavior;

import si.ladja.npcbaritone.core.Baritone;

/**
 * D-015: entiteta nima inventarja. Nadomestek Baritonovega {@code InventoryBehavior}, da
 * premiki, ki bi postavljali bloke, ostanejo nespremenjeni: metnih blokov ni nikoli.
 * Profil z inventarjem je M10.
 */
public final class InventoryBehavior extends Behavior {

    public InventoryBehavior(Baritone baritone) {
        super(baritone);
    }

    public boolean hasGenericThrowaway() {
        return false;
    }

    public boolean selectThrowawayForLocation(boolean select, int x, int y, int z) {
        return false;
    }
}
