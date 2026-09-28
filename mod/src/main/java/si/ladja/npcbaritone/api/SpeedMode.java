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
 * Način hitrosti ene instance (API 2, D-010, D-039). {@code PLAYER} — med vodenjem igralčeva
 * osnovna hitrost, sprint in parkour (Baritonove cene veljajo); {@code OWN} — entiteta ohrani
 * svojo osnovno hitrost ({@code MOVEMENT_SPEED}), parkour je izklopljen.
 */
public enum SpeedMode {
    PLAYER, OWN
}
