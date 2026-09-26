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

import net.minecraft.util.math.BlockPos;
import si.ladja.npcbaritone.core.pathing.movement.EntitySize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * M8.9: tečaj T3 — T1 in T2 za CNPC velikosti 1, 3, 5, 7, 10 (D-028). T1 stoji na izhodišču,
 * T2 {@link #T2_OFFSET} blokov vzhodno. Pred vsakim odsekom tekač entiteti nastavi velikost
 * {@code 0,6/5·size × 1,8/5·size} (CNPC {@code EntityNPCInterface} vr. 1086–1087).
 *
 * <p>Pričakovani izidi: osnovni izid T1/T2, razen kjer velikost po modelu D-028 ne gre skozi
 * ({@link #EXPECT_FAIL}) ali gre skozi, kjer 1×2 ne sme ({@link #ALLOW_FORBIDDEN}). Tabelo
 * preveri headless {@code CourseT3PathTest}; vsaka izjema ima razlog.
 */
public final class CourseT3 implements Course {

    public static final CourseT3 INSTANCE = new CourseT3();

    public static final int[] SIZES = {1, 3, 5, 7, 10};
    public static final BlockPos T2_OFFSET = new BlockPos(60, 0, 0);

    /**
     * Odseki, ki za dano velikost ne morejo uspeti (ključ {@code "size:T1/7"}), z razlogom.
     * Model: višina v blokih = ⌈1,8/5·size⌉, širina > 1 potrebuje 3 stolpce, široke entitete ne
     * plezajo po lestvah.
     */
    static final Map<String, String> EXPECT_FAIL;
    /** Odseki, kjer velikost sme skozi prepovedan stolpec (reža 1,5 je dovolj za nizke). */
    static final Map<String, String> ALLOW_FORBIDDEN;

    static {
        Map<String, String> f = new LinkedHashMap<>();
        f.put(key(7, "T1", 6), "hodnik visok 2, entiteta 2,52 (3 bloki)");
        f.put(key(7, "T2", 1), "vrata visoka 2, entiteta 2,52 (3 bloki)");
        f.put(key(10, "T1", 5), "okvir 3x3 na cilju (5, 5) zadene steber (5, 4)");
        f.put(key(10, "T1", 6), "hodnik 1x2, entiteta 1,2 x 3,6");
        f.put(key(10, "T2", 1), "vrata široka 1, entiteta 1,2 (3 stolpci)");
        f.put(key(10, "T2", 3), "ograjna vrata široka 1");
        f.put(key(10, "T2", 4), "široke entitete ne plezajo po lestvi (Automatone 3a08d43e)");
        f.put(key(10, "T2", 5), "lestev dol neuporabna, padec 5 > maxFallHeightNoWater 3");
        f.put(key(10, "T2", 9), "reže med kaktusi so široke 1");
        EXPECT_FAIL = Collections.unmodifiableMap(f);
        Map<String, String> a = new LinkedHashMap<>();
        a.put(key(1, "T1", 7), "reža 1,5 zadošča za višino 0,36 (1 blok)");
        ALLOW_FORBIDDEN = Collections.unmodifiableMap(a);
    }

    private CourseT3() {
    }

    /** CNPC velikost → širina × višina. */
    public static EntitySize npcSize(int size) {
        return new EntitySize(0.6F / 5F * size, 1.8F / 5F * size);
    }

    @Override
    public String id() {
        return "T3";
    }

    @Override
    public boolean forceChunks() {
        return true;
    }

    @Override
    public List<Segment> segments(BlockPos o) {
        List<Segment> out = new ArrayList<>();
        int index = 1;
        for (int size : SIZES) {
            for (Segment s : CourseT1.segmentsAt(o)) {
                out.add(derive(s, "T1", size, index++));
            }
            for (Segment s : CourseT2.INSTANCE.segments(o.add(T2_OFFSET))) {
                out.add(derive(s, "T2", size, index++));
            }
        }
        return Collections.unmodifiableList(out);
    }

    static String key(int size, String course, int segment) {
        return size + ":" + course + "/" + segment;
    }

    private static Segment derive(Segment s, String course, int size, int index) {
        String k = key(size, course, s.index);
        Expect expect = EXPECT_FAIL.containsKey(k) ? Expect.FAIL : s.expect;
        BlockPos forbidden = ALLOW_FORBIDDEN.containsKey(k) ? null : s.forbiddenColumn;
        return s.forSize(index, "s" + size + " " + course + "/" + s.index + " " + s.name, expect, forbidden, size);
    }

    @Override
    public int build(Sink sink, BlockPos origin) {
        return CourseT1.buildAt(sink, origin) + CourseT2.INSTANCE.build(sink, origin.add(T2_OFFSET));
    }

    @Override
    public BlockPos[] bounds(BlockPos o) {
        BlockPos[] a = CourseT1.INSTANCE.bounds(o);
        BlockPos[] b = CourseT2.INSTANCE.bounds(o.add(T2_OFFSET));
        return new BlockPos[]{
                new BlockPos(Math.min(a[0].getX(), b[0].getX()), Math.min(a[0].getY(), b[0].getY()), Math.min(a[0].getZ(), b[0].getZ())),
                new BlockPos(Math.max(a[1].getX(), b[1].getX()), Math.max(a[1].getY(), b[1].getY()), Math.max(a[1].getZ(), b[1].getZ()))};
    }
}
