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

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stopnja A (D-027): razčlenitev časa glavne niti po sestavnih delih — vsota ns in klicev po
 * razredu in "reži" (dogodek poslušalca ali odsek navigatorja). Samo strežniška nit; vklop z
 * {@code NPCB_PROFILE=1} ({@code t4-run.ps1 -Profile}). Izklopljen stane en branje polja na klic.
 */
public final class PerfProfile {

    /** Reže za poslušalce {@code GameEventHandler}. */
    public static final int ON_TICK = 0;
    public static final int UPDATE_PRE = 1;
    public static final int UPDATE_POST = 2;
    public static final int POST_TICK = 3;
    static final int SLOTS = 6;
    private static final String[] DEFAULT_LABELS = {"onTick", "onPlayerUpdate PRE", "onPlayerUpdate POST", "onPostTick", "#4", "#5"};

    public static volatile boolean enabled = "1".equals(System.getenv("NPCB_PROFILE"));

    private static final Map<Class<?>, long[]> TOTALS = new IdentityHashMap<>();
    private static final Map<Class<?>, String[]> LABELS = new IdentityHashMap<>();

    private PerfProfile() {
    }

    /** @return začetni čas ali 0, če profil ni vklopljen */
    public static long start() {
        return enabled ? System.nanoTime() : 0L;
    }

    public static void add(Class<?> owner, int slot, long t0) {
        if (t0 == 0L) {
            return;
        }
        long d = System.nanoTime() - t0;
        long[] a = TOTALS.get(owner);
        if (a == null) {
            a = new long[SLOTS * 2];
            TOTALS.put(owner, a);
        }
        a[slot * 2] += d;
        a[slot * 2 + 1]++;
    }

    /** Imena rež za razred, ki ne uporablja dogodkov poslušalca (npr. odseki navigatorja). */
    public static void label(Class<?> owner, String... names) {
        LABELS.put(owner, names);
    }

    public static void reset() {
        TOTALS.clear();
    }

    /**
     * @param ticks  strežniških tickov meritve
     * @param mainNs skupni ns knjižnice na glavni niti v istem času (za delež), ali 0
     * @return vrstice CSV {@code component,us_per_tick,share,calls_per_tick,ns_per_call}, padajoče po času
     */
    public static List<String> report(long ticks, long mainNs) {
        List<Object[]> rows = new ArrayList<>();
        for (Map.Entry<Class<?>, long[]> e : TOTALS.entrySet()) {
            String[] labels = LABELS.getOrDefault(e.getKey(), DEFAULT_LABELS);
            long[] a = e.getValue();
            for (int s = 0; s < SLOTS; s++) {
                if (a[s * 2 + 1] > 0) {
                    String label = s < labels.length ? labels[s] : "#" + s;
                    rows.add(new Object[]{e.getKey().getSimpleName() + "." + label, a[s * 2], a[s * 2 + 1]});
                }
            }
        }
        rows.sort((x, y) -> Long.compare((Long) y[1], (Long) x[1]));
        List<String> out = new ArrayList<>();
        out.add("component,us_per_tick,share_of_main,calls_per_tick,ns_per_call");
        long t = Math.max(1, ticks);
        for (Object[] r : rows) {
            long ns = (Long) r[1];
            long calls = (Long) r[2];
            out.add(String.format(Locale.ROOT, "%s,%.1f,%.3f,%.1f,%d", r[0], ns / 1000.0 / t,
                    mainNs > 0 ? ns / (double) mainNs : 0.0, calls / (double) t, ns / Math.max(1, calls)));
        }
        return out;
    }
}
