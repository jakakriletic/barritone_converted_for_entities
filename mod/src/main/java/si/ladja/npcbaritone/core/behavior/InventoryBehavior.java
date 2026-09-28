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
 * D-015: navigacijska entiteta nima inventarja — metnih blokov ni nikoli. Nadomestek
 * Baritonovega {@code InventoryBehavior}, da premiki, ki postavljajo bloke, ostanejo
 * nespremenjeni. M11.5 (D-034): worker jih ima, če jih ima porabnikov inventar.
 */
public final class InventoryBehavior extends Behavior {

    public InventoryBehavior(Baritone baritone) {
        super(baritone);
    }

    /** M11.5: samo worker z metnim blokom v inventarju in {@code allowPlace} (D-031, D-034). */
    public boolean hasGenericThrowaway() {
        if (!baritone.getSettings().allowPlace.value) {
            return false;
        }
        si.ladja.npcbaritone.core.api.work.IWorkContext w = baritone.workContext();
        return w != null && w.hasThrowaway();
    }

    /**
     * Pri igralcu izbere blok v hotbaru; pri workerju ga roke vzamejo iz inventarja šele ob kliku
     * (D-034), zato je tu samo preverba, ali ga imajo.
     */
    public boolean selectThrowawayForLocation(boolean select, int x, int y, int z) {
        return hasGenericThrowaway();
    }
}
