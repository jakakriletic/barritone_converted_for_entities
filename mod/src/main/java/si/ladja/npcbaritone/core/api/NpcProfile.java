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

package si.ladja.npcbaritone.core.api;

/**
 * Privzet NPC profil (RAZISKAVA §8, D-015, D-016, D-017). Spremeni samo vrednosti, ki se
 * za NPC-je razlikujejo od Baritona; vse ostalo ostane Baritonovo.
 */
public final class NpcProfile {

    private NpcProfile() {
    }

    /**
     * Nov, neodvisen nabor nastavitev z NPC privzetimi vrednostmi (profil instance, D-016).
     */
    public static Settings create() {
        return applyDefaults(new Settings());
    }

    public static Settings applyDefaults(Settings s) {
        // D-015: brez rušenja, postavljanja in inventarja
        s.allowBreak.value = false;
        s.allowPlace.value = false;
        s.allowInventory.value = false;
        s.allowWaterBucketFall.value = false;
        s.allowParkour.value = false;
        s.allowSprint.value = true;
        s.maxFallHeightNoWater.value = 3;
        // D-017: začetne časovne omejitve iskanja (umerijo se v M5)
        s.primaryTimeoutMS.value = 150L;
        s.failureTimeoutMS.value = 500L;
        s.planAheadPrimaryTimeoutMS.value = 300L;
        s.planAheadFailureTimeoutMS.value = 800L;
        // D-014: brez predpomnilnika regij; strežnik nima izrisa
        s.chunkCaching.value = false;
        s.renderPath.value = false;
        // D-011: entiteta se dejansko obrne (ni "prostega pogleda" kot pri igralcu)
        s.freeLook.value = false;
        s.antiCheatCompatibility.value = false;
        s.disconnectOnArrival.value = false;
        return s;
    }
}
