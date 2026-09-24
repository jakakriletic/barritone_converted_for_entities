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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * M1.11: začasen iskalni executor — ena daemon nit, omejena vrsta. Nadomešča Baritonov
 * neomejen bazen ({@code ThreadPoolExecutor(4, MAX_VALUE)}). Pravi executor z deljenjem
 * iskanj in mejami iz configa pride v M5 (D-017). Polna vrsta vrže
 * {@link java.util.concurrent.RejectedExecutionException}; klicatelj iskanje označi kot neuspelo.
 */
final class SearchExecutor {

    static final int QUEUE_LIMIT = 256;

    static final Executor INSTANCE;

    static {
        AtomicInteger n = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "npcbaritone-search-" + n.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        };
        INSTANCE = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_LIMIT), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    private SearchExecutor() {
    }
}
