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

import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tečaj T2 — interakcije (03-FAZE, M4.8). Deset prog vzdolž +X kot pri T1, a vsaka proga
 * je <b>zaprta</b>: kamnite stene na {@code z=±6} in {@code x=-7/47}, visoke 4 bloke — sicer
 * bi NPC v superflat svetu ovire obšel zunaj proge in vrata ne bi bila preizkušena.
 * Tekočine so v jarkih s kamnito lupino (ostanejo na mestu), zato globina ≤ 3 pri
 * izhodišču {@code y=4} v superflat svetu (dno jarka je bedrock/y=0).
 *
 * <p>Odseki: lesena vrata, železna vrata (FAILED), ograjna vrata, lestev gor 5, lestev dol 5,
 * voda 1, voda 3, lava z mostom, kaktusi z režami, padec 10 v vodo.
 */
public final class CourseT2 implements Course {

    public static final CourseT2 INSTANCE = new CourseT2();
    public static final int LANE_SPACING = 12;
    /** Višina čiste zračne prostornine nad tlemi proge (stolp v odseku 10 je visok 10). */
    static final int AIR_HEIGHT = 13;

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState WATER = Blocks.WATER.getDefaultState();
    private static final IBlockState LAVA = Blocks.LAVA.getDefaultState();
    private static final IBlockState SAND = Blocks.SAND.getDefaultState();
    private static final IBlockState FENCE = Blocks.OAK_FENCE.getDefaultState();
    private static final IBlockState CACTUS = Blocks.CACTUS.getDefaultState().withProperty(BlockCactus.AGE, 0);

    private CourseT2() {
    }

    @Override
    public String id() {
        return "T2";
    }

    public static BlockPos lane(BlockPos origin, int i) {
        return origin.add(0, 0, i * LANE_SPACING);
    }

    @Override
    public BlockPos[] bounds(BlockPos o) {
        return new BlockPos[]{o.add(-7, -4, -6), o.add(47, AIR_HEIGHT, 9 * LANE_SPACING + 6)};
    }

