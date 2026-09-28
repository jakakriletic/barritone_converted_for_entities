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

package si.ladja.npcbaritone.core;

import org.junit.BeforeClass;
import org.junit.Test;
import si.ladja.npcbaritone.harness.BootstrapOnce;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.pathing.path.PathExecutor;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** M7.10 (korak 3): odločitev, kdaj entiteta v gneči počaka namesto da rine. */
public class CrowdYieldTest {

    /** Settings ob konstrukciji bere Blocks; brez Bootstrapa se Blocks pokvari za ves JVM (Gradle). */
    @BeforeClass
    public static void bootstrap() {
        BootstrapOnce.ensure();
    }

    private static boolean yield(double dirX, double dirZ, double relX, double relZ, boolean moving) throws Exception {
        return yield(dirX, dirZ, relX, relZ, moving, false, 10, 20);
    }

    private static boolean yield(double dirX, double dirZ, double relX, double relZ, boolean moving, boolean navigating,
                                 int self, int other) throws Exception {
        Method m = PathExecutor.class.getDeclaredMethod("shouldYield", double.class, double.class, double.class,
                double.class, boolean.class, boolean.class, int.class, int.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(null, dirX, dirZ, relX, relZ, moving, navigating, self, other);
    }

    @Test
    public void offByDefault() {
        Settings s = si.ladja.npcbaritone.core.api.NpcProfile.create();
        assertFalse(s.npcCrowdYield.value);
        assertEquals(40, (int) s.npcCrowdMaxWaitTicks.value);
    }

    @Test
    public void waitsBehindMovingEntityAhead() throws Exception {
        // premik proti +z, druga entiteta 0,6 bloka pred nami in se premika (vrsta skozi vrata)
        assertTrue(yield(0, 1, 0.1, 0.6, true));
    }

    @Test
    public void doesNotWaitForEntityBehindOrBeside() throws Exception {
        assertFalse(yield(0, 1, 0.0, -0.6, true));  // za nami
        assertFalse(yield(0, 1, 0.6, 0.05, true));  // ob strani
    }

    @Test
    public void doesNotWaitForStandingEntityWithoutPath() throws Exception {
        // prispela ali neaktivna entiteta se ne čaka, ne glede na ID
        assertFalse(yield(0, 1, 0, 0.6, false, false, 20, 10));
        assertFalse(yield(0, -1, 0, -0.6, false, false, 10, 20));
    }

    @Test
    public void standingQueueMemberOnlyHigherIdWaits() throws Exception {
        // obe stojita in imata pot (vrsta v vratih): počaka samo tista z večjim ID-jem
        assertTrue(yield(0, 1, 0, 0.6, false, true, 20, 10));
        assertFalse(yield(0, -1, 0, -0.6, false, true, 10, 20));
    }
}
