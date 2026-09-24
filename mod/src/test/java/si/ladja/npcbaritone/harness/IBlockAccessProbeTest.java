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

package si.ladja.npcbaritone.harness;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * M0.9 sonda: katere {@link IBlockAccess} metode vanilla bloki kličejo, ko jih Baritone
 * vpraša po prehodnosti, in kateri bloki ne prenesejo {@code isPassable(null, null)}.
 *
 * <p>Baritonovo iskanje poti kliče svet prek {@code IBlockAccess} na natanko dveh mestih:
 * {@code block.isPassable(bsi.access, pos)} v {@code MovementHelper.canWalkThroughPosition}
 * in {@code fullyPassablePosition}; drugod {@code isPassable(null, null)} (zanašajoč se na to,
 * da vanilla argumentov ne bere). Izvajalec na glavni niti poleg tega kliče
 * {@code getBoundingBox(world, pos)} ({@code VecUtils}, {@code RotationUtils}) na pravem svetu.
 *
 * <p>Sonda za vsako veljavno stanje vsakega registriranega bloka postavi blok v
 * {@link SyntheticWorld} in obe metodi pokliče skozi posredniški {@link IBlockAccess}, ki
 * beleži klice. Poročilo: {@code build/reports/npcb/iblockaccess-probe.txt}.
 */
public class IBlockAccessProbeTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    private static final List<String> report = new ArrayList<>();

    @Test
    public void isPassableOnlyReadsBlockStates() {
        Probe p = probe((state, access) -> state.getBlock().isPassable(access, POS));
        report("isPassable(access, pos)  [iskalna nit, MovementHelper]", p);
        assertTrue("no states probed", p.states > 1000);
        assertEquals("isPassable threw for " + p.failures, 0, p.failures.size());
        // Pripeto po prvem teku: vanilla isPassable bere samo getBlockState. Če se pojavi
        // druga metoda (svetloba, biom, tile entity), posnetek chunkov (D-013) ne zadošča več.
        assertEquals(new TreeSet<>(java.util.Collections.singleton("getBlockState")), p.methods.keySet());
    }

    @Test
    public void getBoundingBoxOnlyReadsBlockStates() {
        Probe p = probe((state, access) -> state.getBoundingBox(access, POS));
        report("getBoundingBox(access, pos)  [glavna nit, VecUtils/RotationUtils]", p);
        assertEquals("getBoundingBox threw for " + p.failures, 0, p.failures.size());
        // Pripeto po prvem teku: getTileEntity kličejo samo shulker boxi in piston_extension;
        // BSI access vrne null in bloki padejo na privzeto škatlo. Teče na glavni niti na
        // pravem svetu, zato posnetka ne zadeva.
        assertEquals(new TreeSet<>(java.util.Arrays.asList("getBlockState", "getTileEntity")), p.methods.keySet());
        for (String b : p.blocksByMethod.get("getTileEntity")) {
            assertTrue("getTileEntity from " + b, b.endsWith("shulker_box") || b.equals("minecraft:piston_extension"));
        }
    }

    @Test
    public void isPassableWithNullArguments() {
        Set<String> throwing = new TreeSet<>();
        int blocks = 0;
        for (Block block : Block.REGISTRY) {
            blocks++;
            try {
                block.isPassable(null, null);
            } catch (Throwable t) {
                throwing.add(Block.REGISTRY.getNameForObject(block) + " -> " + t.getClass().getSimpleName());
            }
        }
        report.add("== isPassable(null, null)  [MovementHelper catch-all] ==");
        report.add("blokov: " + blocks + ", vrže: " + throwing.size());
        report.addAll(throwing);
        report.add("");
        // Vsi, ki vržejo, morajo biti med tistimi, ki jih Baritone obravnava posebej pred
        // catch-all vejo (vrata, vrata ograje, tekočine, loputa, sneg) — sicer postanejo MAYBE.
        for (String s : throwing) {
            assertTrue("unhandled null-arg isPassable: " + s,
                    s.contains("door") || s.contains("fence_gate") || s.contains("water") || s.contains("lava")
                            || s.contains("trapdoor") || s.contains("snow_layer"));
        }
    }

    @AfterClass
    public static void writeReport() throws IOException {
        File dir = new File(System.getProperty("npcb.reportDir", "build/reports/npcb"));
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create " + dir);
        }
        Files.write(new File(dir, "iblockaccess-probe.txt").toPath(), report, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------------

    private static final BlockPos POS = new BlockPos(8, 64, 8);

    private static final class Probe {
        int states;
        final Map<String, Integer> methods = new TreeMap<>();
        final Map<String, Set<String>> blocksByMethod = new TreeMap<>();
        final List<String> failures = new ArrayList<>();
    }

    private static Probe probe(BiConsumer<IBlockState, IBlockAccess> call) {
        Probe p = new Probe();
        SyntheticWorld w = SyntheticWorld.create().ensureChunks(-1, -1, 1, 1);
        for (Block block : Block.REGISTRY) {
            for (IBlockState state : block.getBlockState().getValidStates()) {
                p.states++;
                w.set(POS, state);
                String name = String.valueOf(Block.REGISTRY.getNameForObject(block));
                IBlockAccess recording = (IBlockAccess) Proxy.newProxyInstance(
                        IBlockAccess.class.getClassLoader(), new Class<?>[]{IBlockAccess.class},
                        (proxy, method, args) -> {
                            p.methods.merge(method.getName(), 1, Integer::sum);
                            p.blocksByMethod.computeIfAbsent(method.getName(), k -> new TreeSet<>()).add(name);
                            try {
                                return method.invoke(w.access(), args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        });
                try {
                    call.accept(state, recording);
                } catch (Throwable t) {
                    p.failures.add(state + " -> " + t);
                }
            }
        }
        return p;
    }

    private static void report(String title, Probe p) {
        report.add("== " + title + " ==");
        report.add("stanj: " + p.states + ", napak: " + p.failures.size());
        for (Map.Entry<String, Integer> e : p.methods.entrySet()) {
            Set<String> blocks = p.blocksByMethod.get(e.getKey());
            report.add(String.format("  %-16s klicev %6d  blokov %3d: %s", e.getKey(), e.getValue(), blocks.size(), blocks));
        }
        report.addAll(p.failures);
        report.add("");
    }
}
