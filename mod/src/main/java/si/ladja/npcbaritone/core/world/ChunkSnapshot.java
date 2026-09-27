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

package si.ladja.npcbaritone.core.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/**
 * D-013: omejena kopija mape naloženih chunkov, narejena na strežniški niti ob začetku
 * iskanja. Kopira se samo mapa (reference na žive chunke), ne vsebina; iskalna nit bere
 * {@code ExtendedBlockStorage} teh chunkov. Nobena metoda ne naloži chunka (D-012).
 */
public final class ChunkSnapshot {

    private ChunkSnapshot() {
    }

    /** Pravokotnik v chunk koordinatah (vključno z mejami). */
    public static final class Bounds {

        /** Brez omejitve: kopija celotne mape (kot Baritone {@code copyLoadedChunks}). */
        public static final Bounds ALL = new Bounds(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

        public final int minX, minZ, maxX, maxZ;

        public Bounds(int minX, int minZ, int maxX, int maxZ) {
            this.minX = Math.min(minX, maxX);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxZ = Math.max(minZ, maxZ);
        }

        /** Chunki, ki pokrivajo bloka a in b, razširjeni za {@code marginChunks} v vse smeri. */
        public static Bounds around(int ax, int az, int bx, int bz, int marginChunks) {
            int m = Math.max(0, marginChunks);
            return new Bounds((Math.min(ax, bx) >> 4) - m, (Math.min(az, bz) >> 4) - m,
                    (Math.max(ax, bx) >> 4) + m, (Math.max(az, bz) >> 4) + m);
        }

        /**
         * D-041: chunki, ki pokrivajo kvadrat {@code x ± blocks, z ± blocks} (vanilla
         * {@code PathNavigate} vidi {@code ChunkCache} entiteta ± (doseg + 8)).
         */
        public static Bounds radius(int x, int z, int blocks) {
            int b = Math.max(0, blocks);
            return new Bounds((x - b) >> 4, (z - b) >> 4, (x + b) >> 4, (z + b) >> 4);
        }

        /** Presek; {@code ALL} je nevtralen element. Prazen presek ni mogoč (oba vsebujeta začetek). */
        public Bounds intersect(Bounds o) {
            if (o.isAll()) {
                return this;
            }
            if (isAll()) {
                return o;
            }
            return new Bounds(Math.max(minX, o.minX), Math.max(minZ, o.minZ), Math.min(maxX, o.maxX), Math.min(maxZ, o.maxZ));
        }

        public boolean isAll() {
            return this == ALL;
        }

        public boolean contains(int chunkX, int chunkZ) {
            return chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
        }

        /** Število chunkov v pravokotniku (long, da ALL ne preliva). */
        public long area() {
            return ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1);
        }

        @Override
        public String toString() {
            return isAll() ? "Bounds[ALL]" : "Bounds[" + minX + "," + minZ + " .. " + maxX + "," + maxZ + "]";
        }
    }

    /**
     * Kopija tistih vnosov {@code live}, ki so v {@code bounds}. Če je pravokotnik manjši od
     * mape, se iterira pravokotnik ({@code get}), sicer mapa — cena je vedno
     * O(min(površina, naloženi)).
     */
    public static Long2ObjectMap<Chunk> copy(Long2ObjectMap<Chunk> live, Bounds bounds) {
        if (bounds.isAll()) {
            return new Long2ObjectOpenHashMap<>(live);
        }
        long area = bounds.area();
        Long2ObjectOpenHashMap<Chunk> out = new Long2ObjectOpenHashMap<>((int) Math.min(area, live.size()) + 1);
        if (area <= live.size()) {
            for (int x = bounds.minX; x <= bounds.maxX; x++) {
                for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
                    long key = ChunkPos.asLong(x, z);
                    Chunk c = live.get(key);
                    if (c != null) {
                        out.put(key, c);
                    }
                }
            }
        } else {
            for (Long2ObjectMap.Entry<Chunk> e : live.long2ObjectEntrySet()) {
                Chunk c = e.getValue();
                if (c != null && bounds.contains(c.x, c.z)) {
                    out.put(e.getLongKey(), c);
                }
            }
        }
        return out;
    }
}
