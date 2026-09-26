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

import org.junit.Test;
import si.ladja.npcbaritone.forge.net.PathSyncMessage;

import static org.junit.Assert.assertEquals;

/** M3.3: shramba poti na klientu (brez izrisa). */
public class ClientPathsTest {

    private static PathSyncMessage active(int id) {
        PathSyncMessage m = new PathSyncMessage();
        m.entityId = id;
        m.active = true;
        return m;
    }

    @Test
    public void inactiveForgetsAndStaleExpires() {
        ClientPaths p = new ClientPaths();
        p.accept(active(1), 0);
        p.accept(active(2), 0);
        assertEquals(2, p.live(100).size());
        p.accept(PathSyncMessage.inactive(1), 200);
        assertEquals(1, p.live(200).size());
        p.accept(active(2), 1000); // osvežitev
        assertEquals(1, p.live(1000 + ClientPaths.EXPIRE_MS).size());
        assertEquals(0, p.live(1001 + ClientPaths.EXPIRE_MS).size());
    }
}
