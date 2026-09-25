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

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.utils.interfaces.IGoalRenderPos;
import si.ladja.npcbaritone.core.pathing.path.PathExecutor;
import si.ladja.npcbaritone.forge.net.PathSyncMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * M3.2: pošiljanje poti za debug prikaz. Prejemniki so igralci, ki
 * <ol>
 *     <li>imajo mod na klientu (FML seznam modov iz rokovanja; vanilla klient nikoli ne dobi
 *     paketa, D-024),</li>
 *     <li>smejo uporabljati {@code /npcb} (OP 2),</li>
 *     <li>so vklopili {@code /npcb debug on} ali je v configu {@code debug.syncPathsToOps=true},</li>
 *     <li>so v istem svetu največ {@value #RANGE} blokov od entitete.</li>
 * </ol>
 * Na entiteto največ 4 Hz ({@value #MIN_INTERVAL} tickov); nespremenjena pot se ponovi na
 * {@value #KEEPALIVE} tickov, da klient ve, da je še živa. Brez prejemnikov se nič ne
 * sestavi in nič ne pošlje (merilo A4: števec ostane 0).
 */
public final class DebugSync {

    public static final DebugSync INSTANCE = new DebugSync();

    static final int MIN_INTERVAL = 5;
    static final int KEEPALIVE = 20;
    static final double RANGE = 128;

    private SimpleNetworkWrapper channel;
    private final Set<UUID> subscribers = new HashSet<>();
    private final Map<EntityLiving, Sent> sent = new WeakHashMap<>();
    private long packetsSent;
    private long tick;

    private static final class Sent {
        long tick;
        int hash;
        boolean active;
    }

    private DebugSync() {
    }

    /** Kliče {@code preInit} na obeh straneh; sporočilo je registrirano tudi na strežniku zaradi kodeka. */
    void register() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(NpcBaritoneMod.MODID);
        channel.registerMessage(PathSyncMessage.Handler.class, PathSyncMessage.class, 0, Side.CLIENT);
    }

    public long packetsSent() {
        return packetsSent;
    }

    public void resetCounter() {
        packetsSent = 0;
    }

    public boolean isSubscribed(EntityPlayerMP player) {
        return subscribers.contains(player.getUniqueID());
    }

    /** @return false, če igralec nima moda na klientu (paketov ne bo dobival) */
    public boolean subscribe(EntityPlayerMP player, boolean on) {
        if (on) {
            subscribers.add(player.getUniqueID());
        } else {
            subscribers.remove(player.getUniqueID());
            // pozabi, kaj je že dobil, da ob ponovnem vklopu dobi vse znova
            sent.clear();
        }
        return hasMod(player);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        subscribers.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || channel == null) {
            return;
        }
        tick++;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        List<EntityPlayerMP> recipients = recipients(server);
        if (recipients.isEmpty()) {
            return;
        }
        Set<EntityLiving> seen = Collections.newSetFromMap(new WeakHashMap<>());
        for (EntityLiving e : Attach.attached()) {
            Baritone b = Attach.get(e);
            NavStatus s = Attach.status(e);
            if (b == null || s == null || e.isDead) {
                continue;
            }
            seen.add(e);
            Sent last = sent.computeIfAbsent(e, k -> {
                Sent n = new Sent();
                n.tick = Long.MIN_VALUE / 2;
                return n;
            });
            if (tick - last.tick < MIN_INTERVAL) {
                continue;
            }
            PathSyncMessage msg = build(e, b, s);
            int hash = hash(msg);
            if (!shouldSend(tick, last.tick, last.hash, hash, last.active, msg.active)) {
                continue;
            }
            int n = sendNear(msg, e, recipients);
            last.tick = tick;
            if (n > 0 || !msg.active) {
                last.hash = hash;
                last.active = msg.active;
            } else {
                last.hash = 0; // nihče ni bil blizu: pošlji takoj, ko kdo pride v doseg
            }
        }
        // odpete entitete: klient naj pot pozabi
        List<EntityLiving> gone = new ArrayList<>();
        for (Map.Entry<EntityLiving, Sent> en : sent.entrySet()) {
            if (!seen.contains(en.getKey())) {
                gone.add(en.getKey());
            }
        }
        for (EntityLiving e : gone) {
            Sent last = sent.remove(e);
            if (last != null && last.active) {
                sendNear(PathSyncMessage.inactive(e.getEntityId()), e, recipients);
            }
        }
    }

    /**
     * Pravilo pošiljanja (po preverbi {@value #MIN_INTERVAL} tickov): sprememba vsebine,
     * prehod aktivno/neaktivno ali keepalive za aktivno pot. Neaktivno se pošlje enkrat.
     */
    static boolean shouldSend(long now, long lastTick, int lastHash, int hash, boolean lastActive, boolean active) {
        if (active != lastActive) {
            return true;
        }
        if (!active) {
            return false;
        }
        return hash != lastHash || now - lastTick >= KEEPALIVE;
    }

    static PathSyncMessage build(EntityLiving e, Baritone b, NavStatus s) {
        PathSyncMessage m = new PathSyncMessage();
        m.entityId = e.getEntityId();
        PathExecutor current = b.getPathingBehavior().getCurrent();
        PathExecutor next = b.getPathingBehavior().getNext();
        Goal goal = s.goal();
        NavStatus.State state = s.state();
        m.active = current != null || goal != null || state == NavStatus.State.SEARCHING;
        if (!m.active) {
            return m;
        }
        m.state = state.ordinal();
        if (current != null) {
            m.path = new ArrayList<BlockPos>(current.getPath().positions());
            m.pathPos = current.getPosition();
        }
        if (next != null) {
            m.next = new ArrayList<BlockPos>(next.getPath().positions());
        }
        if (goal instanceof IGoalRenderPos) {
            m.goal = ((IGoalRenderPos) goal).getGoalPos();
        }
        return m;
    }

    static int hash(PathSyncMessage m) {
        int h = m.active ? 1 : 0;
        h = 31 * h + m.state;
        h = 31 * h + m.pathPos;
        h = 31 * h + m.path.hashCode();
        h = 31 * h + m.next.hashCode();
        h = 31 * h + (m.goal == null ? 0 : m.goal.hashCode());
        return h;
    }

    private int sendNear(PathSyncMessage msg, EntityLiving e, List<EntityPlayerMP> recipients) {
        int n = 0;
        for (EntityPlayerMP p : recipients) {
            if (p.world != e.world || p.getDistanceSq(e) > RANGE * RANGE) {
                continue;
            }
            channel.sendTo(msg, p);
            packetsSent++;
            n++;
        }
        return n;
    }

    private List<EntityPlayerMP> recipients(MinecraftServer server) {
        boolean all = NpcBaritoneMod.config().syncPathsToOps;
        if (!all && subscribers.isEmpty()) {
            return Collections.emptyList();
        }
        List<EntityPlayerMP> out = new ArrayList<>();
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            if ((all || subscribers.contains(p.getUniqueID())) && p.canUseCommand(2, "npcb") && hasMod(p)) {
                out.add(p);
            }
        }
        return out;
    }

    /** Ali ima klient mod (seznam modov iz FML rokovanja). */
    static boolean hasMod(EntityPlayerMP player) {
        if (player.connection == null) {
            return false;
        }
        NetworkDispatcher d = NetworkDispatcher.get(player.connection.netManager);
        if (d == null) {
            return false;
        }
        Map<String, String> mods = d.getModList();
        return mods != null && mods.containsKey(NpcBaritoneMod.MODID);
    }
}
