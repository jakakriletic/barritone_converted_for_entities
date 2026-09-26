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

package si.ladja.npcbaritone.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import si.ladja.npcbaritone.forge.CommonProxy;
import si.ladja.npcbaritone.forge.net.PathSyncMessage;

/**
 * M3.3: klientska stran {@code @SidedProxy}. Hrani prejete poti in jih riše
 * ({@link PathRenderer}). Brez strežnika z modom ali brez {@code /npcb debug on} ne
 * dobi nobenega sporočila in ne riše ničesar.
 */
public class ClientProxy extends CommonProxy {

    static final ClientPaths PATHS = new ClientPaths();

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(new PathRenderer(PATHS));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onPathSync(PathSyncMessage message) {
        Minecraft.getMinecraft().addScheduledTask(() -> PATHS.accept(message, Minecraft.getSystemTime()));
    }

    @Override
    public long[] renderedPaths() {
        return new long[]{PathRenderer.framesWithPaths, PathRenderer.lastFramePaths, PathRenderer.lastFrameMs};
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(PATHS::clear);
    }
}
