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

package si.ladja.npcbaritone.forge;

import si.ladja.npcbaritone.core.api.Settings;

/**
 * D-039: prepisi ene instance nad poimenovanim profilom (CNPC U3/U4 vrata, U6 hitrost).
 * {@code null} pomeni "kot config/profil". Vrstni red: NPC privzete vrednosti → strežniški
 * config → poimenovan profil → ti prepisi ({@link Attach#settingsFor}).
 */
final class InstanceOverrides {

    /** null = strežniški {@code movement.speedMode}. */
    NpcbConfig.SpeedMode speedMode;
    /** null = iz profila ({@code npcOpenDoors}). */
    Boolean openDoors;
    /** null = iz profila ({@code npcOpenIronDoors}). */
    Boolean openIronDoors;

    NpcbConfig.SpeedMode speed(NpcbConfig config) {
        return speedMode != null ? speedMode : config.speedMode;
    }

    Settings apply(Settings s) {
        if (openDoors != null) {
            s.npcOpenDoors.value = openDoors;
        }
        if (openIronDoors != null) {
            s.npcOpenIronDoors.value = openIronDoors;
        }
        return s;
    }
}