    @Override
    public List<Segment> segments(BlockPos o) {
        List<Segment> s = new ArrayList<>();
        BlockPos l;
        l = lane(o, 0);
        s.add(new Segment(1, "lesena vrata", l, l.add(20, 0, 0), Expect.REACH, Double.NaN, null, -90)
                .withOpenables(l.add(10, 0, 0)));
        l = lane(o, 1);
        s.add(new Segment(2, "železna vrata (FAILED)", l, l.add(20, 0, 0), Expect.FAIL, Double.NaN, null, -90));
        l = lane(o, 2);
        s.add(new Segment(3, "ograjna vrata", l, l.add(20, 0, 0), Expect.REACH, Double.NaN, null, -90)
                .withOpenables(l.add(10, 0, 0)));
        l = lane(o, 3);
        s.add(new Segment(4, "lestev gor 5", l, l.add(15, 5, 0), Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 4);
        s.add(new Segment(5, "lestev dol 5", l.add(0, 5, 0), l.add(20, 0, 0), Expect.REACH, 3.5, null, -90));
        l = lane(o, 5);
        s.add(new Segment(6, "voda 1 globoko", l, l.add(20, 0, 0), Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 6);
        s.add(new Segment(7, "voda 3 globoko (plavanje)", l, l.add(20, 0, 0), Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 7);
        s.add(new Segment(8, "lava z mostom ob strani", l, l.add(20, 0, 0), Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 8);
        s.add(new Segment(9, "kaktusi z režami", l, l.add(20, 0, 0), Expect.REACH, Double.NaN, null, -90));
        l = lane(o, 9);
        s.add(new Segment(10, "padec 10 v vodo", l.add(0, 10, 0), l.add(15, 0, 0), Expect.REACH, Double.NaN, null, -90));
        return Collections.unmodifiableList(s);
    }

    @Override
    public int build(Sink w, BlockPos o) {
        Builder b = new Builder(w);
        for (int i = 0; i < 10; i++) {
            BlockPos l = lane(o, i);
            b.fill(l.add(-6, 0, -5), l.add(46, AIR_HEIGHT, 5), AIR);
            b.fill(l.add(-7, -1, -6), l.add(47, -1, 6), STONE);
            b.fill(l.add(-7, 0, -6), l.add(47, 3, -6), STONE);
            b.fill(l.add(-7, 0, 6), l.add(47, 3, 6), STONE);
            b.fill(l.add(-7, 0, -6), l.add(-7, 3, 6), STONE);
            b.fill(l.add(47, 0, -6), l.add(47, 3, 6), STONE);
        }
        BlockPos l;
        // 1: zid x=10 (višina 4), lesena vrata na z=0, zaprta, gledajo proti +X
        l = lane(o, 0);
        b.fill(l.add(10, 0, -5), l.add(10, 3, 5), STONE);
        door(b, l.add(10, 0, 0), Blocks.OAK_DOOR.getDefaultState());
        // 2: isto z železnimi vrati — edini vhod v zadnji del proge
        l = lane(o, 1);
        b.fill(l.add(10, 0, -5), l.add(10, 3, 5), STONE);
        door(b, l.add(10, 0, 0), Blocks.IRON_DOOR.getDefaultState());
        // 3: hrastova ograja x=10 (1,5 visoko, ne da se preskočiti), zaprta ograjna vrata na z=0
        l = lane(o, 2);
        b.fill(l.add(10, 0, -5), l.add(10, 0, 5), FENCE);
        b.set(l.add(10, 0, 0), Blocks.OAK_FENCE_GATE.getDefaultState()
                .withProperty(BlockFenceGate.FACING, EnumFacing.EAST).withProperty(BlockFenceGate.OPEN, false));
        // 4: ploščad x 10..46 visoka 5 (stoji se na y+5), lestev na x=9, z=0 ob njeni zahodni steni
        l = lane(o, 3);
        b.fill(l.add(10, 0, -5), l.add(46, 4, 5), STONE);
        ladder(b, l.add(9, 0, 0), 5, EnumFacing.WEST);
        // 5: ploščad x -6..8 visoka 5 (start na y+5), lestev na x=9 ob njeni vzhodni steni
        l = lane(o, 4);
        b.fill(l.add(-6, 0, -5), l.add(8, 4, 5), STONE);
        ladder(b, l.add(9, 0, 0), 5, EnumFacing.EAST);
        // 6, 7: vodna jarka čez vso širino proge
        trench(b, lane(o, 5), 8, 12, 1, WATER);
        trench(b, lane(o, 6), 8, 14, 3, WATER);
        // 8: lava x 10..12 globine 1, most na z=4
        l = lane(o, 7);
        trench(b, l, 10, 12, 1, LAVA);
        b.fill(l.add(10, -1, 4), l.add(12, -1, 4), STONE);
        // 9: kaktusi na x=10 pri sodih z (-4..4) na pesku, nad njimi kamen (ne rastejo);
        //    reže širine 1 pri lihih z
        l = lane(o, 8);
        for (int z = -4; z <= 4; z += 2) {
            b.set(l.add(10, -1, z), SAND);
            b.set(l.add(10, 0, z), CACTUS);
            b.set(l.add(10, 1, z), STONE);
        }
        // 10: stolp x -6..3 visok 10 (start na y+10), bazen x 4..8 globine 2; drugače je padec 10 na kamen
        l = lane(o, 9);
        b.fill(l.add(-6, 0, -5), l.add(3, 9, 5), STONE);
        trench(b, l, 4, 8, 2, WATER);
        return b.changed;
    }

    /** Vrata z dnom na {@code p}, zaprta, gledajo proti +X (zapirajo prehod vzdolž X). */
    private static void door(Builder b, BlockPos p, IBlockState base) {
        IBlockState lower = base.withProperty(BlockDoor.FACING, EnumFacing.EAST).withProperty(BlockDoor.OPEN, false)
                .withProperty(BlockDoor.HINGE, BlockDoor.EnumHingePosition.LEFT).withProperty(BlockDoor.POWERED, false)
                .withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.LOWER);
        b.set(p, lower);
        b.set(p.up(), lower.withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.UPPER));
    }

    /** Lestev višine {@code h} od {@code p} navzgor; {@code facing} = smer stran od stene. */
    private static void ladder(Builder b, BlockPos p, int h, EnumFacing facing) {
        IBlockState s = Blocks.LADDER.getDefaultState().withProperty(BlockLadder.FACING, facing);
        for (int y = 0; y < h; y++) {
            b.set(p.up(y), s);
        }
    }

    /**
     * Jarek čez vso širino proge ({@code z -5..5}) med {@code x1..x2}, globine {@code depth} pod
     * tlemi, s kamnito lupino (tekočina ne odteče). Površina tekočine je na {@code y-1}.
     */
    private static void trench(Builder b, BlockPos l, int x1, int x2, int depth, IBlockState fluid) {
        b.fill(l.add(x1 - 1, -depth - 1, -6), l.add(x2 + 1, -1, 6), STONE);
        b.fill(l.add(x1, -depth, -5), l.add(x2, -1, 5), fluid);
    }
}
