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

import org.junit.After;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Stopnja A: razčlenitev glavne niti — vrstni red, deleži, imena rež, izklop. */
public class PerfProfileTest {

    static final class Slow {
    }

    static final class Fast {
    }

    @After
    public void off() {
        PerfProfile.enabled = false;
        PerfProfile.reset();
    }

    private static void spin(long nanos) {
        long end = System.nanoTime() + nanos;
        while (System.nanoTime() < end) {
            // aktivno čakanje (Java 8 nima Thread.onSpinWait)
        }
    }

    @Test
    public void reportIsSortedWithLabelsAndShares() {
        PerfProfile.enabled = true;
        PerfProfile.reset();
        PerfProfile.label(Fast.class, "fast");
        for (int i = 0; i < 10; i++) {
            long t = PerfProfile.start();
            spin(2_000_000);
            PerfProfile.add(Slow.class, PerfProfile.UPDATE_PRE, t);
            t = PerfProfile.start();
            spin(200_000);
            PerfProfile.add(Fast.class, 0, t);
        }
        List<String> r = PerfProfile.report(10, 25_000_000L);
        assertEquals("component,us_per_tick,share_of_main,calls_per_tick,ns_per_call", r.get(0));
        assertEquals(3, r.size());
        assertTrue(r.get(1), r.get(1).startsWith("Slow.onPlayerUpdate PRE,"));
        assertTrue(r.get(2), r.get(2).startsWith("Fast.fast,"));
        String[] slow = r.get(1).split(",");
        assertTrue(r.get(1), Double.parseDouble(slow[1]) >= 2000);   // µs na tick
        assertEquals(1.0, Double.parseDouble(slow[3]), 1e-9);        // en klic na tick
        assertTrue(r.get(1), Double.parseDouble(slow[2]) > 0.7);     // delež od 25 ms
    }

    @Test
    public void disabledRecordsNothing() {
        PerfProfile.enabled = false;
        PerfProfile.reset();
        long t = PerfProfile.start();
        assertEquals(0L, t);
        PerfProfile.add(Slow.class, 0, t);
        assertEquals(1, PerfProfile.report(1, 0).size());
    }
}
