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

package si.ladja.npcbaritone.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * M6 A5: porabnik (npr. CustomNPC) se prevede samo proti razredom API jarja — brez preostalega
 * {@code core} in {@code forge}. Seznam vzorcev mora biti enak kot v nalogi {@code apiJar}
 * v {@code mod/build.gradle}.
 */
public class ApiJarTest {

    /** Enako kot {@code apiJar} v build.gradle. */
    static final List<String> API_JAR_INCLUDES = Arrays.asList(
            "si/ladja/npcbaritone/api/",
            "si/ladja/npcbaritone/core/api/pathing/goals/",
            "si/ladja/npcbaritone/core/api/utils/BetterBlockPos",
            "si/ladja/npcbaritone/core/api/utils/interfaces/IGoalRenderPos",
            "si/ladja/npcbaritone/core/api/pathing/movement/ActionCosts");

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    private static final String CONSUMER = String.join("\n",
            "package example;",
            "import net.minecraft.entity.EntityLiving;",
            "import net.minecraft.util.math.BlockPos;",
            "import si.ladja.npcbaritone.api.*;",
            "import si.ladja.npcbaritone.core.api.pathing.goals.*;",
            "public class Consumer {",
            "  public static void use(EntityLiving npc) {",
            "    if (!NpcBaritone.available() || NpcBaritone.apiVersion() < 1) return;",
            "    INpcNavigator nav = NpcBaritone.attach(npc, \"default\");",
            "    if (nav == null) return;",
            "    nav.addListener(new NavListener() {",
            "      @Override public void onArrived(EntityLiving e) { }",
            "      @Override public void onFailed(EntityLiving e, String reason) { }",
            "    });",
            "    nav.goTo(new BlockPos(1, 64, 2));",
            "    nav.goTo(new GoalNear(new BlockPos(5, 64, 5), 2));",
            "    nav.goTo(new GoalComposite(new GoalBlock(0, 64, 0), new GoalXZ(10, 10)));",
            "    boolean moving = nav.state() == NavState.MOVING;",
            "    nav.stop();",
            "    if (NpcBaritone.apiVersion() >= 2) {",
            "      nav.reinstall();",
            "      nav.setSpeedMode(SpeedMode.OWN);",
            "      nav.setDoorMode(nav.doorMode() == DoorMode.ALL ? DoorMode.NONE : DoorMode.WOODEN);",
            "      boolean own = nav.speedMode() == SpeedMode.OWN;",
            "    }",
            "  }",
            "}");

    @Test
    public void consumerCompilesAgainstApiJarContentsOnly() throws Exception {
        Path mainOut = Paths.get(NpcBaritone.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue("main classes " + mainOut, Files.isDirectory(mainOut));
        Path apiDir = tmp.newFolder("api-jar").toPath();
        int copied = 0;
        try (Stream<Path> s = Files.walk(mainOut)) {
            for (Path f : s.filter(Files::isRegularFile).collect(Collectors.toList())) {
                String rel = mainOut.relativize(f).toString().replace('\\', '/');
                if (API_JAR_INCLUDES.stream().anyMatch(rel::startsWith)) {
                    Path dst = apiDir.resolve(rel);
                    Files.createDirectories(dst.getParent());
                    Files.copy(f, dst);
                    copied++;
                }
            }
        }
        assertTrue("api jar classes: " + copied, copied > 10);
        // classpath: vse razen main izhoda (tam je celoten core/forge) + samo API razredi
        List<String> cp = new ArrayList<>();
        for (String e : System.getProperty("java.class.path").split(File.pathSeparator)) {
            Path p = Paths.get(e).toAbsolutePath().normalize();
            if (!p.equals(mainOut.toAbsolutePath().normalize())) {
                cp.add(e);
            }
        }
        cp.add(apiDir.toString());
        File src = tmp.newFile("Consumer.java");
        Files.write(src.toPath(), CONSUMER.getBytes(StandardCharsets.UTF_8));
        File out = tmp.newFolder("out");
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull("JDK javac", javac);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int rc = javac.run(null, null, err, "-proc:none", "-nowarn", "-cp", String.join(File.pathSeparator, cp),
                "-d", out.getPath(), src.getPath());
        assertEquals("porabnik se ne prevede samo proti API jarju:\n" + err.toString("UTF-8"), 0, rc);
    }

    @Test
    public void apiPackageImportsOnlyMinecraftJavaAndGoals() throws IOException {
        Path root = null;
        for (String c : new String[]{"src/main/java", "mod/src/main/java"}) {
            if (Files.isDirectory(Paths.get(c, "si/ladja/npcbaritone/api"))) {
                root = Paths.get(c, "si/ladja/npcbaritone/api");
            }
        }
        assertNotNull(root);
        List<String> bad = new ArrayList<>();
        try (Stream<Path> s = Files.walk(root)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                    String t = line.trim();
                    if (t.startsWith("import ") && !(t.startsWith("import net.minecraft.") || t.startsWith("import java.")
                            || t.startsWith("import javax.") || t.startsWith("import si.ladja.npcbaritone.api.")
                            || t.startsWith("import si.ladja.npcbaritone.core.api.pathing.goals."))) {
                        bad.add(f.getFileName() + ": " + t);
                    }
                }
            }
        }
        assertEquals("api/ sme uvažati samo MC, Javo in cilje:\n" + String.join("\n", bad), 0, bad.size());
    }
}
