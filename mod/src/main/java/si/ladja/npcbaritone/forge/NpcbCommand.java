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

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * M2.8, M3.1: ukazi za razvoj in teste (OP 2). Entiteta je izbirnik ali UUID
 * (npr. {@code @e[type=zombie,c=1]}).
 * <pre>
 * /npcb attach &lt;entity&gt; [puppet] [profil]
 * /npcb detach &lt;entity&gt;
 * /npcb goto &lt;entity&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt; | &lt;cilj-entiteta&gt;
 * /npcb stop &lt;entity&gt;
 * /npcb status [entity]
 * /npcb profile list | &lt;entity&gt; &lt;profil&gt;
 * /npcb debug [on|off]
 * /npcb trace on [entity] | off | dump
 * /npcb speedtest &lt;entity&gt; [walk|sprint]
 * /npcb chunks [reset]
 * /npcb perf [reset]
 * /npcb aitest attack [x y z]
 * /npcb stress start &lt;n&gt; [polmer] [sekunde] [rušenje-s] [x y z] | stop
 * /npcb course &lt;t1|t2&gt; build [x y z]
 * /npcb course &lt;t1|t2&gt; run &lt;entity&gt; [x y z]
 * /npcb course stop
 * </pre>
 * Brez koordinat je izhodišče tečaja pošiljateljev položaj.
 */
public class NpcbCommand extends CommandBase {

    private static final List<String> SUB = Arrays.asList("attach", "detach", "goto", "stop", "status", "profile", "debug", "trace",
            "speedtest", "chunks", "course", "perf", "stress", "aitest");

