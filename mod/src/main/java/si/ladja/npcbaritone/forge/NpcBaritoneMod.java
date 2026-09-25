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

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import si.ladja.npcbaritone.core.api.BaritoneAPI;

import java.util.Map;

/**
 * Vstopna točka moda: config, skupni NPC profil, telemetrija, ukaz {@code /npcb} (M2),
 * sled in debug prikaz poti (M3).
 *
 * <p>D-024: vse delovanje je na strežniku; {@link #acceptRemote} sprejme vsako drugo
 * stran, zato se vanilla klient poveže na strežnik z modom in obratno. Mod ne
 * registrira entitet, blokov ali predmetov (D-025), zato ni registra za sinhronizacijo.
 */
@Mod(modid = NpcBaritoneMod.MODID, name = NpcBaritoneMod.NAME, version = NpcBaritoneMod.VERSION)
public final class NpcBaritoneMod {

    public static final String MODID = "npcbaritone";
    @Mod.Instance(MODID)
    public static NpcBaritoneMod INSTANCE;
    public static final String NAME = "NPC Baritone";
    public static final String VERSION = "@VERSION@";
    /** Izvor jedra (D-001, D-026). */
    public static final String UPSTREAM = "cabaletta/baritone v1.2.19 (d9cb2d9)";

    public static final Logger LOG = LogManager.getLogger(MODID);

    /** M3.3: klient riše poti; forge paket klientskega razreda ne uvozi (D-024). */
    @SidedProxy(clientSide = "si.ladja.npcbaritone.client.ClientProxy", serverSide = "si.ladja.npcbaritone.forge.CommonProxy")
    public static CommonProxy proxy = new CommonProxy();

    private static NpcbConfig config = NpcbConfig.defaults();

    public static NpcbConfig config() {
        return config;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());
        config = NpcbConfig.load(cfg);
        config.applyTo(BaritoneAPI.getSettings()); // skupni NPC profil (D-016)
        // Vozovnice so samo za tekoči speedtest; po ponovnem zagonu ne ohranimo nobene.
        ForgeChunkManager.setForcedChunkLoadingCallback(this, (tickets, world) ->
                tickets.forEach(ForgeChunkManager::releaseTicket));
        MinecraftForge.EVENT_BUS.register(Telemetry.INSTANCE);
        MinecraftForge.EVENT_BUS.register(CourseRunner.INSTANCE);
        MinecraftForge.EVENT_BUS.register(PathTrace.INSTANCE);
        MinecraftForge.EVENT_BUS.register(DebugSync.INSTANCE);
        DebugSync.INSTANCE.register();
        proxy.preInit();
        LOG.info("{} {} loaded (side={}, upstream={}); {}", NAME, VERSION, event.getSide(), UPSTREAM, config);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new NpcbCommand());
        LOG.info("{} ready on server (dedicated={})", MODID, event.getServer().isDedicatedServer());
    }

    /**
     * D-024: povezava se sprejme ne glede na to, ali ima druga stran mod.
     * Vzorec {@code XaeroFleetCommandMod.acceptRemote} iz ladja_mod.
     */
    @NetworkCheckHandler
    public boolean acceptRemote(Map<String, String> remoteMods, Side remoteSide) {
        return true;
    }
}
