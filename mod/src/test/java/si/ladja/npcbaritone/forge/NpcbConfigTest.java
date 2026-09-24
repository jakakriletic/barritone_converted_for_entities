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

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** M0.3: config se ustvari s privzetimi vrednostmi in nesmiselne vrednosti obreže. */
public class NpcbConfigTest {

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    /**
     * Forge {@link Configuration} ob konstrukciji prebere mapo igre iz {@link FMLInjectionData}
     * (v igri jo nastavi FML). V navadnem JUnit jo nastavimo na začasno mapo.
     */
    @Before
    public void fakeMinecraftHome() throws Exception {
        Field home = FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        if (home.get(null) == null) {
            home.set(null, tmp.getRoot());
        }
    }

    @Test
    public void defaultsMatchArchitectureTable() {
        NpcbConfig d = NpcbConfig.defaults();
        assertEquals(2, d.searchThreads);
        assertEquals(64, d.searchQueueLimit);
        assertEquals(8, d.snapshotMarginChunks);
        assertEquals(1, d.shareRadiusChunks);
        assertEquals(NpcbConfig.SpeedMode.PLAYER, d.speedMode);
        assertEquals(30, d.maxTurnDegrees);
        assertFalse(d.syncPathsToOps);
    }

    @Test
    public void constructorClampsOutOfRange() {
        NpcbConfig c = new NpcbConfig(0, 100000, 1, 99, null, 1000, true);
        assertEquals(1, c.searchThreads);
        assertEquals(1024, c.searchQueueLimit);
        assertEquals(2, c.snapshotMarginChunks);
        assertEquals(4, c.shareRadiusChunks);
        assertEquals(NpcbConfig.SpeedMode.PLAYER, c.speedMode);
        assertEquals(180, c.maxTurnDegrees);
    }

    @Test
    public void speedModeParsing() {
        assertEquals(NpcbConfig.SpeedMode.OWN, NpcbConfig.parseSpeedMode(" OWN "));
        assertEquals(NpcbConfig.SpeedMode.PLAYER, NpcbConfig.parseSpeedMode("player"));
        assertEquals(NpcbConfig.SpeedMode.PLAYER, NpcbConfig.parseSpeedMode("bogus"));
        assertEquals(NpcbConfig.SpeedMode.PLAYER, NpcbConfig.parseSpeedMode(null));
    }

    @Test
    public void loadCreatesFileWithAllSections() throws Exception {
        File f = new File(tmp.getRoot(), "npcbaritone.cfg");
        NpcbConfig c = NpcbConfig.load(new Configuration(f));
        assertEquals(NpcbConfig.defaults().toString(), c.toString());
        assertTrue(f.isFile());
        String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        for (String cat : Arrays.asList("search {", "profile {", "movement {", "debug {")) {
            assertTrue("missing section " + cat + " in\n" + text, text.contains(cat));
        }
        assertTrue(text.contains("I:threads=2"));
    }

    @Test
    public void loadClampsHandEditedValues() throws Exception {
        File f = new File(tmp.getRoot(), "npcbaritone.cfg");
        Files.write(f.toPath(), Arrays.asList(
                "search {", "    I:threads=99", "    I:queueLimit=1", "}",
                "movement {", "    S:speedMode=own", "    I:maxTurnDegrees=0", "}"), StandardCharsets.UTF_8);
        NpcbConfig c = NpcbConfig.load(new Configuration(f));
        assertEquals(8, c.searchThreads);
        assertEquals(8, c.searchQueueLimit);
        assertEquals(NpcbConfig.SpeedMode.OWN, c.speedMode);
        assertEquals(5, c.maxTurnDegrees);
    }
}
