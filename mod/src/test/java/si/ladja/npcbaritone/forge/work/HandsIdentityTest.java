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
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * M11.1 (D-044): roke delujejo pod izpeljano identiteto, nikoli pod UUID resničnega igralca.
 * {@code EntityPlayerMP.<init>} pokliče {@code PlayerList.getPlayerAdvancements(this)}, ki objekt
 * napredkov poišče po UUID in ga s {@code setPlayer} preveže na novega igralca — z UUID lastnika bi
 * {@code FakePlayer} prevzel napredke igralca, ki je online. Napaka je tiha, zato test.
 */
public class HandsIdentityTest {

    private static final GameProfile OWNER = new GameProfile(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch");

    @Test
    public void derivedIdDiffersFromOwnerAndIsStable() {
        GameProfile a = HandsIdentity.forOwner(OWNER);
        GameProfile b = HandsIdentity.forOwner(new GameProfile(OWNER.getId(), "drugoIme"));
        assertNotEquals("UUID rok ne sme biti UUID lastnika", OWNER.getId(), a.getId());
        assertEquals("izpeljava je odvisna samo od UUID lastnika (ime se lahko spremeni)", a.getId(), b.getId());
        assertEquals(3, a.getId().version());
    }

    @Test
    public void differentOwnersGetDifferentHands() {
        GameProfile other = new GameProfile(UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6"), "jeb_");
        assertNotEquals(HandsIdentity.forOwner(OWNER).getId(), HandsIdentity.forOwner(other).getId());
    }

    @Test
    public void nameIsValidPlayerNameAndMarked() {
        String name = HandsIdentity.forOwner(new GameProfile(OWNER.getId(), "abcdefghijklmnop")).getName();
        assertTrue(name, name.length() <= 16);
        assertTrue(name, name.startsWith("[NPCB]"));
    }

    @Test
    public void noOwnerUsesLibraryIdentity() {
        GameProfile lib = HandsIdentity.forOwner(null);
        assertEquals(HandsIdentity.LIBRARY, lib);
        assertNotEquals(HandsIdentity.LIBRARY.getId(), HandsIdentity.forOwner(OWNER).getId());
    }
}
