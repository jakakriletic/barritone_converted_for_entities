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

package si.ladja.npcbaritone.forge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import si.ladja.npcbaritone.forge.NpcBaritoneMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * M3.2: stanje poti ene entitete, strežnik → klient z modom (samo debug prikaz, D-024).
 * Vzorec Automatonovega {@code PathExecutor.writeToPacket} ({@code 2ebfbf0f}), brez
 * blokov za rušenje/postavljanje (D-015).
 *
 * <p>Format (verzija {@link #FORMAT}): {@code byte format, int entityId, byte flags}; če je
 * {@code ACTIVE}: {@code byte state, varint pathPos, positions(path), positions(next)}, in
 * {@code long goal}, če je {@code HAS_GOAL}. {@code positions} = {@code varint n} + {@code n × long}
 * ({@link BlockPos#toLong()}). Neaktivno sporočilo pove klientu, naj pot pozabi.
 */
public final class PathSyncMessage implements IMessage {

    public static final int FORMAT = 1;
    /** Največ točk na pot; daljša pot se odreže (izris kaže prvih {@value} točk). */
    public static final int MAX_POSITIONS = 2048;

    private static final int ACTIVE = 1;
    private static final int HAS_GOAL = 2;
    private static final int HAS_NEXT = 4;

    public int entityId;
    public boolean active;
    /** Ordinal {@code NavStatus.State}. */
    public int state;
    public int pathPos;
    public List<BlockPos> path = Collections.emptyList();
    public List<BlockPos> next = Collections.emptyList();
    public BlockPos goal;

    public PathSyncMessage() {
    }

    public static PathSyncMessage inactive(int entityId) {
        PathSyncMessage m = new PathSyncMessage();
        m.entityId = entityId;
        return m;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(FORMAT);
        buf.writeInt(entityId);
        int flags = (active ? ACTIVE : 0) | (goal != null ? HAS_GOAL : 0) | (!next.isEmpty() ? HAS_NEXT : 0);
        buf.writeByte(flags);
        if (!active) {
            return;
        }
        buf.writeByte(state);
        ByteBufUtils.writeVarInt(buf, Math.max(0, pathPos), 5);
        writePositions(buf, path);
        if (!next.isEmpty()) {
            writePositions(buf, next);
        }
        if (goal != null) {
            buf.writeLong(goal.toLong());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int format = buf.readUnsignedByte();
        if (format != FORMAT) {
            throw new IllegalArgumentException("npcbaritone path sync: format " + format + " != " + FORMAT);
        }
        entityId = buf.readInt();
        int flags = buf.readUnsignedByte();
        active = (flags & ACTIVE) != 0;
        if (!active) {
            return;
        }
        state = buf.readUnsignedByte();
        pathPos = ByteBufUtils.readVarInt(buf, 5);
        path = readPositions(buf);
        next = (flags & HAS_NEXT) != 0 ? readPositions(buf) : Collections.emptyList();
        goal = (flags & HAS_GOAL) != 0 ? BlockPos.fromLong(buf.readLong()) : null;
    }

    private static void writePositions(ByteBuf buf, List<BlockPos> positions) {
        int n = Math.min(positions.size(), MAX_POSITIONS);
        ByteBufUtils.writeVarInt(buf, n, 5);
        for (int i = 0; i < n; i++) {
            buf.writeLong(positions.get(i).toLong());
        }
    }

    private static List<BlockPos> readPositions(ByteBuf buf) {
        int n = ByteBufUtils.readVarInt(buf, 5);
        if (n < 0 || n > MAX_POSITIONS) {
            throw new IllegalArgumentException("npcbaritone path sync: " + n + " positions");
        }
        List<BlockPos> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(BlockPos.fromLong(buf.readLong()));
        }
        return out;
    }

    /** Registriran na obeh straneh; klientsko delo opravi proxy (strežnik nima klientskih razredov). */
    public static final class Handler implements IMessageHandler<PathSyncMessage, IMessage> {
        @Override
        public IMessage onMessage(PathSyncMessage message, MessageContext ctx) {
            NpcBaritoneMod.proxy.onPathSync(message);
            return null;
        }
    }
}
