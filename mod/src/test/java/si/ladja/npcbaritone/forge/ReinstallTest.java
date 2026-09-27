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
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityJumpHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.profiler.Profiler;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.harness.BootstrapOnce;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * D-039 (CNPC U2): {@code EntityNPCInterface.updateTasks()} ob vsaki posodobitvi AI ustvari nov
 * navigator in move helper ter nove taske. Brez ponovne namestitve bi NPC tiho padel na vanilla
 * in taski bi ukazovali navigatorju, ki ga Baritone ne vodi.
 */
public class ReinstallTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    @SuppressWarnings("unchecked")
    static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (T) unsafeClass.getMethod("allocateInstance", Class.class).invoke(f.get(null), type);
    }

    /** Zombi brez sveta: samo šivi in vrsti taskov (dovolj za {@link Attach#reinstallSeams}). */
    private static EntityZombie entity(Attach.Seams seams) throws Exception {
        EntityZombie z = allocate(EntityZombie.class);
        setFinal(EntityLiving.class, "tasks", z, new EntityAITasks(new Profiler()));
        setFinal(EntityLiving.class, "targetTasks", z, new EntityAITasks(new Profiler()));
        put(z, seams);
        return z;
    }

    private static void put(EntityLiving e, Attach.Seams s) throws Exception {
        setFinal(EntityLiving.class, Attach.NAVIGATOR[1], e, s.navigator);
        setFinal(EntityLiving.class, Attach.MOVE_HELPER[1], e, s.moveHelper);
        setFinal(EntityLiving.class, Attach.JUMP_HELPER[1], e, s.jumpHelper);
    }

    private static void setFinal(Class<?> owner, String name, Object target, Object value) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Attach.Seams seams(Class<? extends PathNavigate> nav, Class<? extends EntityMoveHelper> move,
                                      Class<? extends EntityJumpHelper> jump) throws Exception {
        return new Attach.Seams(allocate(nav), allocate(move), allocate(jump));
    }

    @Test
    public void updateTasksReplacementIsUndone() throws Exception {
        Attach.Seams vanilla = seams(PathNavigateGround.class, EntityMoveHelper.class, EntityJumpHelper.class);
        Attach.Seams ours = seams(BaritonePathNavigate.class, BaritoneMoveHelper.class, BaritoneJumpHelper.class);
        PathNavigate v1 = vanilla.navigator;
        EntityJumpHelper j1 = vanilla.jumpHelper;
        EntityZombie npc = entity(ours);

        // CNPC updateTasks(): nov vanilla navigator in move helper, novi taski
        PathNavigate v2 = allocate(PathNavigateGround.class);
        EntityMoveHelper m2 = allocate(EntityMoveHelper.class);
        npc.tasks.taskEntries.clear();
        AttachRewireTest.CachingTask fresh = new AttachRewireTest.CachingTask(v2);
        AttachRewireTest.CachingTask stale = new AttachRewireTest.CachingTask(v1);
        AttachRewireTest.CachingTask early = new AttachRewireTest.CachingTask(ours.navigator);
        npc.tasks.addTask(1, fresh);
        npc.tasks.addTask(2, stale);
        npc.targetTasks.addTask(1, early);
        setFinal(EntityLiving.class, Attach.NAVIGATOR[1], npc, v2);
        setFinal(EntityLiving.class, Attach.MOVE_HELPER[1], npc, m2);

        assertEquals("nav + fresh + stale + move", 4, Attach.reinstallSeams(npc, vanilla, ours));
        assertSame(ours.navigator, npc.getNavigator());
        assertSame(ours.moveHelper, npc.getMoveHelper());
        assertSame(ours.jumpHelper, npc.getJumpHelper());
        assertSame(ours.navigator, fresh.nav());
        assertSame(ours.navigator, stale.nav());
        assertSame(ours.navigator, early.nav());
        assertSame("detach vrne najnovejši vanilla navigator", v2, vanilla.navigator);
        assertSame(m2, vanilla.moveHelper);
        assertSame("jump helperja CNPC ni zamenjal", j1, vanilla.jumpHelper);

        assertEquals("idempotentno", 0, Attach.reinstallSeams(npc, vanilla, ours));
    }

    /** Detach brez reinstall: porabnikov najnovejši šiv ostane, stari vanilla se ne vrne. */
    @Test
    public void detachAdoptsSeamReplacedByConsumer() throws Exception {
        Attach.Seams vanilla = seams(PathNavigateGround.class, EntityMoveHelper.class, EntityJumpHelper.class);
        Attach.Seams ours = seams(BaritonePathNavigate.class, BaritoneMoveHelper.class, BaritoneJumpHelper.class);
        EntityMoveHelper m1 = vanilla.moveHelper;
        EntityZombie npc = entity(ours);
        PathNavigate v3 = allocate(PathNavigateGround.class);
        setFinal(EntityLiving.class, Attach.NAVIGATOR[1], npc, v3);

        Attach.adoptReplaced(npc, vanilla, ours);
        assertSame(v3, vanilla.navigator);
        assertSame(m1, vanilla.moveHelper);
    }
}
