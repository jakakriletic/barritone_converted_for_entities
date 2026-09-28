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

package si.ladja.npcbaritone.api.work;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * API 3 (D-033): delovno območje workerja — unija kvadrov v blokovnih koordinatah, <b>oba konca
 * vključena</b>. Worker izven območja ne ruši in ne postavlja (preverba pri iskanju poti in ob
 * izvedbi). Nespremenljivo; varno za iskalne niti.
 */
public final class WorkArea {

    /** Kvader {@code [minX..maxX] × [minY..maxY] × [minZ..maxZ]}. */
    public static final class Box {
        public final int minX, minY, minZ, maxX, maxY, maxZ;

        Box(BlockPos a, BlockPos b) {
            minX = Math.min(a.getX(), b.getX());
            minY = Math.min(a.getY(), b.getY());
            minZ = Math.min(a.getZ(), b.getZ());
            maxX = Math.max(a.getX(), b.getX());
            maxY = Math.max(a.getY(), b.getY());
            maxZ = Math.max(a.getZ(), b.getZ());
        }

        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        @Override
        public String toString() {
            return "[" + minX + "," + minY + "," + minZ + " .. " + maxX + "," + maxY + "," + maxZ + "]";
        }
    }

    private final List<Box> boxes;

    private WorkArea(List<Box> boxes) {
        this.boxes = Collections.unmodifiableList(boxes);
    }

    /** Kvader med dvema kotoma (vrstni red kotov ni pomemben), oba vključena. */
    public static WorkArea box(BlockPos a, BlockPos b) {
        List<Box> l = new ArrayList<>(1);
        l.add(new Box(a, b));
        return new WorkArea(l);
    }

    public WorkArea union(WorkArea other) {
        List<Box> l = new ArrayList<>(boxes);
        l.addAll(other.boxes);
        return new WorkArea(l);
    }

    public boolean contains(BlockPos pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean contains(int x, int y, int z) {
        for (Box b : boxes) {
            if (b.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public List<Box> boxes() {
        return boxes;
    }

    @Override
    public String toString() {
        return "WorkArea" + boxes;
    }
}
