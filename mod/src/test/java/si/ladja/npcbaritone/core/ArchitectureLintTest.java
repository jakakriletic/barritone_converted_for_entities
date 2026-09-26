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

package si.ladja.npcbaritone.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * M1.6 in M1.13: poceni, trajna preverjanja na izvorni kodi (ArchUnit-lite).
 * <ul>
 *     <li>D-012: {@code core} ne kliče sveta mimo {@code BlockStateInterface} (klici, ki na
 *     strežniku naložijo ali generirajo chunk), razen v dovoljenih razredih.</li>
 *     <li>D-024: nobena datoteka izven {@code client/} ne uvaža {@code net.minecraft.client}.</li>
 *     <li>Meje paketov: {@code core} ne uvaža {@code forge}; izven {@code client/} nihče ne uvaža {@code client} (M3).</li>
 *     <li>D-002: v {@code mod/src/main} ni Elytre.</li>
 * </ul>
 * Test bere izvorne datoteke, zato mu ni mar za prevod; lovi besedilo, ne tipov.
 */
public class ArchitectureLintTest {

    /** Klici, ki na strežniku lahko naložijo ali generirajo chunk (D-012). */
    private static final Pattern WORLD_ACCESS = Pattern.compile(
            "world\\(\\)\\.getBlockState\\(|\\bworld\\.getBlockState\\(|\\.getChunkFromChunkCoords\\(|"
                    + "\\.getChunkFromBlockCoords\\(|\\.provideChunk\\(|\\.loadChunk\\(|\\.getChunk\\(");

    /** Razredi, ki smejo brati svet neposredno (posnetek in BSI sta edina vira). */
    private static final List<String> WORLD_ACCESS_ALLOWED = java.util.Arrays.asList(
            "si/ladja/npcbaritone/core/world/",
            "si/ladja/npcbaritone/core/utils/BlockStateInterface.java");

    @Test
    public void coreReadsWorldOnlyThroughBlockStateInterface() throws IOException {
        List<String> hits = grep(core(), WORLD_ACCESS, WORLD_ACCESS_ALLOWED);
        assertEquals("D-012 violations:\n" + String.join("\n", hits), 0, hits.size());
    }

    @Test
    public void noClientImportsOutsideClientPackage() throws IOException {
        List<String> hits = grep(mainJava(), Pattern.compile("^import\\s+net\\.minecraft\\.client\\.|Minecraft\\.getMinecraft\\(\\)"),
                java.util.Collections.singletonList("si/ladja/npcbaritone/client/"));
        assertEquals("D-024 violations:\n" + String.join("\n", hits), 0, hits.size());
    }

    @Test
    public void coreDoesNotImportForge() throws IOException {
        List<String> hits = grep(core(), Pattern.compile("^import\\s+si\\.ladja\\.npcbaritone\\.forge\\."), java.util.Collections.emptyList());
        assertEquals("core -> forge imports:\n" + String.join("\n", hits), 0, hits.size());
    }

    /** M3: {@code client} doseže samo {@code @SidedProxy} kot niz; drugače bi dedicated strežnik naložil klientske razrede. */
    @Test
    public void nothingOutsideClientImportsClientPackage() throws IOException {
        List<String> hits = grep(mainJava(), Pattern.compile("^import\\s+si\\.ladja\\.npcbaritone\\.client\\."),
                java.util.Collections.singletonList("si/ladja/npcbaritone/client/"));
        assertEquals("client imports outside client/:\n" + String.join("\n", hits), 0, hits.size());
    }

    @Test
    public void noElytra() throws IOException {
        List<String> hits = grep(mainJava(), Pattern.compile("(?i)elytra(?!Flying|_)"), java.util.Collections.emptyList());
        // isElytraFlying() je vanilla metoda entitete, ki jo premiki še preverjajo; to ni Elytra proces.
        assertEquals("D-002: elytra code left:\n" + String.join("\n", hits), 0, hits.size());
    }

    @Test
    public void lintActuallyScansSources() throws IOException {
        try (Stream<Path> s = Files.walk(core())) {
            long n = s.filter(p -> p.toString().endsWith(".java")).count();
            assertTrue("core has only " + n + " java files — wrong source root?", n > 100);
        }
        // pozitivna kontrola vzorca
        assertTrue(WORLD_ACCESS.matcher("x = ctx.world().getBlockState(pos);").find());
        assertTrue(WORLD_ACCESS.matcher("entity.world.getBlockState(pos)").find());
    }

    // ------------------------------------------------------------------------

    private static Path mainJava() {
        for (String candidate : new String[]{"src/main/java", "mod/src/main/java"}) {
            Path p = Paths.get(candidate);
            if (Files.isDirectory(p.resolve("si/ladja/npcbaritone"))) {
                return p;
            }
        }
        throw new IllegalStateException("cannot locate mod/src/main/java from " + Paths.get("").toAbsolutePath());
    }

    private static Path core() {
        return mainJava().resolve("si/ladja/npcbaritone/core");
    }

    private static List<String> grep(Path root, Pattern pattern, List<String> allowedPrefixes) throws IOException {
        Path base = mainJava();
        List<String> hits = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
        for (Path f : files) {
            String rel = base.relativize(f).toString().replace('\\', '/');
            if (allowedPrefixes.stream().anyMatch(rel::startsWith)) {
                continue;
            }
            List<String> lines = Files.readAllLines(f, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmed = line.trim();
                if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                    continue;
                }
                if (pattern.matcher(line).find()) {
                    hits.add(rel + ":" + (i + 1) + ": " + trimmed);
                }
            }
        }
        return hits;
    }
}
