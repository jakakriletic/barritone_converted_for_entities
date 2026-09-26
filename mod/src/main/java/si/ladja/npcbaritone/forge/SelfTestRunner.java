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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemDoor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Samodejne "ročne" preverbe v klientu (M3 A1 + ukazi, D-008, M6 A1–A3): igralec je v svetu,
 * vpiše {@code /npcb selftest} in stoji. Okoli njega se postavi arena (zato nov superflat svet),
 * koraki tečejo zaporedno, izid gre v klepet in v {@code npcbaritone/runs/selftest-<čas>.csv}.
 *
 * <p>Med testom je igralec zaščiten (napadi se štejejo, škoda se prekliče); igralni način,
 * težavnost, čas in pravili {@code doMobSpawning}/{@code doDaylightCycle} se na koncu vrnejo.
 * Zombi in vaščan imata vzporedno kontrolo brez Baritona: če pade Baritonova entiteta,
 * kontrola pove, ali vanilla v istem okolju sploh uspe.
 *
 * <p>Arena (relativno na igralca o, tla y-1): zid x=8 (z -6..6) z režo 1×2 na z=4 (zombi);
 * zid x=-5 (z -2..2, višina 2) (volk); koliba x 16..22, z 14..20 z vrati na (16, 0, 17) (vaščan).
 */
public final class SelfTestRunner {

    public static final SelfTestRunner INSTANCE = new SelfTestRunner();

    /**
     * M3 A2 (vanilla klient na dedicated): z okoljsko spremenljivko {@code NPCB_SELFTEST_ON_JOIN=1}
     * se test začne sam 5 s po prijavi prvega igralca (brez OP), strežnik se po koncu ustavi.
     */
    static final boolean ON_JOIN = "1".equals(System.getenv("NPCB_SELFTEST_ON_JOIN"));

    private Run run;
    private EntityPlayerMP pending;
    private int pendingTicks;
    private int shutdownTicks = -1;

    private SelfTestRunner() {
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (ON_JOIN && run == null && pending == null && shutdownTicks < 0 && event.player instanceof EntityPlayerMP) {
            pending = (EntityPlayerMP) event.player;
            pendingTicks = 100;
            pending.sendMessage(new TextComponentString("[npcb] " + TextFormatting.YELLOW
                    + "selftest se začne čez 5 s — stoj pri miru, strežnik se po koncu ustavi"));
        }
    }

    public boolean isRunning() {
        return run != null;
    }

    /** En korak: {@link #start}, nato {@link #tick} vsak tick, dokler ne vrne izida (true/false). */
    private abstract static class Step {
        final String id;
        final String criterion;
        final int timeout;
        String detail = "";

        Step(String id, String criterion, int timeout) {
            this.id = id;
            this.criterion = criterion;
            this.timeout = timeout;
        }

        abstract void start(Run r);

        /** @return null = še teče; true/false = izid */
        abstract Boolean tick(Run r, int t);

        /** Ob izteku časa. */
        Boolean timeout(Run r) {
            return false;
        }

        void end(Run r) {
        }
    }

    private static final class Run {
        final WorldServer world;
        final MinecraftServer server;
        final EntityPlayerMP player;
        final BlockPos o;
        final GameType gameType;
        final EnumDifficulty difficulty;
        final long time;
        final String mobSpawning;
        final String daylight;
        final List<Step> steps = new ArrayList<>();
        final List<String> rows = new ArrayList<>();
        final List<EntityLiving> spawned = new ArrayList<>();
        int index = -1;
        int t;
        int passed;
        // stanje korakov
        EntityHusk husk;
        EntityZombie zombie;
        EntityZombie controlZombie;
        int zombieHit = -1;
        int controlZombieHit = -1;
        EntityWolf wolf;
        EntityVillager villager;
        EntityVillager controlVillager;

        Run(EntityPlayerMP player, BlockPos o) {
            this.player = player;
            this.world = (WorldServer) player.world;
            this.server = player.getServer();
            this.o = o;
            this.gameType = player.interactionManager.getGameType();
            this.difficulty = world.getDifficulty();
            this.time = world.getWorldTime();
            this.mobSpawning = world.getGameRules().getString("doMobSpawning");
            this.daylight = world.getGameRules().getString("doDaylightCycle");
        }

