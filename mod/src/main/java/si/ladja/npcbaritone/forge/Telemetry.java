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

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * M2: meritve na strežniku.
 * <ul>
 *     <li>števec {@code ChunkEvent.Load} na strežniških svetovih (D-012, M2.10, merilo A4)</li>
 *     <li>{@code /npcb speedtest}: 10 s ravne hoje ali sprinta, m/s in tresenje yaw (A2, A3, A5)</li>
 * </ul>
 * Vse teče na strežniški niti (Forge dogodki).
 */
public final class Telemetry {

    public static final Telemetry INSTANCE = new Telemetry();

    static final int WARMUP_TICKS = 20;
    static final int MEASURE_TICKS = 200;
    static final int TEST_DISTANCE = 80;

    private long chunkLoads;
    private long chunkLoadsSinceReset;
    private final List<SpeedTest> tests = new ArrayList<>();

    private Telemetry() {
    }

    public long chunkLoadsSinceReset() {
        return chunkLoadsSinceReset;
    }

    public long chunkLoadsTotal() {
        return chunkLoads;
    }

    public void resetChunkLoads() {
        chunkLoadsSinceReset = 0;
    }

    /** Speedtest brez igralca teče samo za izbrano entiteto. */
    @SubscribeEvent
    public void onCanUpdate(EntityEvent.CanUpdate event) {
        for (SpeedTest test : tests) {
            if (event.getEntity() == test.entity) {
                event.setCanUpdate(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getWorld().isRemote) {
            chunkLoads++;
            chunkLoadsSinceReset++;
        }
    }

    // ------------------------------------------------------------------ speedtest

    private static final class SpeedTest {
        final EntityLiving entity;
        final Baritone baritone;
        final ICommandSender sender;
        final boolean sprint;
        final Settings restore;
        final ForgeChunkManager.Ticket ticket;
        int ticks;
        double x0, z0;
        final float[] yaw = new float[WARMUP_TICKS + MEASURE_TICKS + 1];
        boolean done;

        SpeedTest(EntityLiving entity, Baritone baritone, ICommandSender sender, boolean sprint, Settings restore,
                  ForgeChunkManager.Ticket ticket) {
            this.entity = entity;
            this.baritone = baritone;
            this.sender = sender;
            this.sprint = sprint;
            this.restore = restore;
            this.ticket = ticket;
        }
    }

    /** Začne test: entiteta gre 80 blokov naravnost v smeri, v katero gleda. */
    public BlockPos startSpeedTest(EntityLiving entity, Baritone baritone, ICommandSender sender, boolean sprint, NpcbConfig config) {
        Settings restore = baritone.getSettings();
        Settings profile = Attach.profileFor(config);
        profile.allowSprint.value = sprint;
        baritone.setSettings(profile);
        EnumFacing facing = EnumFacing.fromAngle(entity.rotationYaw);
        BlockPos start = new BlockPos(entity);
        BlockPos goal = start.offset(facing, TEST_DISTANCE);
        ForgeChunkManager.Ticket ticket = null;
        // Ukaz je namenska meritev: pripravi ravno progo pred iskanjem, ko na strežniku ni igralca.
        if (entity.world instanceof WorldServer) {
            WorldServer world = (WorldServer) entity.world;
            ticket = ForgeChunkManager.requestTicket(NpcBaritoneMod.INSTANCE, world, ForgeChunkManager.Type.NORMAL);
            if (ticket == null) {
                throw new IllegalStateException("speedtest: ni Forge chunk ticketa");
            }
            int minX = (Math.min(start.getX(), goal.getX()) - 16) >> 4;
            int maxX = (Math.max(start.getX(), goal.getX()) + 16) >> 4;
            int minZ = (Math.min(start.getZ(), goal.getZ()) - 16) >> 4;
            int maxZ = (Math.max(start.getZ(), goal.getZ()) + 16) >> 4;
            for (int cx = minX; cx <= maxX; cx++) {
                for (int cz = minZ; cz <= maxZ; cz++) {
                    world.getChunkProvider().provideChunk(cx, cz);
                    if (cz == (start.getZ() >> 4)) {
                        ForgeChunkManager.forceChunk(ticket, new ChunkPos(cx, cz));
                    }
                }
            }
        }
        // /tp je lahko premaknil entiteto v še negeneriran chunk; po pripravi proge jo znova vpiši vanj.
        entity.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
        tests.add(new SpeedTest(entity, baritone, sender, sprint, restore, ticket));
        return goal;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || tests.isEmpty()) {
            return;
        }
        for (Iterator<SpeedTest> it = tests.iterator(); it.hasNext(); ) {
            SpeedTest t = it.next();
            if (t.entity.isDead) {
                if (t.ticket != null) ForgeChunkManager.releaseTicket(t.ticket);
                it.remove();
                continue;
            }
            CourseRunner.keepTicking(t.entity);
            if (t.ticks < t.yaw.length) {
                t.yaw[t.ticks] = t.entity.rotationYaw;
            }
            if (t.ticks == WARMUP_TICKS) {
                t.x0 = t.entity.posX;
                t.z0 = t.entity.posZ;
            }
            boolean stopped = t.ticks > WARMUP_TICKS && !t.baritone.getPathingBehavior().isPathing()
                    && !t.baritone.getPathingBehavior().getInProgress().isPresent();
            if (t.ticks >= WARMUP_TICKS + MEASURE_TICKS || stopped) {
                finish(t, stopped);
                it.remove();
                continue;
            }
            t.ticks++;
        }
    }

    private void finish(SpeedTest t, boolean stopped) {
        if (t.ticket != null) ForgeChunkManager.releaseTicket(t.ticket);
        int measured = t.ticks - WARMUP_TICKS;
        double dx = t.entity.posX - t.x0;
        double dz = t.entity.posZ - t.z0;
        double meters = Math.sqrt(dx * dx + dz * dz);
        double mps = measured <= 0 ? 0 : meters / (measured / 20.0);
        int jitter = 0;
        for (int i = WARMUP_TICKS + 5; i <= t.ticks && i < t.yaw.length; i++) {
            if (Math.abs(MathHelper.wrapDegrees(t.yaw[i] - t.yaw[i - 5])) > 90) {
                jitter++;
            }
        }
        t.baritone.getPathingBehavior().cancelEverything();
        t.baritone.setSettings(t.restore);
        String msg = String.format(Locale.ROOT, "speedtest %s: %.3f m/s (%.2f m v %d tickih)%s; tresenje yaw>90/5t: %d",
                t.sprint ? "sprint" : "hoja", mps, meters, measured, stopped ? " [pot se je končala prej]" : "", jitter);
        NpcBaritoneMod.LOG.info("NPCB-SPEEDTEST-DONE {} entity={} {}", msg, t.entity.getEntityId(), t.entity.getName());
        t.sender.sendMessage(new TextComponentString("[npcb] " + msg));
    }
}
