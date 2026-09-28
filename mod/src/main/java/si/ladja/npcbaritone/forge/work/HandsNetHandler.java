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

package si.ladja.npcbaritone.forge.work;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.util.text.ITextComponent;

/**
 * M11.1 (D-032): povezava brez omrežja za {@code FakePlayer} rok.
 *
 * <p>{@code ForgeHooks.onBlockBreakEvent} ob preklicanem {@code BreakEvent} brez preverbe
 * {@code null} kliče {@code player.connection.sendPacket(SPacketBlockChange)} (in paket
 * {@code TileEntity}); {@code PlayerInteractionManager.tryHarvestBlock} v creative prav tako.
 * Goli {@code FakePlayer} ima {@code connection == null} → NPE, ko zaščitni mod ali porabnik
 * prekliče rušenje. Konstruktor nastavi {@code player.connection = this}; paketi se zavržejo.
 */
public class HandsNetHandler extends NetHandlerPlayServer {

    public HandsNetHandler(EntityPlayerMP player) {
        super(player.getServer(), new NetworkManager(EnumPacketDirection.SERVERBOUND), player);
    }

    /** Namesti, če roke še nimajo povezave; vrne, ali je bila nameščena. */
    public static boolean install(EntityPlayerMP player) {
        if (player.connection != null) {
            return false;
        }
        new HandsNetHandler(player);
        return true;
    }

    @Override
    public void sendPacket(Packet<?> packet) {
    }

    @Override
    public void disconnect(ITextComponent reason) {
    }

    @Override
    public void update() {
    }
}
