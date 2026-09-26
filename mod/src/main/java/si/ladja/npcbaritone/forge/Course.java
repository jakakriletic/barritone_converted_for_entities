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
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Skupni tipi tečajev (T1, T2, …): odsek, pričakovan izid, cilj postavljanja, postavljalec. */
public interface Course {

    /** Ime v ukazu in CSV ({@code T1}, {@code T2}). */
    String id();

    List<Segment> segments(BlockPos origin);

    /** Postavi tečaj; vrne število spremenjenih blokov. */
    int build(Sink sink, BlockPos origin);

    /** Najmanjši in največji kot območja, ki ga tečaj postavi (za podpis blokov, M4.9). */
    BlockPos[] bounds(BlockPos origin);

    /**
     * M8.9: ali tekač med tekom prisili nalaganje chunkov v {@link #bounds} (strežnik brez igralca).
     * Od 2026-09-27 za vse tečaje: na dedicated strežniku brez igralca se chunki izven spawn
     * območja med tekom razložijo; BSI razloženi chunk bere kot zrak, zato je T2/8 in T2/9
     * (2026-09-27 00:11) izvajalec prekinil ("future movement impossible") in novo iskanje ni
     * našlo cilja — odvisno od časa, 2026-09-25 je isti tek uspel.
     */
    default boolean forceChunks() {
        return true;
    }

    /** Postavi v svet (flag 2: brez posodobitev sosedov). */
    default int build(World w, BlockPos origin) {
        return build(new Sink() {
            @Override
            public IBlockState get(BlockPos pos) {
                return w.getBlockState(pos);
            }

            @Override
            public void set(BlockPos pos, IBlockState state) {
                w.setBlockState(pos, state, 2);
            }
        }, origin);
    }

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
        /** Vrata/ograjna vrata (spodnja polovica), ki morajo biti po odseku zaprta (M4 A4). */
        public final List<BlockPos> openables;
        /** M8.9: CNPC velikost (1–10), na katero se entiteta nastavi pred odsekom; 0 = brez spremembe. */
        public final int npcSize;

        public Segment(int index, String name, BlockPos start, BlockPos goal, Expect expect, double maxFall, BlockPos forbiddenColumn, float startYaw) {
            this.index = index;
            this.name = name;
            this.start = start;
            this.goal = goal;
            this.expect = expect;
            this.maxFall = maxFall;
            this.forbiddenColumn = forbiddenColumn;
            this.startYaw = startYaw;
            this.openables = Collections.emptyList();
            this.npcSize = 0;
        }

        private Segment(Segment s, List<BlockPos> openables) {
            this.index = s.index;
            this.name = s.name;
            this.start = s.start;
            this.goal = s.goal;
            this.expect = s.expect;
            this.maxFall = s.maxFall;
            this.forbiddenColumn = s.forbiddenColumn;
            this.startYaw = s.startYaw;
            this.openables = Collections.unmodifiableList(openables);
            this.npcSize = s.npcSize;
        }

        /** M8.9: odsek osnovnega tečaja za dano velikost (T3). */
        private Segment(Segment s, int index, String name, Expect expect, BlockPos forbiddenColumn, int npcSize) {
            this.index = index;
            this.name = name;
            this.start = s.start;
            this.goal = s.goal;
            this.expect = expect;
            this.maxFall = s.maxFall;
            this.forbiddenColumn = forbiddenColumn;
            this.startYaw = s.startYaw;
            this.openables = s.openables;
            this.npcSize = npcSize;
        }

        public Segment withOpenables(BlockPos... positions) {
            return new Segment(this, Arrays.asList(positions));
        }

        public Segment forSize(int index, String name, Expect expect, BlockPos forbiddenColumn, int npcSize) {
            return new Segment(this, index, name, expect, forbiddenColumn, npcSize);
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