        BlockPos at(int x, int y, int z) {
            return o.add(x, y, z);
        }
    }

    public void start(EntityPlayerMP player) {
        if (run != null) {
            throw new IllegalStateException("selftest already running");
        }
        Run r = new Run(player, new BlockPos(player));
        r.world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        r.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        buildArena(r);
        player.connection.setPlayerLocation(r.o.getX() + 0.5, r.o.getY(), r.o.getZ() + 0.5, -90f, 20f);
        r.steps.add(new RenderOn());
        r.steps.add(new Commands());
        r.steps.add(new RenderOff());
        r.steps.add(new ZombieAttack());
        r.steps.add(new WolfFollows());
        r.steps.add(new VillagerHome());
        run = r;
        say(r, TextFormatting.YELLOW + "selftest: " + r.steps.size() + " korakov, ~3 min. Stoj pri miru, ne odpiraj menija (ESC ustavi strežnik).");
        // M3 A2: vanilla klient nima kanala npcbaritone (D-024) — zapis, s katerim klientom je test tekel
        NpcBaritoneMod.LOG.info("NPCB-SELFTEST-CLIENT player={} mod_on_client={} dedicated={}", player.getName(),
                DebugSync.hasMod(player), r.server.isDedicatedServer());
        next(r);
    }

    public void abort(String why) {
        Run r = run;
        if (r != null) {
            if (r.index >= 0 && r.index < r.steps.size()) {
                record(r, r.steps.get(r.index), false, "prekinjeno: " + why);
            }
            finish(r);
        }
    }

