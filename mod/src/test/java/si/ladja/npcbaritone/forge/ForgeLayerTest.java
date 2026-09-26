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

import net.minecraft.entity.EntityLiving;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.NpcProfile;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.harness.BootstrapOnce;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** M2: deli forge plasti, ki se dajo preveriti brez sveta. */
public class ForgeLayerTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    @Test
    public void inputAxesMatchVanillaMovementInput() {
        assertEquals(1f, BaritoneMoveHelper.axis(true, false, false), 0);
        assertEquals(-1f, BaritoneMoveHelper.axis(false, true, false), 0);
        assertEquals(0f, BaritoneMoveHelper.axis(true, true, false), 0);
        assertEquals(0.3f, BaritoneMoveHelper.axis(true, false, true), 1e-6);
        assertEquals(0f, BaritoneMoveHelper.axis(false, false, true), 0);
    }

    /**
     * R-14: v razvojnem okolju morajo MCP imena obstajati in biti taka, kot jih pričakuje
     * {@link Attach}; SRG imena (prvo v seznamu) se preverijo v izdanem jarju (smoke).
     */
    @Test
    public void attachFieldNamesExistInDevEnvironment() throws Exception {
        for (String[] names : new String[][]{Attach.NAVIGATOR, Attach.MOVE_HELPER, Attach.JUMP_HELPER}) {
            assertTrue(names[0].startsWith("field_"));
            Field f = EntityLiving.class.getDeclaredField(names[1]);
            assertFalse(names[1] + " must not be final", Modifier.isFinal(f.getModifiers()));
        }
    }

    @Test
    public void configAppliesToProfile() {
        NpcbConfig c = new NpcbConfig(2, 64, 12, 1, NpcbConfig.SpeedMode.OWN, 45, false);
        Settings s = c.applyTo(NpcProfile.create());
        assertEquals(12, (int) s.npcSnapshotMarginChunks.value);
        assertEquals(45f, s.npcMaxTurnDegrees.value, 0);
        assertFalse("own speed disables parkour (D-010)", s.allowParkour.value);
        Settings player = NpcbConfig.defaults().applyTo(NpcProfile.create());
        assertEquals(30f, player.npcMaxTurnDegrees.value, 0);
        assertEquals(8, (int) player.npcSnapshotMarginChunks.value);
    }
}
