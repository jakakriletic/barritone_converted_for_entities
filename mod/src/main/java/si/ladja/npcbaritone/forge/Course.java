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
import net.minecraft.util.math.BlockPos;

/** Skupni tipi tečajev (T1, T2, …): odsek, pričakovan izid, cilj postavljanja, postavljalec. */
public interface Course {

    enum Expect { REACH, FAIL }

    final class Segment {
        public final int index;
        public final String name;
        public final BlockPos start;
        public final BlockPos goal;
        public final Expect expect;
        /** Največja dovoljena padalna razdalja (bloki); NaN = ni preverjeno. */
        public final double maxFall;
        /** Blok (x, z), skozi katerega entiteta ne sme iti; null = ni preverjeno. */
        public final BlockPos forbiddenColumn;
        /** Začetni yaw (−90 = proti +X). */
        public final float startYaw;

        public Segment(int index, String name, BlockPos start, BlockPos goal, Expect expect, double maxFall, BlockPos forbiddenColumn, float startYaw) {
            this.index = index;
            this.name = name;
            this.start = start;
            this.goal = goal;
            this.expect = expect;
            this.maxFall = maxFall;
            this.forbiddenColumn = forbiddenColumn;
            this.startYaw = startYaw;
        }
    }

    /** Cilj postavljanja: svet v igri ali sintetični svet v testih. */
    interface Sink {
        IBlockState get(BlockPos pos);

        void set(BlockPos pos, IBlockState state);
    }


    /** Postavljalec: šteje spremenjene bloke. */
    final class Builder {
        final Sink w;
        public int changed;

        public Builder(Sink w) {
            this.w = w;
        }

        public void set(BlockPos p, IBlockState s) {
            if (w.get(p) != s) {
                w.set(p, s);
                changed++;
            }
        }

        public void fill(BlockPos a, BlockPos c, IBlockState s) {
            for (BlockPos p : BlockPos.getAllInBoxMutable(a, c)) {
                set(p.toImmutable(), s);
            }
        }
    }
}