    private void next(Run r) {
        if (r.index >= 0) {
            r.steps.get(r.index).end(r);
        }
        r.index++;
        r.t = 0;
        if (r.index >= r.steps.size()) {
            finish(r);
            return;
        }
        Step s = r.steps.get(r.index);
        say(r, TextFormatting.GRAY + "[" + (r.index + 1) + "/" + r.steps.size() + "] " + s.id + ": " + s.criterion);
        s.start(r);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (pending != null && --pendingTicks <= 0) {
            EntityPlayerMP p = pending;
            pending = null;
            if (!p.hasDisconnected()) {
                start(p);
            }
        }
        if (shutdownTicks > 0 && --shutdownTicks == 0) {
            MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                NpcBaritoneMod.LOG.info("NPCB-SELFTEST-SHUTDOWN");
                server.initiateShutdown();
            }
        }
        Run r = run;
        if (r == null) {
            return;
        }
        if (r.player.isDead || r.player.hasDisconnected()) {
            abort("igralec je odšel");
            return;
        }
        r.t++;
        Step s = r.steps.get(r.index);
        Boolean result;
        try {
            result = r.t > s.timeout ? s.timeout(r) : s.tick(r, r.t);
        } catch (RuntimeException e) {
            NpcBaritoneMod.LOG.error("selftest {}", s.id, e);
            s.detail = "izjema: " + e;
            result = false;
        }
        if (r.t % 20 == 0 && result == null) {
            for (EntityLiving e : new EntityLiving[]{r.zombie, r.controlZombie, r.wolf, r.villager, r.controlVillager}) {
                if (e != null && !e.isDead) {
                    NpcBaritoneMod.LOG.info("NPCB-SELFTEST-DBG {} t={} {} {}", s.id, r.t, e.getName(), AiTestRunner.describe(e));
                }
            }
        }
        if (result != null) {
            record(r, s, result, s.detail);
            next(r);
        }
    }

    /** Igralec med testom ne dobi škode; napade entitet testa šteje. */
    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        Run r = run;
        if (r == null || event.getEntityLiving() != r.player) {
            return;
        }
        Entity src = event.getSource().getTrueSource();
        if (src != null && src == r.zombie && r.zombieHit < 0) {
            r.zombieHit = r.t;
        }
        if (src != null && src == r.controlZombie && r.controlZombieHit < 0) {
            r.controlZombieHit = r.t;
        }
        event.setCanceled(true);
    }

    // ------------------------------------------------------------------ koraki

    /** M3 A1: OP z modom na klientu vidi pot (izris v istem procesu). */
    private static final class RenderOn extends Step {
        long frames0;

        RenderOn() {
            super("M3 A1 izris", "debug on, husk (puppet) gre 12 blokov; klient mora narisati pot v 3 s", 60);
        }

        @Override
        void start(Run r) {
            r.husk = spawn(r, new EntityHusk(r.world), r.at(2, 0, 10), -90f);
            Attach.attach(r.husk, true, NpcBaritoneMod.config());
            boolean mod = DebugSync.INSTANCE.subscribe(r.player, true);
            long[] rp = NpcBaritoneMod.proxy.renderedPaths();
            frames0 = rp == null ? 0 : rp[0];
            detail = mod ? "" : "strežnik ne vidi moda na klientu";
            Attach.get(r.husk).getCustomGoalProcess().setGoalAndPath(new GoalBlock(r.at(14, 0, 10)));
        }

        @Override
        Boolean tick(Run r, int t) {
            long[] rp = NpcBaritoneMod.proxy.renderedPaths();
            if (rp == null) {
                detail = "SKIP: ni klienta v tem procesu (dedicated) — preveri na očeh";
                return true;
            }
            if (rp[0] > frames0 && rp[1] > 0) {
                detail = String.format(Locale.ROOT, "pot narisana po %d tickih (%d sličic, poti v sličici %d)", t, rp[0] - frames0, rp[1]);
                return true;
            }
            return null;
        }

        @Override
        Boolean timeout(Run r) {
            long[] rp = NpcBaritoneMod.proxy.renderedPaths();
            detail = (detail.isEmpty() ? "" : detail + "; ") + "v 3 s ni narisane poti"
                    + (rp != null && System.currentTimeMillis() - rp[2] > 1000 ? " (okno ne riše — minimizirano?)" : "");
            return false;
        }
    }

    /** M3 preverbe 3: ukazi status, profile list, profile walk, goto do igralca. */
    private static final class Commands extends Step {
        BlockPos goal;
        final List<String> failed = new ArrayList<>();

        Commands() {
            super("M3 ukazi", "status, profile list, profile walk, goto @igralec — husk pride do tebe", 600);
        }

        @Override
        void start(Run r) {
            String id = r.husk.getCachedUniqueIdString();
            goal = new BlockPos(r.player);
            for (String cmd : new String[]{"npcb status " + id, "npcb profile list", "npcb profile " + id + " walk",
                    "npcb goto " + id + " " + r.player.getName()}) {
                net.minecraft.command.ICommandSender as = r.player.canUseCommand(2, "npcb") ? r.player : r.server;
                if (r.server.getCommandManager().executeCommand(as, cmd) <= 0) {
                    failed.add(cmd.split(" ")[1]);
                }
            }
        }

        @Override
        Boolean tick(Run r, int t) {
            if (!failed.isEmpty()) {
                detail = "ukazi z napako: " + failed;
                return false;
            }
            double d = Math.sqrt(r.husk.getDistanceSqToCenter(goal));
            if (d < 1.5) {
                detail = String.format(Locale.ROOT, "4/4 ukazi OK, husk pri tebi po %d tickih (profil %s)", t, Attach.profile(r.husk));
                return true;
            }
            return null;
        }

        @Override
        Boolean timeout(Run r) {
            detail = String.format(Locale.ROOT, "husk ni prišel v 30 s (razdalja %.1f, stanje %s)",
                    Math.sqrt(r.husk.getDistanceSqToCenter(goal)), Attach.status(r.husk).state());
            return false;
        }
    }

    /** M3 A1: po debug off črte izginejo (klient pozabi pot po 3 s brez osvežitve). */
    private static final class RenderOff extends Step {
        long offMs;

        RenderOff() {
            super("M3 A1 debug off", "po /npcb debug off črte izginejo v ≤ 3,5 s", 80);
        }

        @Override
        void start(Run r) {
            // husk mora imeti aktivno pot, sicer ni kaj skriti
            Attach.get(r.husk).getCustomGoalProcess().setGoalAndPath(new GoalBlock(r.at(12, 0, 10)));
            offMs = -1;
        }

        @Override
        Boolean tick(Run r, int t) {
            long[] rp = NpcBaritoneMod.proxy.renderedPaths();
            if (rp == null) {
                detail = "SKIP: ni klienta v tem procesu";
                return true;
            }
            if (offMs < 0) {
                if (t >= 20) { // pot je spet narisana, nato izklop
                    DebugSync.INSTANCE.subscribe(r.player, false);
                    offMs = System.currentTimeMillis();
                }
                return null;
            }
            if (rp[1] == 0 && rp[2] > offMs) {
                long ms = rp[2] - offMs;
                detail = "črte izginile po " + ms + " ms";
                return ms <= 3500;
            }
            return null;
        }

        @Override
        void end(Run r) {
            DebugSync.INSTANCE.subscribe(r.player, false);
            kill(r, r.husk);
        }
    }

    /** D-008 + M6 A1: zombi brez puppet (vanilla AI + Baritonov navigator) najde in napade igralca. */
    private static final class ZombieAttack extends Step {
        BaritonePathNavigate nav;

        ZombieAttack() {
            super("D-008 + M6 A1 zombi", "noč, survival; zombi za zidom z režo te najde in udari (kontrola brez Baritona vzporedno)", 900);
        }

        @Override
        void start(Run r) {
            r.player.setGameType(GameType.SURVIVAL);
            if (r.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
                r.server.setDifficultyForAllWorlds(EnumDifficulty.EASY);
            }
            r.world.setWorldTime(18000);
            // vanilla EntityAINearestAttackableTarget rabi vidno črto: obe črti do igralca gresta skozi režo (8, 0..1, 4)
            r.zombie = spawn(r, new EntityZombie(r.world), r.at(16, 0, 8), 90f);
            r.controlZombie = spawn(r, new EntityZombie(r.world), r.at(15, 0, 7), 90f);
            Attach.attach(r.zombie, false, NpcBaritoneMod.config());
            nav = (BaritonePathNavigate) r.zombie.getNavigator();
        }

        @Override
        Boolean tick(Run r, int t) {
            if (r.zombieHit >= 0 && (r.controlZombieHit >= 0 || t > r.zombieHit + 200)) {
                return result(r);
            }
            return null;
        }

        @Override
        Boolean timeout(Run r) {
            return result(r);
        }

        private Boolean result(Run r) {
            long searches = nav.baritone().getPathingBehavior().searchesStarted();
            detail = String.format(Locale.ROOT, "Baritone: %s, klicev navigatorja %d, iskanj %d; kontrola: %s",
                    r.zombieHit >= 0 ? "udarec po " + r.zombieHit + " tickih" : "NI udarca v 45 s",
                    nav.requests(), searches,
                    r.controlZombieHit >= 0 ? "udarec po " + r.controlZombieHit + " tickih" : "NI udarca");
            return r.zombieHit >= 0 && searches > 0;
        }

        @Override
        void end(Run r) {
            kill(r, r.zombie);
            kill(r, r.controlZombie);
        }
    }

    /** M6 A3: ukročen volk (EntityAIFollowOwner) pride do igralca okoli zidu, brez teleporta. */
    private static final class WolfFollows extends Step {
        BaritonePathNavigate nav;
        double lastX;
        double lastZ;
        int teleports;

        WolfFollows() {
            super("M6 A3 volk", "ukročen volk 11 blokov stran za zidom pride do tebe peš (brez teleporta)", 400);
        }

        @Override
        void start(Run r) {
            EntityWolf w = new EntityWolf(r.world);
            w.setTamed(true);
            w.setOwnerId(r.player.getUniqueID());
            w.setSitting(false);
            r.wolf = spawn(r, w, r.at(-11, 0, 0), -90f);
            Attach.attach(r.wolf, false, NpcBaritoneMod.config());
            nav = (BaritonePathNavigate) r.wolf.getNavigator();
            lastX = r.wolf.posX;
            lastZ = r.wolf.posZ;
        }

        @Override
        Boolean tick(Run r, int t) {
            double step = Math.hypot(r.wolf.posX - lastX, r.wolf.posZ - lastZ);
            if (step > 2.5) {
                teleports++;
            }
            lastX = r.wolf.posX;
            lastZ = r.wolf.posZ;
            if (r.wolf.getDistance(r.player) <= 3.0) {
                long searches = nav.baritone().getPathingBehavior().searchesStarted();
                detail = String.format(Locale.ROOT, "pri tebi po %d tickih; klicev navigatorja %d, iskanj %d, teleportov %d",
                        t, nav.requests(), searches, teleports);
                return searches > 0 && teleports == 0;
            }
            return null;
        }

        @Override
        Boolean timeout(Run r) {
            detail = String.format(Locale.ROOT, "ni prišel v 20 s: razdalja %.1f, klicev navigatorja %d, iskanj %d, teleportov %d",
                    r.wolf.getDistance(r.player), nav.requests(), nav.baritone().getPathingBehavior().searchesStarted(), teleports);
            return false;
        }

        @Override
        void end(Run r) {
            kill(r, r.wolf);
        }
    }

    /** M6 A2: ponoči gre vaščan (EntityAIMoveIndoors) skozi vrata v kolibo; kontrola vzporedno. */
    private static final class VillagerHome extends Step {
        BaritonePathNavigate nav;
        int villageTick = -1;
        int homeTick = -1;
        int controlHomeTick = -1;

        VillagerHome() {
            super("M6 A2 vaščan", "noč; vaščan 8 blokov pred kolibo gre skozi vrata noter (kontrola brez Baritona vzporedno)", 1800);
        }

        @Override
        void start(Run r) {
            r.world.setWorldTime(18000);
            r.villager = spawn(r, new EntityVillager(r.world), r.at(8, 0, 16), -90f);
            r.controlVillager = spawn(r, new EntityVillager(r.world), r.at(8, 0, 18), -90f);
            Attach.attach(r.villager, false, NpcBaritoneMod.config());
            nav = (BaritonePathNavigate) r.villager.getNavigator();
        }

        private static boolean inside(Run r, EntityLiving e) {
            double x = e.posX - r.o.getX();
            double z = e.posZ - r.o.getZ();
            return x > 17 && x < 22 && z > 15 && z < 20;
        }

        @Override
        Boolean tick(Run r, int t) {
            if (villageTick < 0 && r.world.getVillageCollection().getNearestVillage(r.at(16, 0, 17), 32) != null) {
                villageTick = t;
            }
            if (homeTick < 0 && inside(r, r.villager)) {
                homeTick = t;
            }
            if (controlHomeTick < 0 && inside(r, r.controlVillager)) {
                controlHomeTick = t;
            }
            if (homeTick >= 0 && (controlHomeTick >= 0 || t > homeTick + 200)) {
                return result(r);
            }
            return null;
        }

        @Override
        Boolean timeout(Run r) {
            return result(r);
        }

        private Boolean result(Run r) {
            detail = String.format(Locale.ROOT, "vas zaznana po %s; Baritone: %s (klicev navigatorja %d, iskanj %d); kontrola: %s",
                    villageTick < 0 ? "— (NI vasi)" : villageTick + " tickih",
                    homeTick >= 0 ? "v kolibi po " + homeTick + " tickih" : "NI v kolibi v 90 s",
                    nav.requests(), nav.baritone().getPathingBehavior().searchesStarted(),
                    controlHomeTick >= 0 ? "v kolibi po " + controlHomeTick + " tickih" : "NI v kolibi");
            return homeTick >= 0;
        }

        @Override
        void end(Run r) {
            kill(r, r.villager);
            kill(r, r.controlVillager);
        }
    }

    // ------------------------------------------------------------------ pomožno

    private static <T extends EntityLiving> T spawn(Run r, T e, BlockPos p, float yaw) {
        e.setLocationAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0f);
        e.enablePersistence();
        r.world.spawnEntity(e);
        r.spawned.add(e);
        return e;
    }

    private static void kill(Run r, EntityLiving e) {
        if (e != null) {
            Attach.detach(e);
            e.setDead();
        }
    }

    private static void record(Run r, Step s, boolean pass, String detail) {
        if (pass) {
            r.passed++;
        }
        r.rows.add(String.format(Locale.ROOT, "%d,\"%s\",%s,%d,\"%s\"", r.index + 1, s.id, pass ? "OK" : "NAPAKA", r.t,
                detail.replace("\"", "'")));
        say(r, (pass ? TextFormatting.GREEN + "OK      " : TextFormatting.RED + "NAPAKA  ") + TextFormatting.RESET + s.id + " — " + detail);
        NpcBaritoneMod.LOG.info("NPCB-SELFTEST-STEP {} {} {} ticks={} {}", r.index + 1, s.id, pass ? "OK" : "NAPAKA", r.t, detail);
    }

    private void finish(Run r) {
        run = null;
        for (EntityLiving e : r.spawned) {
            kill(r, e);
        }
        DebugSync.INSTANCE.subscribe(r.player, false);
        r.player.setGameType(r.gameType);
        r.server.setDifficultyForAllWorlds(r.difficulty);
        r.world.setWorldTime(r.time);
        r.world.getGameRules().setOrCreateGameRule("doMobSpawning", r.mobSpawning);
        r.world.getGameRules().setOrCreateGameRule("doDaylightCycle", r.daylight);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File out = new File("npcbaritone/runs", "selftest-" + stamp + ".csv");
        List<String> lines = new ArrayList<>();
        lines.add("step,name,result,ticks,detail");
        lines.addAll(r.rows);
        try {
            Files.createDirectories(out.getAbsoluteFile().getParentFile().toPath());
            Files.write(out.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            NpcBaritoneMod.LOG.error("cannot write {}", out, e);
        }
        say(r, (r.passed == r.steps.size() ? TextFormatting.GREEN : TextFormatting.RED) + "selftest končan: " + r.passed + "/"
                + r.steps.size() + " OK" + TextFormatting.RESET + " — " + out.getPath());
        NpcBaritoneMod.LOG.info("NPCB-SELFTEST-DONE passed={} total={} csv={}", r.passed, r.steps.size(), out.getAbsolutePath());
        if (ON_JOIN && r.server.isDedicatedServer()) {
            shutdownTicks = 200; // igralec še 10 s vidi izid
            say(r, "strežnik se ustavi čez 10 s");
        }
    }

    /** Arena okoli igralca: x -16..30, z -10..24, tla y-1, zrak do y+6. */
    private static void buildArena(Run r) {
        WorldServer w = r.world;
        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState planks = Blocks.PLANKS.getDefaultState();
        for (BlockPos p : BlockPos.getAllInBoxMutable(r.at(-16, -1, -10), r.at(30, 6, 24))) {
            int dx = p.getX() - r.o.getX();
            int dy = p.getY() - r.o.getY();
            int dz = p.getZ() - r.o.getZ();
            IBlockState s = air;
            if (dy == -1) {
                s = stone;
            } else if (dx == 8 && dz >= -6 && dz <= 6 && dy <= 2 && !(dz == 4 && dy <= 1)) {
                s = stone; // zid za zombija z režo 1×2 na z=4
            } else if (dx == -5 && dz >= -2 && dz <= 2 && dy <= 1) {
                s = stone; // zid za volka
            } else if (dx >= 16 && dx <= 22 && dz >= 14 && dz <= 20 && dy <= 3) {
                boolean wall = dx == 16 || dx == 22 || dz == 14 || dz == 20;
                s = dy == 3 ? planks : (wall ? planks : air); // koliba s streho
            }
            w.setBlockState(p.toImmutable(), s, 2);
        }
        BlockPos door = r.at(16, 0, 17);
        w.setBlockState(door, air, 2);
        w.setBlockState(door.up(), air, 2);
        ItemDoor.placeDoor(w, door, EnumFacing.EAST, Blocks.OAK_DOOR, false);
    }

    private static void say(Run r, String msg) {
        NpcBaritoneMod.LOG.info("[selftest] {}", TextFormatting.getTextWithoutFormattingCodes(msg));
        try {
            r.player.sendMessage(new TextComponentString("[npcb] " + msg));
        } catch (RuntimeException ignored) {
            // igralec je lahko odšel
        }
    }
}
