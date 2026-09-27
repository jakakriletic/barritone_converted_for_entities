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

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityZombie;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.harness.BootstrapOnce;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * D-039 (CNPC U3/U4, U6): prepisi instance nad profilom. Vrstni red config → profil → instanca;
 * način hitrosti instance odloča o parkourju in o osnovni hitrosti med vodenjem.
 */
public class InstanceOverridesTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    private static NpcbConfig config(NpcbConfig.SpeedMode speed) {
        Map<String, Map<String, String>> profiles = NpcbConfig.parseProfiles(new String[]{
                "parkour: allowParkour=true", "closed: npcOpenDoors=false"});
        return new NpcbConfig(2, 64, 8, 1, speed, 30, false, profiles);
    }

    @Test
    public void speedModeOfInstanceDecidesParkour() {
        InstanceOverrides none = new InstanceOverrides();
        assertFalse("globalno own", Attach.settingsFor(config(NpcbConfig.SpeedMode.OWN), "parkour", none).allowParkour.value);
        assertTrue("globalno player", Attach.settingsFor(config(NpcbConfig.SpeedMode.PLAYER), "parkour", none).allowParkour.value);

        InstanceOverrides player = new InstanceOverrides();
        player.speedMode = NpcbConfig.SpeedMode.PLAYER;
        assertTrue("instanca player nad globalnim own",
                Attach.settingsFor(config(NpcbConfig.SpeedMode.OWN), "parkour", player).allowParkour.value);
        InstanceOverrides own = new InstanceOverrides();
        own.speedMode = NpcbConfig.SpeedMode.OWN;
        assertFalse("instanca own nad globalnim player",
                Attach.settingsFor(config(NpcbConfig.SpeedMode.PLAYER), "parkour", own).allowParkour.value);
        assertEquals(NpcbConfig.SpeedMode.OWN, own.speed(config(NpcbConfig.SpeedMode.PLAYER)));
        assertEquals(NpcbConfig.SpeedMode.PLAYER, none.speed(config(NpcbConfig.SpeedMode.PLAYER)));
    }

    @Test
    public void doorOverridesComeAfterProfile() {
        NpcbConfig c = config(NpcbConfig.SpeedMode.PLAYER);
        InstanceOverrides none = new InstanceOverrides();
        Settings closed = Attach.settingsFor(c, "closed", none);
        assertFalse("profil zapre vrata", closed.npcOpenDoors.value);
        assertFalse(closed.npcOpenIronDoors.value);

        InstanceOverrides all = new InstanceOverrides();
        all.openDoors = true;
        all.openIronDoors = true;
        Settings s = Attach.settingsFor(c, "closed", all);
        assertTrue("instanca nad profilom", s.npcOpenDoors.value);
        assertTrue(s.npcOpenIronDoors.value);

        Settings def = Attach.settingsFor(c, NpcbConfig.DEFAULT_PROFILE, none);
        assertTrue("privzeto WOODEN", def.npcOpenDoors.value);
        assertFalse(def.npcOpenIronDoors.value);
    }

    /** Preklop med vodenjem: osnovna hitrost se vrne ali nastavi takoj, brez vodenja se ne dotakne. */
    @Test
    public void moveHelperSwitchesBaseSpeedWhileControlling() throws Exception {
        EntityZombie zombie = ReinstallTest.allocate(EntityZombie.class);
        zombie.getAttributeMap().registerAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23);
        BaritoneMoveHelper move = new BaritoneMoveHelper(zombie, null, NpcbConfig.SpeedMode.OWN);
        move.setSpeedMode(NpcbConfig.SpeedMode.PLAYER);
        assertEquals("brez vodenja nespremenjeno", 0.23, base(zombie), 1e-9);

        move.takeControl();
        assertEquals("player med vodenjem", BaritoneMoveHelper.PLAYER_BASE_SPEED, base(zombie), 1e-9);
        move.setSpeedMode(NpcbConfig.SpeedMode.OWN);
        assertEquals("own vrne NPC-jevo", 0.23, base(zombie), 1e-9);
        assertEquals(0.02F, zombie.jumpMovementFactor, 1e-6);
        move.setSpeedMode(NpcbConfig.SpeedMode.PLAYER);
        assertEquals("nazaj na player", BaritoneMoveHelper.PLAYER_BASE_SPEED, base(zombie), 1e-9);
        assertEquals(NpcbConfig.SpeedMode.PLAYER, move.speedMode());
    }

    private static double base(EntityZombie z) {
        return z.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue();
    }
}
