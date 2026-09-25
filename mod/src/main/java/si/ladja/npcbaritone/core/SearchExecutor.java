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

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * M5.1 (D-017): iskalni bazen s fiksnim številom niti in <b>prednostno</b> vrsto z zgornjo
 * mejo. Prednost ima iskanje entitete, ki je najbližje igralcu; pri enaki prednosti starejše
 * (FIFO). Poln bazen zavrne z {@link RejectedExecutionException} → {@code queue_full}.
 *
 * <p>Življenjski cikel (M5.5): {@link #start} ob zagonu strežnika (config), {@link #stop}
 * ob ustavitvi; po {@code stop} se ob naslednji uporabi znova zažene z zadnjo nastavitvijo
 * (enoigralski svet: izhod in ponoven vstop, JUnit).
 */
public final class SearchExecutor {

    public static final int DEFAULT_THREADS = 2;
    public static final int DEFAULT_QUEUE_LIMIT = 64;

    private static final AtomicInteger THREAD_NO = new AtomicInteger();
    private static final AtomicLong SEQ = new AtomicLong();
    private static final ThreadGroup GROUP = new ThreadGroup("npcbaritone-search");

    private static ThreadPoolExecutor pool;
    private static int queueLimit = DEFAULT_QUEUE_LIMIT;
    private static int threads = DEFAULT_THREADS;

    private SearchExecutor() {
    }

    /** Zažene (ali zamenja) bazen. Klic na strežniški niti ob zagonu strežnika. */
    public static synchronized void start(int threadCount, int limit) {
        stop();
        threads = Math.max(1, Math.min(8, threadCount));
        queueLimit = Math.max(1, limit);
        ThreadFactory factory = r -> {
            Thread t = new Thread(GROUP, r, "npcbaritone-search-" + THREAD_NO.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        };
        pool = new ThreadPoolExecutor(threads, threads, 60L, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(), factory);
    }

    /**
     * Ustavi bazen: čakajoča iskanja se zavržejo, tekoča se prekinejo (A* ima časovno omejitev
     * ≤ 1 s), počaka največ 2 s.
     *
     * @return število zavrženih čakajočih iskanj
     */
    public static synchronized int stop() {
        if (pool == null) {
            return 0;
        }
        ThreadPoolExecutor p = pool;
        pool = null;
        int dropped = p.shutdownNow().size();
        try {
            p.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return dropped;
    }

    private static synchronized ThreadPoolExecutor pool() {
        if (pool == null) {
            start(threads, queueLimit);
        }
        return pool;
    }

    /**
     * Odda iskanje.
     *
     * @param priority manjše = prej (npr. kvadrat razdalje do najbližjega igralca)
     * @throws RejectedExecutionException vrsta je polna ali bazen se ustavlja
     */
    public static void submit(Runnable search, long priority) {
        ThreadPoolExecutor p = pool();
        if (p.getQueue().size() >= queueLimit) {
            SearchStats.REJECTED.incrementAndGet();
            throw new RejectedExecutionException("npcbaritone search queue full (" + queueLimit + ")");
        }
        SearchStats.SUBMITTED.incrementAndGet();
        p.execute(new Task(search, priority, SEQ.incrementAndGet(), System.nanoTime()));
    }

    public static synchronized int threads() {
        return pool == null ? 0 : pool.getMaximumPoolSize();
    }

    public static synchronized int queued() {
        return pool == null ? 0 : pool.getQueue().size();
    }

    public static synchronized int active() {
        return pool == null ? 0 : pool.getActiveCount();
    }

    /** Vse niti, ki jih je bazen kdajkoli ustvaril (A3: nobene izven bazena). */
    public static int threadsCreated() {
        return THREAD_NO.get();
    }

    static final class Task implements Runnable, Comparable<Task> {
        final Runnable body;
        final long priority;
        final long seq;
        final long submitNanos;

        Task(Runnable body, long priority, long seq, long submitNanos) {
            this.body = body;
            this.priority = priority;
            this.seq = seq;
            this.submitNanos = submitNanos;
        }

        @Override
        public void run() {
            long t0 = System.nanoTime();
            SearchStats.QUEUE_WAIT_NANOS.add(t0 - submitNanos);
            try {
                body.run();
            } finally {
                SearchStats.SEARCH_NANOS.add(System.nanoTime() - t0);
                SearchStats.COMPLETED.incrementAndGet();
            }
        }

        @Override
        public int compareTo(Task o) {
            int c = Long.compare(priority, o.priority);
            return c != 0 ? c : Long.compare(seq, o.seq);
        }
    }
}
