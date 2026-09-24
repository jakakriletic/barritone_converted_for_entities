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

package si.ladja.npcbaritone.harness;

import net.minecraft.init.Bootstrap;
import org.junit.rules.ExternalResource;

/**
 * Enkratna registracija blokov, predmetov in biomov v JVM brez zagona igre (D-022).
 *
 * <p>Uporaba: {@code @ClassRule public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();}
 * ali neposredno {@link #ensure()}. Izmerjeno v raziskavi: ~2,2 s ob prvem klicu.
 */
public final class BootstrapOnce extends ExternalResource {

    private static volatile long bootstrapMillis = -1;

    public static synchronized void ensure() {
        if (!Bootstrap.isRegistered()) {
            long t0 = System.nanoTime();
            Bootstrap.register();
            bootstrapMillis = (System.nanoTime() - t0) / 1_000_000L;
        }
        if (Bootstrap.hasErrored) {
            throw new IllegalStateException("Bootstrap.register() reported errors");
        }
    }

    /** Trajanje prve registracije v tem JVM ali -1, če je ni izvedel ta razred. */
    public static long bootstrapMillis() {
        return bootstrapMillis;
    }

    @Override
    protected void before() {
        ensure();
    }
}
