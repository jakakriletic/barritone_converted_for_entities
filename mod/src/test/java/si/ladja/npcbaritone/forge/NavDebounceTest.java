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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static si.ladja.npcbaritone.forge.NavDebounce.Action.KEEP;
import static si.ladja.npcbaritone.forge.NavDebounce.Action.NEW;
import static si.ladja.npcbaritone.forge.NavDebounce.Action.REJECT;
import static si.ladja.npcbaritone.forge.NavDebounce.Action.RESUME;
import static si.ladja.npcbaritone.forge.NavStatus.State.ARRIVED;
import static si.ladja.npcbaritone.forge.NavStatus.State.FAILED;
import static si.ladja.npcbaritone.forge.NavStatus.State.IDLE;
import static si.ladja.npcbaritone.forge.NavStatus.State.MOVING;
import static si.ladja.npcbaritone.forge.NavStatus.State.SEARCHING;

/** M6.1 (D-018): kdaj klici vanilla taskov sprožijo novo iskanje; M6 A4 na ravni pravil. */
public class NavDebounceTest {

    private static final BlockPos A = new BlockPos(10, 64, 10);

    @Test
    public void sameGoalTwentyTimesPerSecondIsOneSearch() {
        NavDebounce d = new NavDebounce();
        int searches = 0;
        for (int t = 0; t < 20; t++) {
            // prvi klic začne iskanje, nato hoja; cilj niha za ±1 blok (entiteta se premika)
            NavStatus.State s = t == 0 ? IDLE : (t < 3 ? SEARCHING : MOVING);
            BlockPos p = A.add(t % 2, 0, -(t % 2));
            if (d.request(p, 1, t, s, false) == NEW) {
                searches++;
            }
            d.tick(t, s);
        }
        assertEquals("A4: 20 klicev z istim ciljem = 1 iskanje", 1, searches);
    }

    @Test
    public void differentGoalIsNewSearch() {
        NavDebounce d = new NavDebounce();
        assertEquals(NEW, d.request(A, 1, 0, IDLE, false));
        assertEquals(NEW, d.request(A.add(2, 0, 0), 1, 1, MOVING, false));
        assertEquals("sledenje: toleranca 2", KEEP, d.request(A.add(4, 0, 0), 2, 2, MOVING, false));
    }

    @Test
    public void clearPausesAndSameGoalResumesWithinTenTicks() {
        NavDebounce d = new NavDebounce();
        d.request(A, 1, 0, IDLE, false);
        d.clear(5);
        assertTrue(d.paused());
        assertFalse(d.tick(10, MOVING));
        assertEquals(RESUME, d.request(A, 1, 12, MOVING, false));
        assertFalse(d.paused());
    }

    @Test
    public void pauseExpiresIntoRealCancel() {
        NavDebounce d = new NavDebounce();
        d.request(A, 1, 0, IDLE, false);
        d.clear(5);
        assertFalse(d.tick(15, MOVING));
        assertTrue("po 10 tickih pravi preklic", d.tick(16, MOVING));
        assertFalse(d.paused());
        assertEquals("po preklicu isti cilj = novo iskanje", NEW, d.request(A, 1, 20, IDLE, false));
    }

    @Test
    public void arrivedKeepsWhileStillInGoal() {
        NavDebounce d = new NavDebounce();
        d.request(A, 1, 0, IDLE, false);
        assertEquals(KEEP, d.request(A, 1, 50, ARRIVED, true));
        assertEquals("odrinjen iz cilja → znova", NEW, d.request(A, 1, 60, ARRIVED, false));
    }

    @Test
    public void failedGoalIsRejectedForTwentyTicks() {
        NavDebounce d = new NavDebounce();
        d.request(A, 1, 0, IDLE, false);
        d.tick(30, FAILED);
        assertEquals(REJECT, d.request(A, 1, 31, FAILED, false));
        assertEquals(REJECT, d.request(A, 1, 49, FAILED, false));
        assertEquals(NEW, d.request(A, 1, 50, FAILED, false));
        assertEquals("drug cilj takoj", NEW, d.request(A.add(5, 0, 0), 1, 51, FAILED, false));
    }

    @Test
    public void newGoalWinsOverStaleOutcome() {
        // takoj po novem cilju je proces aktiven, zastavici še kažeta stari izid
        assertEquals(SEARCHING, NavStatus.decide(false, false, true, true, false));
        assertEquals(SEARCHING, NavStatus.decide(false, false, true, false, true));
        assertEquals(FAILED, NavStatus.decide(false, false, false, true, true));
        assertEquals(ARRIVED, NavStatus.decide(false, false, false, false, true));
        assertEquals(MOVING, NavStatus.decide(true, true, true, true, true));
        assertEquals(IDLE, NavStatus.decide(false, false, false, false, false));
    }

    @Test
    public void sprintAndSizePolicy() {
        assertTrue("brez taska po profilu", BaritonePathNavigate.allowsSprint(0));
        assertFalse("napad/tavanje 1,0 hodi", BaritonePathNavigate.allowsSprint(1.0));
        assertFalse(BaritonePathNavigate.allowsSprint(0.6));
        assertTrue("panika 1,25+", BaritonePathNavigate.allowsSprint(1.25));
        assertTrue("zombi 0,6×1,95", BaritonePathNavigate.fits(0.6F, 1.95F));
        assertTrue("volk 0,6×0,85", BaritonePathNavigate.fits(0.6F, 0.85F));
        assertFalse("pajek 1,4×0,9 → vanilla (D-019)", BaritonePathNavigate.fits(1.4F, 0.9F));
        assertFalse("enderman 0,6×2,9 → vanilla", BaritonePathNavigate.fits(0.6F, 2.9F));
    }

    @Test
    public void largeEntitiesSwitch() {
        // privzeti config: D-019 ostane (obnašanje porabnika se brez stikala ne spremeni)
        assertFalse(NpcbConfig.defaults().largeEntities);
        assertFalse(BaritonePathNavigate.fits(1.4F, 0.9F, false));
        // D-028: s stikalom do 3 stolpcev (širina ≤ 3,0) in 4 blokov (višina ≤ 4,0)
        assertTrue("pajek 1,4×0,9", BaritonePathNavigate.fits(1.4F, 0.9F, true));
        assertTrue("enderman 0,6×2,9", BaritonePathNavigate.fits(0.6F, 2.9F, true));
        assertTrue("železni golem 1,4×2,7", BaritonePathNavigate.fits(1.4F, 2.7F, true));
        assertTrue("CNPC size 10 1,2×3,6", BaritonePathNavigate.fits(0.6F / 5F * 10, 1.8F / 5F * 10, true));
        assertTrue("zombi 0,6×1,95", BaritonePathNavigate.fits(0.6F, 1.95F, true));
        assertFalse("ghast 4×4 → vanilla", BaritonePathNavigate.fits(4.0F, 4.0F, true));
        assertFalse("višina 4,5 → vanilla", BaritonePathNavigate.fits(0.6F, 4.5F, true));
        assertFalse("ničelna velikost", BaritonePathNavigate.fits(0F, 1.8F, true));
    }
}
