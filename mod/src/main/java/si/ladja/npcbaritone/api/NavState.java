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

/**
 * Stanje navigacije NPC-ja (API 1). {@code IDLE} — brez cilja; {@code SEARCHING} — iskanje
 * poti teče v ozadju; {@code MOVING} — hodi po poti; {@code ARRIVED} — noge v cilju;
 * {@code FAILED} — pot ne obstaja ali je bila zavrnjena ({@link INpcNavigator#failReason()}).
 */
public enum NavState {
    IDLE, SEARCHING, MOVING, ARRIVED, FAILED
}
