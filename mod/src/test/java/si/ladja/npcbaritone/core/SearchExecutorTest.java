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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** M5.1/M5.5: prednostna vrsta, zgornja meja, zapiranje in ponovni zagon, števci. */
public class SearchExecutorTest {

    @After
    public void restoreDefaults() {
        SearchExecutor.start(SearchExecutor.DEFAULT_THREADS, SearchExecutor.DEFAULT_QUEUE_LIMIT);
    }

    /** Zasede edino nit, dokler se zapah ne sprosti. */
    private static CountDownLatch blockOnlyThread() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        SearchExecutor.submit(() -> {
            started.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, 0);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        return release;
    }

    @Test
    public void closerToPlayerRunsFirstThenFifo() throws Exception {
        SearchExecutor.start(1, 16);
        CountDownLatch release = blockOnlyThread();
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(4);
        SearchExecutor.submit(() -> { order.add("far"); done.countDown(); }, 900);
        SearchExecutor.submit(() -> { order.add("near-a"); done.countDown(); }, 4);
        SearchExecutor.submit(() -> { order.add("mid"); done.countDown(); }, 100);
        SearchExecutor.submit(() -> { order.add("near-b"); done.countDown(); }, 4);
        release.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(java.util.Arrays.asList("near-a", "near-b", "mid", "far"), order);
    }

    @Test
    public void fullQueueRejectsAndCounts() throws Exception {
        SearchExecutor.start(1, 2);
        CountDownLatch release = blockOnlyThread();
        SearchStats.REJECTED.set(0);
        SearchExecutor.submit(() -> { }, 1);
        SearchExecutor.submit(() -> { }, 1);
        try {
            SearchExecutor.submit(() -> { }, 1);
            fail("tretje čakajoče iskanje mora biti zavrnjeno");
        } catch (RejectedExecutionException expected) {
            assertEquals(1, SearchStats.REJECTED.get());
        } finally {
            release.countDown();
        }
    }

    @Test
    public void stopDropsQueuedAndNextUseRestarts() throws Exception {
        SearchExecutor.start(1, 8);
        CountDownLatch release = blockOnlyThread();
        SearchExecutor.submit(() -> { }, 1);
        SearchExecutor.submit(() -> { }, 1);
        release.countDown();
        int dropped = SearchExecutor.stop();
        assertTrue("zavrženih " + dropped, dropped <= 2);
        assertEquals(0, SearchExecutor.threads());
        CountDownLatch ran = new CountDownLatch(1);
        SearchExecutor.submit(ran::countDown, 1);
        assertTrue(ran.await(5, TimeUnit.SECONDS));
        assertEquals("ponovni zagon z zadnjo nastavitvijo", 1, SearchExecutor.threads());
    }

    @Test
    public void threadsAreNamedAndCounted() throws Exception {
        SearchExecutor.start(2, 8);
        List<String> names = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(4);
        for (int i = 0; i < 4; i++) {
            SearchExecutor.submit(() -> { names.add(Thread.currentThread().getName()); done.countDown(); }, 1);
        }
        assertTrue(done.await(5, TimeUnit.SECONDS));
        for (String n : names) {
            assertTrue(n, n.startsWith("npcbaritone-search-"));
        }
        assertTrue(new java.util.HashSet<>(names).size() <= 2);
    }

    @Test
    public void samplesPercentiles() {
        SearchStats.Samples s = new SearchStats.Samples();
        for (int i = 1; i <= 100; i++) {
            s.add(i);
        }
        assertEquals(50, s.percentile(50));
        assertEquals(95, s.percentile(95));
        assertEquals(100, s.percentile(100));
        assertEquals(1, s.percentile(0));
        s.clear();
        assertEquals(0, s.percentile(95));
    }
}
