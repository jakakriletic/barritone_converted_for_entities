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

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * M6.9 / M6 A1, A4: vanilla AI z Baritonovim navigatorjem, brez igralca. Husk (vanilla zombi
 * AI, ne gori) je pripet <b>brez</b> {@code puppet}; vaščan z {@code NoAI} stoji za zidom z režo
 * 20 blokov stran. Husk ga mora sam najti ({@code EntityAINearestAttackableTarget}) in napasti
 * ({@code EntityAIAttackMelee} — {@code setPath}, nato {@code tryMoveToEntityLiving} vsakih
 * 4–11 tickov). Izid: tick prvega udarca, klici navigatorja in nova iskanja (A4).
 *
 * <p>Arena: zaprta proga 30 × 11 (stene višine 3), zid na x=10 z režo 1×2 na z=+3.
 */
public final class AiTestRunner {

    public static final AiTestRunner INSTANCE = new AiTestRunner();
    static final int TIMEOUT_TICKS = 600;

    private Run run;

    private AiTestRunner() {
    }

    public boolean isRunning() {
        return run != null;
    }

    private static final class Run {
        final WorldServer world;
        final ICommandSender sender;
        final EntityHusk husk;
        final EntityVillager villager;
        final BaritonePathNavigate nav;
        int tick;
        int hitTick = -1;

        Run(WorldServer world, ICommandSender sender, EntityHusk husk, EntityVillager villager, BaritonePathNavigate nav) {
            this.world = world;
            this.sender = sender;
            this.husk = husk;
            this.villager = villager;
            this.nav = nav;
        }
    }

    public void startAttack(WorldServer world, ICommandSender sender, BlockPos o) {
        if (run != null) {
            throw new IllegalStateException("aitest already running");
        }
        buildArena(world, o);
        EntityVillager villager = new EntityVillager(world);
        villager.setLocationAndAngles(o.getX() + 20.5, o.getY(), o.getZ() + 0.5, 90f, 0f);
        villager.setNoAI(true);
        villager.enablePersistence();
        world.spawnEntity(villager);
        EntityHusk husk = new EntityHusk(world);
        husk.setLocationAndAngles(o.getX() + 0.5, o.getY(), o.getZ() + 0.5, -90f, 0f);
        husk.enablePersistence();
        world.spawnEntity(husk);
        Attach.attach(husk, false, NpcBaritoneMod.config()); // vanilla AI ostane
        BaritonePathNavigate nav = (BaritonePathNavigate) husk.getNavigator();
        run = new Run(world, sender, husk, villager, nav);
        say(run, "aitest attack: husk (vanilla AI + Baritone navigator) → vaščan za zidom z režo, 20 blokov");
    }

    @SubscribeEvent
    public void onCanUpdate(EntityEvent.CanUpdate event) {
        Run r = run;
        if (r != null && (event.getEntity() == r.husk || event.getEntity() == r.villager)) {
            event.setCanUpdate(true);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        Run r = run;
        if (r != null && event.getEntity() == r.villager && event.getSource().getTrueSource() == r.husk && r.hitTick < 0) {
            r.hitTick = r.tick;
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || run == null) {
            return;
        }
        Run r = run;
        r.world.resetUpdateEntityTick();
        r.tick++;
        if (r.hitTick >= 0 || r.tick >= TIMEOUT_TICKS || r.husk.isDead) {
            finish(r);
            run = null;
        }
    }

    private void finish(Run r) {
        String result = r.hitTick >= 0 ? "HIT" : (r.husk.isDead ? "DEAD" : "TIMEOUT");
        long searches = r.nav.baritone().getPathingBehavior().searchesStarted();
        String row = String.format(Locale.ROOT, "attack,%s,%d,%d,%d,%d,%.2f",
                result, r.hitTick, r.nav.requests(), r.nav.newSearches(), searches,
                r.husk.getDistance(r.villager));
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File out = new File("npcbaritone/runs", "ai-attack-" + stamp + ".csv");
        try {
            Files.createDirectories(out.getAbsoluteFile().getParentFile().toPath());
            Files.write(out.toPath(), Arrays.asList("scenario,result,hit_tick,nav_requests,nav_new_searches,searches,final_distance", row),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            NpcBaritoneMod.LOG.error("cannot write {}", out, e);
        }
        say(r, String.format(Locale.ROOT, "aitest attack: %s po %d tickih; klicev navigatorja %d, novih iskanj %d, iskanj skupaj %d",
                result, r.hitTick, r.nav.requests(), r.nav.newSearches(), searches));
        NpcBaritoneMod.LOG.info("NPCB-AITEST-DONE scenario=attack result={} hit_tick={} nav_requests={} nav_new_searches={} searches={} csv={}",
                result, r.hitTick, r.nav.requests(), r.nav.newSearches(), searches, out.getAbsolutePath());
        Attach.detach(r.husk);
        r.husk.setDead();
        r.villager.setDead();
    }

    /** Zaprta proga x -1..31, z -6..6; tla y-1; zid x=10 z režo 1×2 na z=+3. */
    static void buildArena(WorldServer w, BlockPos o) {
        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState air = Blocks.AIR.getDefaultState();
        for (BlockPos p : BlockPos.getAllInBoxMutable(o.add(-1, -1, -6), o.add(31, 4, 6))) {
            int dx = p.getX() - o.getX();
            int dy = p.getY() - o.getY();
            int dz = p.getZ() - o.getZ();
            boolean floor = dy == -1;
            boolean wall = (dx == -1 || dx == 31 || dz == -6 || dz == 6) && dy <= 2;
            boolean mid = dx == 10 && dy <= 2 && !(dz == 3 && dy <= 1);
            w.setBlockState(p.toImmutable(), floor || wall || mid ? stone : air, 2);
        }
    }

    private static void say(Run r, String msg) {
        NpcBaritoneMod.LOG.info(msg);
        try {
            r.sender.sendMessage(new TextComponentString("[npcb] " + msg));
        } catch (RuntimeException ignored) {
            // pošiljatelj je lahko odšel
        }
    }
}
