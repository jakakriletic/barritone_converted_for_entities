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

package si.ladja.npcbaritone.core.api.utils;

/**
 * NPC Baritone (D-015): entiteta ne ruši, ne postavlja in nima inventarja; od klientskega
 * nadzornika ostane samo doseg. Interakcije z vrati pridejo v M4.
 *
 * @author Brady
 * @since 12.14.2018
 */
public interface IPlayerController {

    double getBlockReachDistance();
}
