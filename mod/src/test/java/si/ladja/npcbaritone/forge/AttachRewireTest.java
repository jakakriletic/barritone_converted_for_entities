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

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.harness.BootstrapOnce;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * M6 (najdeno 2026-09-26 ob pripravi {@code /npcb selftest}): vanilla taski
 * {@code EntityAIFollowOwner}, {@code EntityAIAvoidEntity} in {@code EntityAIFollow} si
 * navigator shranijo v konstruktorju. Brez preusmeritve po {@code attach} ukazujejo staremu
 * vanilla navigatorju, ki ga nihče več ne tiktaka — ukročen volk bi stal.
 */
public class AttachRewireTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    /** Kot EntityAIFollowOwner: navigator v private final polju. */
    static class CachingTask extends EntityAIBase {
        private final PathNavigate nav;

        CachingTask(PathNavigate nav) {
            this.nav = nav;
        }

        @Override
        public boolean shouldExecute() {
            return false;
        }

        PathNavigate nav() {
            return nav;
        }
    }

    /** Podrazred z drugim navigatorjem (npr. modded task) in poljem ožjega tipa. */
    static class SubTask extends CachingTask {
        final PathNavigate unrelated;
        final PathNavigateSwimmer narrow;

        SubTask(PathNavigate nav, PathNavigate unrelated, PathNavigateSwimmer narrow) {
            super(nav);
            this.unrelated = unrelated;
            this.narrow = narrow;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (T) unsafeClass.getMethod("allocateInstance", Class.class).invoke(f.get(null), type);
    }

    @Test
    public void cachedNavigatorsFollowAttachAndDetach() throws Exception {
        PathNavigate vanilla = allocate(PathNavigateGround.class);
        PathNavigate ours = allocate(PathNavigateGround.class);
        PathNavigate other = allocate(PathNavigateGround.class);
        PathNavigateSwimmer swimmer = allocate(PathNavigateSwimmer.class);
        CachingTask follow = new CachingTask(vanilla);
        SubTask modded = new SubTask(vanilla, other, swimmer);
        CachingTask foreign = new CachingTask(other);

        assertEquals(2, Attach.rewireNavigators(Arrays.asList(follow, modded, foreign), vanilla, ours));
        assertSame(ours, follow.nav());
        assertSame(ours, modded.nav());
        assertSame(other, modded.unrelated);
        assertSame(swimmer, modded.narrow);
        assertSame(other, foreign.nav());

        assertEquals(2, Attach.rewireNavigators(Arrays.asList(follow, modded, foreign), ours, vanilla));
        assertSame(vanilla, follow.nav());
        assertSame(vanilla, modded.nav());
    }

    /** Polje ožjega tipa, ki našega navigatorja ne more sprejeti, ostane nespremenjeno. */
    @Test
    public void fieldOfIncompatibleTypeIsLeftAlone() throws Exception {
        PathNavigateSwimmer vanilla = allocate(PathNavigateSwimmer.class);
        PathNavigate ours = allocate(PathNavigateGround.class);
        SubTask task = new SubTask(allocate(PathNavigateGround.class), allocate(PathNavigateGround.class), vanilla);
        assertEquals(0, Attach.rewireNavigators(Arrays.asList(task), vanilla, ours));
        assertSame(vanilla, task.narrow);
    }
}
