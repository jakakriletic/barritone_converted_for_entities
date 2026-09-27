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
 * Katera vrata instanca odpira (API 2, D-039). Vrata se odprejo neposredno in zaprejo za
 * entiteto; rušenja ni (D-031).
 * <ul>
 * <li>{@code NONE} — ničesar ne odpira; vrata in ograjna vrata so prehodna samo, če so že odprta
 * (CNPC {@code doorInteract = 2});</li>
 * <li>{@code WOODEN} — lesena vrata in ograjna vrata (privzeto; CNPC {@code doorInteract} 0 in 1);</li>
 * <li>{@code ALL} — tudi železna in druga ne-lesena vrata (CNPC "odpri vsa vrata").</li>
 * </ul>
 */
public enum DoorMode {
    NONE, WOODEN, ALL
}
