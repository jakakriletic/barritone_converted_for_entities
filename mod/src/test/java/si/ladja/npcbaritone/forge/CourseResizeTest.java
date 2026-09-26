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

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.util.math.AxisAlignedBB;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.pathing.movement.EntitySize;
import si.ladja.npcbaritone.harness.BootstrapOnce;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

/**
 * M8.9: {@link CourseRunner#resize} mora velikost res uveljaviti tudi pri zombijih.
 * {@code EntityZombie.setSize} si po prvem klicu velikost samo zapomni ({@code zombieWidth},
 * {@code zombieHeight}); uveljavi jo šele {@code multiplySize}. Prvi tek T3 (2026-09-26) je
 * zato vseh 100 odsekov prevozil s huskom 0,60 × 1,95.
 */
public class CourseResizeTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    /** Husk kot po konstruktorju (velikost 0,6 × 1,95 že nastavljena), brez sveta. */
    private static EntityHusk husk() throws Exception {
        // sun.misc.Unsafe prek refleksije: javac --release 8 paketa sun.misc ne vidi
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        EntityHusk husk = (EntityHusk) unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, EntityHusk.class);
        Field firstUpdate = Entity.class.getDeclaredField("firstUpdate");
        firstUpdate.setAccessible(true);
        firstUpdate.setBoolean(husk, true); // brez sveta: Entity.setSize ob rasti ne kliče move()
        husk.setEntityBoundingBox(new AxisAlignedBB(0, 0, 0, 0, 0, 0));
        CourseRunner.resize(husk, 0.6F, 1.95F); // kot konstruktor EntityZombie
        assertEquals(0.6F, husk.width, 1e-6);
        assertEquals(1.95F, husk.height, 1e-6);
        return husk;
    }

    @Test
    public void resizeAppliesToZombiesForEveryCnpcSize() throws Exception {
        EntityHusk husk = husk();
        for (int size : new int[]{1, 3, 5, 7, 10, 5, 1}) {
            EntitySize s = CourseT3.npcSize(size);
            CourseRunner.resize(husk, s.width, s.height);
            assertEquals("width, size " + size, s.width, husk.width, 1e-6);
            assertEquals("height, size " + size, s.height, husk.height, 1e-6);
            AxisAlignedBB bb = husk.getEntityBoundingBox();
            assertEquals("box height, size " + size, s.height, bb.maxY - bb.minY, 1e-5);
        }
    }
}
