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

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * M5.6: števci iskanja, skupni za vse instance. Pišejo iskalne niti (trajanje, čakanje v
 * vrsti) in strežniška nit (kopija posnetka, zavrnitve); bere forge plast ({@code /npcb perf},
 * stresni tečaj). Vzorci so v obročnih medpomnilnikih z zgornjo mejo, zato poraba pomnilnika
 * ne raste z dolžino teka.
 */
public final class SearchStats {

    /** Kapaciteta vsakega vzorčnega obroča (zadnjih N vzorcev). */
    static final int CAPACITY = 1 << 16;

    public static final Samples SEARCH_NANOS = new Samples();
    public static final Samples QUEUE_WAIT_NANOS = new Samples();
    public static final Samples SNAPSHOT_NANOS = new Samples();
    /** Število chunkov v posnetku ob iskanju (za razlago µs kopije). */
    public static final Samples SNAPSHOT_CHUNKS = new Samples();

    public static final AtomicLong SUBMITTED = new AtomicLong();
    public static final AtomicLong COMPLETED = new AtomicLong();
    public static final AtomicLong REJECTED = new AtomicLong();
    public static final AtomicLong FAILED = new AtomicLong();
    /** M5.2: iskanja, ki so prevzela rezultat drugega (zaenkrat vedno 0). */
    public static final AtomicLong SHARED = new AtomicLong();

    private SearchStats() {
    }

    public static void reset() {
        SEARCH_NANOS.clear();
        QUEUE_WAIT_NANOS.clear();
        SNAPSHOT_NANOS.clear();
        SNAPSHOT_CHUNKS.clear();
        SUBMITTED.set(0);
        COMPLETED.set(0);
        REJECTED.set(0);
        FAILED.set(0);
        SHARED.set(0);
    }

    /** Obroč zadnjih {@link #CAPACITY} vzorcev; varen za več piscev. */
    public static final class Samples {
        private final long[] ring = new long[CAPACITY];
        private long count;

        public synchronized void add(long v) {
            ring[(int) (count++ % CAPACITY)] = v;
        }

        public synchronized void clear() {
            count = 0;
        }

        /** Vseh dodanih od zadnjega {@link #clear()} (lahko več kot hranjenih). */
        public synchronized long count() {
            return count;
        }

        /** Urejena kopija hranjenih vzorcev. */
        public synchronized long[] sorted() {
            int n = (int) Math.min(count, CAPACITY);
            long[] out = Arrays.copyOf(ring, n);
            Arrays.sort(out);
            return out;
        }

        /** p ∈ [0, 100]; 0 pri praznem. */
        public long percentile(double p) {
            return percentile(sorted(), p);
        }

        public static long percentile(long[] sorted, double p) {
            if (sorted.length == 0) {
                return 0;
            }
            int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
            return sorted[Math.max(0, Math.min(sorted.length - 1, idx))];
        }
    }

    /** Enovrstični povzetek (µs) za log in ukaz. */
    public static String summary() {
        long[] s = SEARCH_NANOS.sorted();
        long[] w = QUEUE_WAIT_NANOS.sorted();
        long[] c = SNAPSHOT_NANOS.sorted();
        return String.format(Locale.ROOT,
                "iskanj=%d končanih=%d zavrnjenih=%d neuspelih=%d deljenih=%d | iskanje µs p50=%d p95=%d | vrsta µs p50=%d p95=%d | posnetek µs p50=%d p95=%d (chunkov p50=%d)",
                SUBMITTED.get(), COMPLETED.get(), REJECTED.get(), FAILED.get(), SHARED.get(),
                Samples.percentile(s, 50) / 1000, Samples.percentile(s, 95) / 1000,
                Samples.percentile(w, 50) / 1000, Samples.percentile(w, 95) / 1000,
                Samples.percentile(c, 50) / 1000, Samples.percentile(c, 95) / 1000,
                SNAPSHOT_CHUNKS.percentile(50));
    }
}
