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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.math.BlockPos;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.forge.net.PathSyncMessage;
import si.ladja.npcbaritone.harness.BootstrapOnce;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** M3: paket poti, pravila pošiljanja, CSV sled, poimenovani profili — brez sveta. */
public class M3VisibilityTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    // ------------------------------------------------------------ M3.2 paket

    private static PathSyncMessage roundTrip(PathSyncMessage m) {
        ByteBuf buf = Unpooled.buffer();
        m.toBytes(buf);
        PathSyncMessage r = new PathSyncMessage();
        r.fromBytes(buf);
        assertEquals("ves paket prebran", 0, buf.readableBytes());
        return r;
    }

    @Test
    public void pathMessageRoundTrip() {
        PathSyncMessage m = new PathSyncMessage();
        m.entityId = 4711;
        m.active = true;
        m.state = NavStatus.State.MOVING.ordinal();
        m.pathPos = 3;
        m.path = Arrays.asList(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0), new BlockPos(-30000000 + 1, 0, 29999999), new BlockPos(2, 255, -5));
        m.next = Collections.singletonList(new BlockPos(7, 70, 7));
        m.goal = new BlockPos(10, 65, -3);
        PathSyncMessage r = roundTrip(m);
        assertEquals(4711, r.entityId);
        assertTrue(r.active);
        assertEquals(NavStatus.State.MOVING.ordinal(), r.state);
        assertEquals(3, r.pathPos);
        assertEquals(m.path, r.path);
        assertEquals(m.next, r.next);
        assertEquals(m.goal, r.goal);
    }

    @Test
    public void pathMessageWithoutGoalOrNext() {
        PathSyncMessage m = new PathSyncMessage();
        m.entityId = 1;
        m.active = true;
        m.state = NavStatus.State.SEARCHING.ordinal();
        PathSyncMessage r = roundTrip(m);
        assertTrue(r.active);
        assertTrue(r.path.isEmpty());
        assertTrue(r.next.isEmpty());
        assertNull(r.goal);
    }

    @Test
    public void inactiveMessageIsSixBytes() {
        ByteBuf buf = Unpooled.buffer();
        PathSyncMessage.inactive(99).toBytes(buf);
        assertEquals(6, buf.readableBytes());
        PathSyncMessage r = new PathSyncMessage();
        r.fromBytes(buf);
        assertEquals(99, r.entityId);
        assertFalse(r.active);
    }

    @Test
    public void longPathIsCapped() {
        PathSyncMessage m = new PathSyncMessage();
        m.active = true;
        List<BlockPos> path = new ArrayList<>();
        for (int i = 0; i < PathSyncMessage.MAX_POSITIONS + 500; i++) {
            path.add(new BlockPos(i, 64, 0));
        }
        m.path = path;
        PathSyncMessage r = roundTrip(m);
        assertEquals(PathSyncMessage.MAX_POSITIONS, r.path.size());
        assertEquals(path.get(PathSyncMessage.MAX_POSITIONS - 1), r.path.get(r.path.size() - 1));
    }

    @Test
    public void unknownFormatIsRejected() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(PathSyncMessage.FORMAT + 1);
        buf.writeInt(1);
        buf.writeByte(0);
        try {
            new PathSyncMessage().fromBytes(buf);
            fail("format mora biti preverjen");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    // ------------------------------------------------------------ M3.2 pravila pošiljanja

    @Test
    public void sendRules() {
        // prehod neaktivno -> aktivno in nazaj: vedno
        assertTrue(DebugSync.shouldSend(100, 99, 1, 1, false, true));
        assertTrue(DebugSync.shouldSend(100, 99, 1, 1, true, false));
        // neaktivno ostane neaktivno: nikoli (ne ponavljamo)
        assertFalse(DebugSync.shouldSend(1000, 0, 1, 1, false, false));
        // aktivno: sprememba vsebine ali keepalive
        assertTrue(DebugSync.shouldSend(105, 100, 1, 2, true, true));
        assertFalse(DebugSync.shouldSend(105, 100, 1, 1, true, true));
        assertTrue(DebugSync.shouldSend(100 + DebugSync.KEEPALIVE, 100, 1, 1, true, true));
        // 4 Hz: najmanjši interval 5 tickov
        assertEquals(5, DebugSync.MIN_INTERVAL);
    }

    @Test
    public void hashFollowsContent() {
        PathSyncMessage a = new PathSyncMessage();
        a.active = true;
        a.path = Arrays.asList(new BlockPos(0, 64, 0), new BlockPos(1, 64, 0));
        PathSyncMessage b = new PathSyncMessage();
        b.active = true;
        b.path = new ArrayList<>(a.path);
        assertEquals(DebugSync.hash(a), DebugSync.hash(b));
        b.pathPos = 1;
        assertTrue(DebugSync.hash(a) != DebugSync.hash(b));
    }

    // ------------------------------------------------------------ M3.4 sled

    @Test
    public void traceRowHasAllColumnsAndQuotesGoal() {
        String row = PathTrace.format(12345L, 7, "Zombie", "T1/3", "MOVING", 1.5, 64, -2.25,
                "GoalBlock{x=1,y=2,z=3}", 40, 5, "MovementTraverse", 812, 3, 2, "", "CALC_STARTED|CALC_FINISHED_NOW_EXECUTING");
        List<String> cells = parseCsvLine(row);
        assertEquals(PathTrace.COLUMNS, cells.size());
        assertEquals("GoalBlock{x=1,y=2,z=3}", cells.get(8));
        assertEquals("64.000", cells.get(6));
        assertEquals("CALC_STARTED|CALC_FINISHED_NOW_EXECUTING", cells.get(16));
    }

    @Test
    public void csvEscaping() {
        assertEquals("", PathTrace.csv(null));
        assertEquals("abc", PathTrace.csv("abc"));
        assertEquals("\"a,b\"", PathTrace.csv("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", PathTrace.csv("say \"hi\""));
    }

    @Test
    public void traceDumpWritesHeaderEvenWhenEmpty() throws Exception {
        PathTrace.INSTANCE.start(null);
        PathTrace.INSTANCE.stop();
        File f = new File(tmp.getRoot(), "sub/trace.csv");
        assertEquals(0, PathTrace.INSTANCE.dump(f));
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        assertEquals(Collections.singletonList(PathTrace.HEADER), lines);
        assertEquals(17, PathTrace.COLUMNS);
    }

    @Test
    public void failReasonCodes() {
        assertEquals("no_path", NavStatus.reasonFrom("failure"));
        assertEquals("queue_full", NavStatus.reasonFrom("queue_full"));
        assertEquals("exception", NavStatus.reasonFrom("exception"));
        assertEquals("unknown", NavStatus.reasonFrom(null));
    }

    // ------------------------------------------------------------ M3.1 profili (D-016)

    @Test
    public void namedProfilesParse() {
        Map<String, Map<String, String>> p = NpcbConfig.parseProfiles(new String[]{
                "Walk: allowSprint=false",
                "x: a=1, B = 2 ,, =3, c=",
                "bad name: a=1",
                ":nothing",
                "noColon"});
        assertEquals(Arrays.asList("walk", "x"), new ArrayList<>(p.keySet()));
        assertEquals("false", p.get("walk").get("allowsprint"));
        assertEquals(2, p.get("x").size());
        assertEquals("2", p.get("x").get("b"));
    }

    @Test
    public void defaultProfileAlwaysPresent() {
        NpcbConfig c = NpcbConfig.defaults();
        assertTrue(c.profiles.containsKey(NpcbConfig.DEFAULT_PROFILE));
        assertTrue(c.profiles.get(NpcbConfig.DEFAULT_PROFILE).isEmpty());
        assertTrue(c.profiles.keySet().containsAll(Arrays.asList("walk", "parkour", "cautious")));
    }

    @Test
    public void profileOverridesApply() {
        NpcbConfig c = NpcbConfig.defaults();
        Settings def = Attach.profileFor(c, "default");
        assertTrue(def.allowSprint.value);
        assertFalse(def.allowParkour.value);
        assertFalse(Attach.profileFor(c, "WALK").allowSprint.value);
        assertTrue(Attach.profileFor(c, "parkour").allowParkour.value);
        assertEquals(2, (int) Attach.profileFor(c, "cautious").maxFallHeightNoWater.value);
        // profil ne sme povoziti strežniškega configa, ki ga ne omenja
        assertEquals(30f, Attach.profileFor(c, "walk").npcMaxTurnDegrees.value, 0);
    }

    @Test
    public void ownSpeedModeKeepsParkourOff() {
        NpcbConfig own = new NpcbConfig(2, 64, 8, 1, NpcbConfig.SpeedMode.OWN, 30, false);
        assertFalse(Attach.profileFor(own, "parkour").allowParkour.value);
    }

    @Test
    public void badProfilesAreRejected() {
        NpcbConfig c = NpcbConfig.defaults();
        try {
            Attach.profileFor(c, "nope");
            fail("neznan profil");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("walk"));
        }
        NpcbConfig bad = new NpcbConfig(2, 64, 8, 1, NpcbConfig.SpeedMode.PLAYER, 30, false,
                NpcbConfig.parseProfiles(new String[]{"typo: allowSprnt=false"}));
        try {
            Attach.profileFor(bad, "typo");
            fail("neznana nastavitev");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("allowsprnt"));
        }
    }

    // ------------------------------------------------------------ pomožno

    /** Minimalen RFC 4180 bralnik ene vrstice. */
    static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (q) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        q = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                q = true;
            } else if (ch == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
