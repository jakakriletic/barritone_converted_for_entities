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

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import si.ladja.npcbaritone.core.SearchExecutor;
import si.ladja.npcbaritone.core.SearchStats;

import java.util.Locale;

/**
 * M5.6: čas <b>glavne niti</b>, ki ga porabi knjižnica — {@code baritone.tick()} (vključno s
 * kopijo posnetka in izvajalcem poti), {@code EntityInteractions} in {@code BaritoneMoveHelper}
 * — seštet čez vse NPC-je v enem strežniškem ticku. To je merilo M5 A1 (D-027); ne vsebuje
 * vanilla posodobitve entitete (fizika, trki), ki bi jo mob imel tudi brez Baritona.
 * Poleg tega vzorči celoten MSPT strežnika za kontekst.
 *
 * <p>Samo strežniška nit; {@link #add} kličejo kavlji v navigatorju in move helperju.
 */
public final class PerfMeter {

    public static final PerfMeter INSTANCE = new PerfMeter();

    /** ns knjižnice na strežniški tick (vsota čez NPC-je). */
    public final SearchStats.Samples mainNanosPerTick = new SearchStats.Samples();
    /** ns celotnega strežniškega ticka (vanilla {@code tickTimeArray}). */
    public final SearchStats.Samples serverTickNanos = new SearchStats.Samples();

    private long accumulated;
    private long ticks;
    private long maxNanos;

    private PerfMeter() {
    }

    void add(long nanos) {
        accumulated += nanos;
    }

    public void reset() {
        mainNanosPerTick.clear();
        serverTickNanos.clear();
        accumulated = 0;
        ticks = 0;
        maxNanos = 0;
    }

    public long ticks() {
        return ticks;
    }

    public long maxNanos() {
        return maxNanos;
    }

    /** Zadnji po vseh ostalih poslušalcih: zapre tick. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        mainNanosPerTick.add(accumulated);
        maxNanos = Math.max(maxNanos, accumulated);
        accumulated = 0;
        ticks++;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null && server.getTickCounter() > 0) {
            // tekoči tick se zapiše šele po END; vzamemo prejšnjega
            serverTickNanos.add(server.tickTimeArray[(server.getTickCounter() - 1) % 100]);
        }
    }

    public String summary() {
        long[] m = mainNanosPerTick.sorted();
        long[] t = serverTickNanos.sorted();
        return String.format(Locale.ROOT,
                "glavna nit µs/tick p50=%d p95=%d p99=%d max=%d (tickov %d) | MSPT p50=%.1f p95=%.1f | bazen niti=%d čaka=%d dela=%d ustvarjenih=%d",
                SearchStats.Samples.percentile(m, 50) / 1000, SearchStats.Samples.percentile(m, 95) / 1000,
                SearchStats.Samples.percentile(m, 99) / 1000, maxNanos / 1000, ticks,
                SearchStats.Samples.percentile(t, 50) / 1e6, SearchStats.Samples.percentile(t, 95) / 1e6,
                SearchExecutor.threads(), SearchExecutor.queued(), SearchExecutor.active(), SearchExecutor.threadsCreated());
    }
}