    @Override
    public String getName() {
        return "npcb";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/npcb <attach|detach|goto|stop|status|profile|debug|trace|speedtest|chunks|course|perf|stress> ...";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "attach": {
                need(args, 2, "/npcb attach <entity> [puppet] [profil]");
                EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                boolean puppet = false;
                String profile = NpcbConfig.DEFAULT_PROFILE;
                for (int i = 2; i < args.length; i++) {
                    if ("puppet".equalsIgnoreCase(args[i])) {
                        puppet = true;
                    } else {
                        profile = args[i];
                    }
                }
                if (Attach.get(e) != null) {
                    reply(sender, "že pripet: " + describe(e) + " profil=" + Attach.profile(e) + " (za menjavo /npcb profile)");
                    break;
                }
                try {
                    Attach.attach(e, puppet, NpcBaritoneMod.config(), profile);
                } catch (IllegalArgumentException ex) {
                    throw new CommandException(ex.getMessage());
                }
                reply(sender, "pripet: " + describe(e) + (puppet ? " (puppet)" : "") + " profil=" + Attach.profile(e));
                break;
            }
            case "detach": {
                need(args, 2, "/npcb detach <entity>");
                EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                reply(sender, Attach.detach(e) ? "odpet: " + describe(e) : "ni pripet: " + describe(e));
                break;
            }
            case "goto": {
                String usage = "/npcb goto <entity> <x> <y> <z> | /npcb goto <entity> <cilj-entiteta>";
                need(args, 3, usage);
                EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                BlockPos pos;
                if (args.length == 3) {
                    pos = new BlockPos(getEntity(server, sender, args[2]));
                } else {
                    need(args, 5, usage);
                    pos = parseBlockPos(sender, args, 2, false);
                }
                Baritone b = attached(e);
                Goal goal = new GoalBlock(pos);
                b.getCustomGoalProcess().setGoalAndPath(goal);
                reply(sender, describe(e) + " -> " + goal);
                break;
            }
            case "stop": {
                need(args, 2, "/npcb stop <entity>");
                EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                attached(e).getPathingBehavior().cancelEverything();
                reply(sender, "ustavljen: " + describe(e));
                break;
            }
            case "speedtest": {
                need(args, 2, "/npcb speedtest <entity> [walk|sprint]");
                EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                boolean sprint = args.length > 2 && "sprint".equalsIgnoreCase(args[2]);
                Baritone b = Attach.get(e);
                if (b == null) {
                    b = Attach.attach(e, true, NpcBaritoneMod.config());
                }
                BlockPos goal = Telemetry.INSTANCE.startSpeedTest(e, b, sender, sprint, NpcBaritoneMod.config());
                reply(sender, "speedtest " + (sprint ? "sprint" : "hoja") + " začet: " + describe(e) + " -> " + goal
                        + " (" + Telemetry.WARMUP_TICKS + " tickov ogrevanja + " + Telemetry.MEASURE_TICKS + " meritve)");
                break;
            }
            case "status": {
                if (args.length > 1) {
                    EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                    attached(e);
                    for (String line : statusDetail(e)) {
                        reply(sender, line);
                    }
                    break;
                }
                List<EntityLiving> all = Attach.attached();
                reply(sender, "pripetih: " + all.size() + ", ChunkEvent.Load od reseta: " + Telemetry.INSTANCE.chunkLoadsSinceReset()
                        + ", debug paketov: " + DebugSync.INSTANCE.packetsSent()
                        + ", trace: " + (PathTrace.INSTANCE.isRecording() ? "snema" : "ne") + " (" + PathTrace.INSTANCE.rowCount() + " vrstic)");
                for (EntityLiving e : all) {
                    NavStatus st = Attach.status(e);
                    if (st == null) {
                        continue;
                    }
                    reply(sender, " " + describe(e) + (Attach.isPuppet(e) ? " puppet" : "") + " profil=" + Attach.profile(e)
                            + " " + st.state() + (st.failReason().isEmpty() ? "" : "(" + st.failReason() + ")")
                            + " goal=" + st.goal());
                }
                break;
            }
            case "profile": {
                String usage = "/npcb profile list | /npcb profile <entity> <profil>";
                need(args, 2, usage);
                if ("list".equalsIgnoreCase(args[1])) {
                    NpcBaritoneMod.config().profiles.forEach((name, o) ->
                            reply(sender, " " + name + (o.isEmpty() ? " (NPC privzeto + config)" : " " + o)));
                    break;
                }
                need(args, 3, usage);
                EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                attached(e);
                try {
                    Attach.setProfile(e, NpcBaritoneMod.config(), args[2]);
                } catch (IllegalArgumentException ex) {
                    throw new CommandException(ex.getMessage());
                }
                reply(sender, describe(e) + " profil=" + Attach.profile(e));
                break;
            }
            case "debug": {
                if (args.length == 1) {
                    boolean mine = sender instanceof EntityPlayerMP && DebugSync.INSTANCE.isSubscribed((EntityPlayerMP) sender);
                    reply(sender, "debug prikaz: " + (mine ? "vklopljen" : "izklopljen")
                            + ", syncPathsToOps=" + NpcBaritoneMod.config().syncPathsToOps
                            + ", poslanih paketov: " + DebugSync.INSTANCE.packetsSent());
                    break;
                }
                if (!(sender instanceof EntityPlayerMP)) {
                    throw new CommandException("/npcb debug on|off je za igralca (izris je na klientu)");
                }
                EntityPlayerMP player = (EntityPlayerMP) sender;
                boolean on = parseOnOff(args[1]);
                boolean hasMod = DebugSync.INSTANCE.subscribe(player, on);
                reply(sender, "debug prikaz " + (on ? "vklopljen" : "izklopljen")
                        + (on && !hasMod ? " — klient nima moda npcbaritone, poti ne bodo poslane" : ""));
                break;
            }
            case "trace": {
                String usage = "/npcb trace on [entity] | off | dump";
                need(args, 2, usage);
                String op = args[1].toLowerCase(Locale.ROOT);
                if ("on".equals(op)) {
                    List<EntityLiving> only = args.length > 2
                            ? Collections.singletonList(getEntity(server, sender, args[2], EntityLiving.class)) : null;
                    PathTrace.INSTANCE.start(only);
                    reply(sender, "trace snema " + (only == null ? "vse pripete" : describe(only.get(0))));
                } else if ("off".equals(op)) {
                    PathTrace.INSTANCE.stop();
                    reply(sender, "trace ustavljen (" + PathTrace.INSTANCE.rowCount() + " vrstic; /npcb trace dump)");
                } else if ("dump".equals(op)) {
                    File f = PathTrace.defaultFile();
                    try {
                        int n = PathTrace.INSTANCE.dump(f);
                        reply(sender, "trace: " + n + " vrstic -> " + f.getPath()
                                + (PathTrace.INSTANCE.dropped() > 0 ? " (izpuščenih " + PathTrace.INSTANCE.dropped() + ")" : ""));
                        NpcBaritoneMod.LOG.info("NPCB-TRACE-DUMP rows={} csv={}", n, f.getAbsolutePath());
                    } catch (IOException ex) {
                        throw new CommandException("trace dump: " + ex.getMessage());
                    }
                } else {
                    throw new WrongUsageException(usage);
                }
                break;
            }
            case "chunks": {
                if (args.length > 1 && "reset".equalsIgnoreCase(args[1])) {
                    Telemetry.INSTANCE.resetChunkLoads();
                }
                reply(sender, "ChunkEvent.Load: od reseta " + Telemetry.INSTANCE.chunkLoadsSinceReset()
                        + ", skupaj " + Telemetry.INSTANCE.chunkLoadsTotal());
                break;
            }
            case "course": {
                String usage = "/npcb course <t1|t2> build [x y z] | /npcb course <t1|t2> run <entity> [x y z] | /npcb course stop";
                need(args, 2, usage);
                if ("stop".equalsIgnoreCase(args[1])) {
                    CourseRunner.INSTANCE.abort();
                    break;
                }
                need(args, 3, usage);
                Course course = course(args[1]);
                if (course == null) {
                    throw new WrongUsageException(usage);
                }
                if ("build".equalsIgnoreCase(args[2])) {
                    BlockPos origin = args.length >= 6 ? parseBlockPos(sender, args, 3, false) : sender.getPosition();
                    int n = course.build(sender.getEntityWorld(), origin);
                    reply(sender, course.id() + " postavljen pri " + origin + " (" + n + " blokov)");
                } else if ("run".equalsIgnoreCase(args[2])) {
                    need(args, 4, usage);
                    EntityLiving e = getEntity(server, sender, args[3], EntityLiving.class);
                    BlockPos origin = args.length >= 7 ? parseBlockPos(sender, args, 4, false) : sender.getPosition();
                    if (CourseRunner.INSTANCE.isRunning()) {
                        throw new CommandException("Tečaj že teče (/npcb course stop)");
                    }
                    int n = course.build(e.world, origin);
                    Baritone b = Attach.get(e);
                    if (b == null) {
                        b = Attach.attach(e, true, NpcBaritoneMod.config());
                    }
                    Telemetry.INSTANCE.resetChunkLoads();
                    CourseRunner.INSTANCE.start(e, b, sender, course, origin);
                    reply(sender, course.id() + " teče pri " + origin + " (postavljenih " + n + " blokov)");
                } else {
                    throw new WrongUsageException(usage);
                }
                break;
            }
            case "aitest": {
                String usage = "/npcb aitest attack [x y z]";
                need(args, 2, usage);
                if (!"attack".equalsIgnoreCase(args[1]) || !(sender.getEntityWorld() instanceof net.minecraft.world.WorldServer)) {
                    throw new WrongUsageException(usage);
                }
                if (AiTestRunner.INSTANCE.isRunning()) {
                    throw new CommandException("aitest že teče");
                }
                BlockPos origin = args.length >= 5 ? parseBlockPos(sender, args, 2, false) : sender.getPosition();
                AiTestRunner.INSTANCE.startAttack((net.minecraft.world.WorldServer) sender.getEntityWorld(), sender, origin);
                break;
            }
            case "perf": {
                if (args.length > 1 && "reset".equalsIgnoreCase(args[1])) {
                    si.ladja.npcbaritone.core.SearchStats.reset();
                    PerfMeter.INSTANCE.reset();
                }
                reply(sender, si.ladja.npcbaritone.core.SearchStats.summary());
                reply(sender, PerfMeter.INSTANCE.summary());
                NpcBaritoneMod.LOG.info("NPCB-PERF {} | {}", si.ladja.npcbaritone.core.SearchStats.summary(), PerfMeter.INSTANCE.summary());
                break;
            }
            case "stress": {
                String usage = "/npcb stress start <n> [polmer=48] [sekunde=120] [rušenje-s=5] [x y z] | /npcb stress stop";
                need(args, 2, usage);
                if ("stop".equalsIgnoreCase(args[1])) {
                    StressRunner.INSTANCE.abort("ukaz");
                    break;
                }
                need(args, 3, usage);
                if (!"start".equalsIgnoreCase(args[1])) {
                    throw new WrongUsageException(usage);
                }
                if (StressRunner.INSTANCE.isRunning() || CourseRunner.INSTANCE.isRunning()) {
                    throw new CommandException("Stres ali tečaj že teče");
                }
                int n = parseInt(args[2], 1, 1000);
                int radius = args.length > 3 ? parseInt(args[3], 8, 128) : 48;
                int seconds = args.length > 4 ? parseInt(args[4], 5, 7200) : 120;
                int breakEvery = args.length > 5 ? parseInt(args[5], 0, 600) : 5;
                BlockPos origin = args.length >= 9 ? parseBlockPos(sender, args, 6, false) : sender.getPosition();
                if (!(sender.getEntityWorld() instanceof net.minecraft.world.WorldServer)) {
                    throw new CommandException("samo na strežniku");
                }
                int spawned = StressRunner.INSTANCE.start((net.minecraft.world.WorldServer) sender.getEntityWorld(), sender, origin,
                        n, radius, seconds, breakEvery);
                reply(sender, "T4: " + spawned + "/" + n + " NPC-jev pri " + origin);
                break;
            }
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUB);
        }
        if (args.length >= 3 && args.length <= 4 && "attach".equalsIgnoreCase(args[0])) {
            List<String> opts = new java.util.ArrayList<>(NpcBaritoneMod.config().profiles.keySet());
            opts.add(0, "puppet");
            return getListOfStringsMatchingLastWord(args, opts);
        }
        if (args.length == 2 && "profile".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "list");
        }
        if (args.length == 3 && "profile".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, NpcBaritoneMod.config().profiles.keySet());
        }
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "on", "off");
        }
        if (args.length == 2 && "perf".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "reset");
        }
        if (args.length == 2 && "stress".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "start", "stop");
        }
        if (args.length == 2 && "trace".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "on", "off", "dump");
        }
        if (args.length == 2 && "course".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "t1", "t2", "stop");
        }
        if (args.length == 3 && "course".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "build", "run");
        }
        if (args.length == 3 && "speedtest".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "walk", "sprint");
        }
        if (args.length >= 3 && args.length <= 5 && "goto".equalsIgnoreCase(args[0])) {
            return getTabCompletionCoordinate(args, 2, targetPos);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "course":
                return index == 3;
            case "goto":
                return index == 1 || (index == 2 && args.length == 3);
            case "trace":
                return index == 2;
            case "profile":
            case "attach":
            case "detach":
            case "stop":
            case "status":
            case "speedtest":
                return index == 1;
            default:
                return false;
        }
    }

    private static Baritone attached(EntityLiving e) throws CommandException {
        Baritone b = Attach.get(e);
        if (b == null) {
            throw new CommandException("Entiteta ni pripeta; najprej /npcb attach");
        }
        return b;
    }

    static Course course(String id) {
        switch (id.toLowerCase(Locale.ROOT)) {
            case "t1":
                return CourseT1.INSTANCE;
            case "t2":
                return CourseT2.INSTANCE;
            default:
                return null;
        }
    }

    static List<String> statusDetail(EntityLiving e) {
        Baritone b = Attach.get(e);
        NavStatus st = Attach.status(e);
        List<String> out = new java.util.ArrayList<>();
        out.add(describe(e) + (Attach.isPuppet(e) ? " puppet" : "") + " profil=" + Attach.profile(e));
        out.add(" stanje=" + st.state() + (st.failReason().isEmpty() ? "" : " razlog=" + st.failReason())
                + " zadnji dogodek=" + st.lastEvent());
        out.add(" cilj=" + st.goal());
        si.ladja.npcbaritone.core.pathing.path.PathExecutor cur = b.getPathingBehavior().getCurrent();
        if (cur != null) {
            int pos = cur.getPosition();
            java.util.List<si.ladja.npcbaritone.core.api.pathing.movement.IMovement> mv = cur.getPath().movements();
            out.add(" pot: " + pos + "/" + cur.getPath().length() + " premik="
                    + (pos >= 0 && pos < mv.size() ? mv.get(pos).getClass().getSimpleName() : "-")
                    + (b.getPathingBehavior().getNext() != null ? " +naslednji segment" : ""));
        }
        out.add(String.format(Locale.ROOT, " iskanj=%d ponovnih=%d neuspehov=%d zadnje=%d µs (%s)",
                b.getPathingBehavior().searchesStarted(), st.replans(), st.failures(), st.lastSearchMicros(),
                b.getPathingBehavior().lastSearchResult()));
        return out;
    }

    static boolean parseOnOff(String v) throws WrongUsageException {
        if ("on".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v)) {
            return true;
        }
        if ("off".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) {
            return false;
        }
        throw new WrongUsageException("on|off");
    }

    private static void need(String[] args, int n, String usage) throws WrongUsageException {
        if (args.length < n) {
            throw new WrongUsageException(usage);
        }
    }

    private static String describe(EntityLiving e) {
        return e.getName() + "#" + e.getEntityId() + " @" + new BlockPos(e);
    }

    private static void reply(ICommandSender sender, String msg) {
        sender.sendMessage(new TextComponentString("[npcb] " + msg));
    }
}
