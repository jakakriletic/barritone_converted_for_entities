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
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * M2.8: minimalni ukazi za razvoj in teste (OP 2). Entiteta je izbirnik ali UUID
 * (npr. {@code @e[type=zombie,c=1]}).
 * <pre>
 * /npcb attach &lt;entity&gt; [puppet]
 * /npcb detach &lt;entity&gt;
 * /npcb goto &lt;entity&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt;
 * /npcb stop &lt;entity&gt;
 * /npcb speedtest &lt;entity&gt; [walk|sprint]
 * /npcb status
 * /npcb chunks [reset]
 * /npcb course t1 build [x y z]
 * /npcb course t1 run &lt;entity&gt; [x y z]
 * /npcb course stop
 * </pre>
 * Brez koordinat je izhodišče tečaja pošiljateljev položaj.
 */
public class NpcbCommand extends CommandBase {

    private static final List<String> SUB = Arrays.asList("attach", "detach", "goto", "stop", "speedtest", "status", "chunks", "course");

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
        return "/npcb <attach|detach|goto|stop|speedtest|status|chunks|course> ...";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException(getUsage(sender));
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "attach": {
                need(args, 2, "/npcb attach <entity> [puppet]");
                EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                boolean puppet = args.length > 2 && "puppet".equalsIgnoreCase(args[2]);
                Attach.attach(e, puppet, NpcBaritoneMod.config());
                reply(sender, "pripet: " + describe(e) + (puppet ? " (puppet)" : ""));
                break;
            }
            case "detach": {
                need(args, 2, "/npcb detach <entity>");
                EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                reply(sender, Attach.detach(e) ? "odpet: " + describe(e) : "ni pripet: " + describe(e));
                break;
            }
            case "goto": {
                need(args, 5, "/npcb goto <entity> <x> <y> <z>");
                EntityLiving e = getEntity(server, sender, args[1], EntityLiving.class);
                BlockPos pos = parseBlockPos(sender, args, 2, false);
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
                List<EntityLiving> all = Attach.attached();
                reply(sender, "pripetih: " + all.size() + ", ChunkEvent.Load od reseta: " + Telemetry.INSTANCE.chunkLoadsSinceReset());
                for (EntityLiving e : all) {
                    Baritone b = Attach.get(e);
                    if (b == null) {
                        continue;
                    }
                    reply(sender, " " + describe(e) + (Attach.isPuppet(e) ? " puppet" : "")
                            + " goal=" + b.getPathingBehavior().getGoal()
                            + " pathing=" + b.getPathingBehavior().isPathing()
                            + " searching=" + b.getPathingBehavior().getInProgress().isPresent());
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
                String usage = "/npcb course t1 build [x y z] | /npcb course t1 run <entity> [x y z] | /npcb course stop";
                need(args, 2, usage);
                if ("stop".equalsIgnoreCase(args[1])) {
                    CourseRunner.INSTANCE.abort();
                    break;
                }
                need(args, 3, usage);
                if (!"t1".equalsIgnoreCase(args[1])) {
                    throw new WrongUsageException(usage);
                }
                if ("build".equalsIgnoreCase(args[2])) {
                    BlockPos origin = args.length >= 6 ? parseBlockPos(sender, args, 3, false) : sender.getPosition();
                    int n = CourseT1.build(sender.getEntityWorld(), origin);
                    reply(sender, "T1 postavljen pri " + origin + " (" + n + " blokov)");
                } else if ("run".equalsIgnoreCase(args[2])) {
                    need(args, 4, usage);
                    EntityLiving e = getEntity(server, sender, args[3], EntityLiving.class);
                    BlockPos origin = args.length >= 7 ? parseBlockPos(sender, args, 4, false) : sender.getPosition();
                    if (CourseRunner.INSTANCE.isRunning()) {
                        throw new CommandException("Tečaj že teče (/npcb course stop)");
                    }
                    int n = CourseT1.build(e.world, origin);
                    Baritone b = Attach.get(e);
                    if (b == null) {
                        b = Attach.attach(e, true, NpcBaritoneMod.config());
                    }
                    Telemetry.INSTANCE.resetChunkLoads();
                    CourseRunner.INSTANCE.start(e, b, sender, origin);
                    reply(sender, "T1 teče pri " + origin + " (postavljenih " + n + " blokov)");
                } else {
                    throw new WrongUsageException(usage);
                }
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
        if (args.length == 3 && "attach".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "puppet");
        }
        if (args.length == 2 && "course".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "t1", "stop");
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
        return args.length > 0 && "course".equalsIgnoreCase(args[0]) ? index == 3 : index == 1;
    }

    private static Baritone attached(EntityLiving e) throws CommandException {
        Baritone b = Attach.get(e);
        if (b == null) {
            throw new CommandException("Entiteta ni pripeta; najprej /npcb attach");
        }
        return b;
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
