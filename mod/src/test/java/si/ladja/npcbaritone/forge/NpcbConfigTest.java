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
import si.ladja.npcbaritone.harness.BootstrapOnce;

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
    public void avoidWaterProfilePenalizesSurfaceWaterOnlyWhenSelected() {
        BootstrapOnce.ensure();
        NpcbConfig config = NpcbConfig.defaults();
        assertEquals(0.0, Attach.profileFor(config, "default").npcWaterPenalty.value, 0.001);
        // D-040: 8 blokov hoje na blok vode = vanilla PathNodeType.WATER (malus 8)
        assertEquals(8 * si.ladja.npcbaritone.core.api.pathing.movement.ActionCosts.WALK_ONE_BLOCK_COST,
                Attach.profileFor(config, "avoid_water").npcWaterPenalty.value, 0.01);
        assertEquals(3.0, Attach.profileFor(config, "avoid_water").walkOnWaterOnePenalty.value, 0.001);
    }

    @Test
    public void crowdYieldOffByDefaultAndConfigOrPropertyTurnsItOn() throws Exception {
        BootstrapOnce.ensure();
        assertFalse(NpcbConfig.defaults().crowdYield);
        assertFalse(Attach.profileFor(NpcbConfig.defaults(), "default").npcCrowdYield.value);
        File f = new File(tmp.getRoot(), "crowd.cfg");
        Files.write(f.toPath(), Arrays.asList("movement {", "    B:crowdYield=true", "}"), StandardCharsets.UTF_8);
        NpcbConfig on = NpcbConfig.load(new Configuration(f));
        assertTrue(on.crowdYield);
        assertTrue(on.applyTo(si.ladja.npcbaritone.core.api.NpcProfile.create()).npcCrowdYield.value);
        assertTrue(on.toString().contains("crowdYield=true"));
        File g = new File(tmp.getRoot(), "prop.cfg");
        System.setProperty("npcbcrowdyield", "true");
        try {
            assertTrue(NpcbConfig.load(new Configuration(g)).crowdYield);
        } finally {
            System.clearProperty("npcbcrowdyield");
        }
        assertFalse(NpcbConfig.load(new Configuration(g)).crowdYield);
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

    /** M11.2 (D-030): {@code worker.enabled} izklopi plast 2 in 3 v celoti; privzeto vklopljeno. */
    @Test
    public void workerEnabledByDefaultAndConfigTurnsItOff() throws Exception {
        assertTrue(NpcbConfig.defaults().workerEnabled);
        File f = tmp.newFile("npcb-worker.cfg");
        Files.write(f.toPath(), Arrays.asList("worker {", "    B:enabled=false", "}"), StandardCharsets.UTF_8);
        NpcbConfig off = NpcbConfig.load(new Configuration(f));
        assertFalse(off.workerEnabled);
        assertTrue(off.toString().contains("workerEnabled=false"));
    }
}
