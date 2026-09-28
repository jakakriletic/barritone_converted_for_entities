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

package si.ladja.npcbaritone.forge.work;

import com.mojang.authlib.GameProfile;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * M11.1 (D-044): identiteta {@code FakePlayer}-ja, prek katerega worker ruši in postavlja (D-032).
 *
 * <p>Nikoli UUID resničnega igralca: {@code EntityPlayerMP.<init>} pokliče
 * {@code PlayerList.getPlayerAdvancements(this)}, ta objekt napredkov poišče po UUID in ga s
 * {@code setPlayer(this)} preveže na novo entiteto; enako {@code getPlayerStatsFile}. Z UUID
 * lastnika bi roke prevzele napredke igralca, ki je online ({@code flushDirty} bi nato pošiljal
 * pakete prek povezave rok). Zato UUID različice 3 iz {@code "NpcBaritone:" + UUID lastnika}:
 * stalen za lastnika (preživi preimenovanje), različen od vsakega pravega računa (ti so v4).
 */
public final class HandsIdentity {

    /** Roke brez lastnika (porabnik ga ne poda). */
    public static final GameProfile LIBRARY = new GameProfile(
            UUID.nameUUIDFromBytes("NpcBaritone:library".getBytes(StandardCharsets.UTF_8)), "[NpcBaritone]");

    private static final String PREFIX = "[NPCB]";
    private static final int MAX_NAME = 16;

    private HandsIdentity() {
    }

    public static GameProfile forOwner(@Nullable GameProfile owner) {
        if (owner == null || owner.getId() == null) {
            return LIBRARY;
        }
        UUID id = UUID.nameUUIDFromBytes(("NpcBaritone:" + owner.getId()).getBytes(StandardCharsets.UTF_8));
        String n = owner.getName() == null ? "" : owner.getName();
        String name = PREFIX + n;
        return new GameProfile(id, name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name);
    }
}
