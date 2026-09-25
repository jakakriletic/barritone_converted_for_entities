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

import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tečaj T1 — osnovno gibanje (03-FAZE, M2.9). Deset odsekov v vzporednih progah vzdolž +X,
 * razmik 12 blokov po Z. Vsaka proga: tla iz kamna na {@code y-1}, zrak {@code y..y+6},
 * širina z ±5, dolžina x −6..+46 od izhodišča proge. Postavi se z {@code setBlockState}
 * (flag 2, brez posodobitev sosedov), zato mora biti območje naloženo (spawn chunki).
 *
 * <p>Razlaga odsekov, kjer načrt ni enoznačen, je zapisana pri odseku.
 */
public final class CourseT1 {

    public static final int LANE_SPACING = 12;

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState TOP_SLAB = Blocks.STONE_SLAB.getDefaultState()
            .withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.STONE)
            .withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP);

    private CourseT1() {
    }

    /** Izhodišče proge i (noge entitete na startu). */
    public static BlockPos lane(BlockPos origin, int i) {
        return origin.add(0, 0, i * LANE_SPACING);
    }

    public static List<Course.Segment> segments(BlockPos o) {
        List<Course.Segment> s = new ArrayList<>();
        BlockPos l;
        l = lane(o, 0);
        s.add(new Course.Segment(1, "ravnina 30", l, l.add(30, 0, 0), Course.Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 1);
        s.add(new Course.Segment(2, "zid z režo", l, l.add(20, 0, 0), Course.Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 2);
        s.add(new Course.Segment(3, "stopnice gor 1/2/3", l, l.add(15, 3, 0), Course.Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 3);
        s.add(new Course.Segment(4, "padec 2/3/4 (4 mora zaviti)", l.add(3, 4, 0), l.add(12, 0, 0), Course.Expect.REACH, 3.5, null, -90));
        l = lane(o, 4);
        s.add(new Course.Segment(5, "diagonala ob stebrih", l, l.add(5, 0, 5), Course.Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 5);
        s.add(new Course.Segment(6, "ozek hodnik 1x2", l, l.add(11, 0, 0), Course.Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 6);
        s.add(new Course.Segment(7, "reža 1,5 pod ploščo (obhod)", l, l.add(20, 0, 0), Course.Expect.REACH, Double.NaN, l.add(10, 0, 0), -90));
        l = lane(o, 7);
        s.add(new Course.Segment(8, "obrat 180", l.add(20, 0, 0), l.add(8, 0, 0), Course.Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 8);
        s.add(new Course.Segment(9, "cilj za vogalom", l, l.add(10, 0, -2), Course.Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 9);
        s.add(new Course.Segment(10, "nedosegljiv cilj (FAILED)", l, l.add(15, 0, 0), Course.Expect.FAIL, Double.NaN, null, -90));
        return Collections.unmodifiableList(s);
    }

    /** Postavi vse proge v svet (flag 2: brez posodobitev sosedov). */
    public static int build(World w, BlockPos o) {
        return build(new Course.Sink() {
            @Override
            public IBlockState get(BlockPos pos) {
                return w.getBlockState(pos);
            }

            @Override
            public void set(BlockPos pos, IBlockState state) {
                w.setBlockState(pos, state, 2);
            }
        }, o);
    }

    /** Postavi vse proge. Vrne število spremenjenih blokov. */
    public static int build(Course.Sink w, BlockPos o) {
        Course.Builder b = new Course.Builder(w);
        for (int i = 0; i < 10; i++) {
            BlockPos l = lane(o, i);
            b.fill(l.add(-6, 0, -5), l.add(46, 6, 5), AIR);
            b.fill(l.add(-6, -1, -5), l.add(46, -1, 5), STONE);
        }
        BlockPos l;
        // 2: zid x=10, z -5..5, višina 3; reža širine 1 na z=+3
        l = lane(o, 1);
        b.fill(l.add(10, 0, -5), l.add(10, 2, 5), STONE);
        b.fill(l.add(10, 0, 3), l.add(10, 1, 3), AIR);
        // 3: stopnice po 1 blok: x>=5 višina 1, x>=8 višina 2, x>=11 višina 3
        l = lane(o, 2);
        b.fill(l.add(5, 0, -5), l.add(46, 0, 5), STONE);
        b.fill(l.add(8, 1, -5), l.add(46, 1, 5), STONE);
        b.fill(l.add(11, 2, -5), l.add(46, 2, 5), STONE);
        // 4: ploščad x 0..6, z -2..2, vrh na y+3 (stoji se na y+4): padec 4 na tla;
        //    ob z=-3 stopnica višine 2 (padec 2 + 2) — edina dovoljena pot dol
        l = lane(o, 3);
        b.fill(l.add(0, 0, -2), l.add(6, 3, 2), STONE);
        b.fill(l.add(0, 0, -3), l.add(6, 1, -3), STONE);
        // 5: stebri višine 2 na (k+1, k), k = 0..4: diagonala je prosta z enim prostim vogalom
        l = lane(o, 4);
        for (int k = 0; k <= 4; k++) {
            b.fill(l.add(k + 1, 0, k), l.add(k + 1, 1, k), STONE);
        }
        // 6: hodnik 1x2 dolg 12 (x 0..12, z=0), stene z=±1, strop y+2, zadnja stena x=-1
        l = lane(o, 5);
        b.fill(l.add(0, 0, -1), l.add(12, 1, -1), STONE);
        b.fill(l.add(0, 0, 1), l.add(12, 1, 1), STONE);
        b.fill(l.add(-1, 0, -1), l.add(12, 2, 1), STONE);
        b.fill(l.add(0, 0, 0), l.add(12, 1, 0), AIR);
        // 7: zid x=10 višine 3; pri z=0 odprtina z zgornjo polovično ploščo na y+1 (1,5 bloka
        //    prostora, entiteta 1,95 ne gre skozi); obhod skozi režo 1x2 pri z=-4
        l = lane(o, 6);
        b.fill(l.add(10, 0, -5), l.add(10, 2, 5), STONE);
        b.set(l.add(10, 0, 0), AIR);
        b.set(l.add(10, 1, 0), TOP_SLAB);
        b.fill(l.add(10, 0, -4), l.add(10, 1, -4), AIR);
        // 8: prazna proga (obrat 180°)
        // 9: zid x=5 za z -5..2 in zid z=2 za x 5..15 (višina 3): cilj (10, -2) je za vogalom
        l = lane(o, 8);
        b.fill(l.add(5, 0, -5), l.add(5, 2, 2), STONE);
        b.fill(l.add(5, 0, 2), l.add(15, 2, 2), STONE);
        // 10: zaprta škatla 3x3x3 okoli cilja (15, 0, 0), notranjost zrak
        l = lane(o, 9);
        b.fill(l.add(14, 0, -1), l.add(16, 2, 1), STONE);
        b.fill(l.add(15, 0, 0), l.add(15, 1, 0), AIR);
        b.fill(l.add(14, 3, -1), l.add(16, 3, 1), STONE);
        return b.changed;
    }
}
