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

package si.ladja.npcbaritone.client;

import si.ladja.npcbaritone.forge.net.PathSyncMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * M3.3: zadnje prejete poti po ID entitete. Samo glavna nit klienta (sporočila pridejo
 * prek {@code addScheduledTask}). Vnos brez osvežitve {@value #EXPIRE_MS} ms se pozabi —
 * strežnik aktivno pot ponovi vsaj na sekundo ({@code DebugSync.KEEPALIVE}).
 */
public final class ClientPaths {

    static final long EXPIRE_MS = 3000;

    private final Map<Integer, Entry> paths = new HashMap<>();

    public static final class Entry {
        public final PathSyncMessage message;
        public final long receivedMs;

        Entry(PathSyncMessage message, long receivedMs) {
            this.message = message;
            this.receivedMs = receivedMs;
        }
    }

    public void accept(PathSyncMessage message, long nowMs) {
        if (message.active) {
            paths.put(message.entityId, new Entry(message, nowMs));
        } else {
            paths.remove(message.entityId);
        }
    }

    /** Odstrani zastarele vnose in vrne preostale. */
    public Collection<Entry> live(long nowMs) {
        for (Iterator<Entry> it = paths.values().iterator(); it.hasNext(); ) {
            if (nowMs - it.next().receivedMs > EXPIRE_MS) {
                it.remove();
            }
        }
        List<Entry> out = new ArrayList<>(paths.values());
        return out;
    }

    public int size() {
        return paths.size();
    }

    public void clear() {
        paths.clear();
    }
}
