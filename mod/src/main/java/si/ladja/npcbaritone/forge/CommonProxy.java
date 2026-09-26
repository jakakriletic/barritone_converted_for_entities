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

import si.ladja.npcbaritone.forge.net.PathSyncMessage;

/**
 * Strežniška (in skupna) stran {@code @SidedProxy}. Dedicated strežnik sporočil poti ne
 * dobiva; klient ima {@code si.ladja.npcbaritone.client.ClientProxy} (M3.3), ki ga forge
 * paket nikoli ne uvozi neposredno (D-024).
 */
public class CommonProxy {

    public void preInit() {
    }

    /** Klic iz omrežne niti; privzeto nič. */
    public void onPathSync(PathSyncMessage message) {
    }

    /**
     * Selftest (M3 A1): {sličic z vsaj eno potjo, poti v zadnji sličici, čas zadnje sličice ms}
     * iz izrisa v istem procesu; {@code null} brez klienta (dedicated strežnik).
     */
    public long[] renderedPaths() {
        return null;
    }
}
