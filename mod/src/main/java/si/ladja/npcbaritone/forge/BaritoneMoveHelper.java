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

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.utils.input.Input;
import si.ladja.npcbaritone.core.utils.InputOverrideHandler;
import si.ladja.npcbaritone.core.pathing.path.PathExecutor;

/**
 * D-010: edina točka, ki piše gibalne vhode entitete, dokler vodi Baritone.
 *
 * <p>Vrstni red je ključen: {@code setAIMoveSpeed} pokliče tudi {@code setMoveForward}, zato
 * pride pred njim. Sneak pomnoži vhoda z 0,3 (kot {@code MovementInputFromOptions}); sprint
 * gre prek {@code setSprinting} (vanilla +30 % na atribut). V načinu "kot igralec" je osnovni
 * {@code MOVEMENT_SPEED} med vodenjem 0,1 in {@code jumpMovementFactor} kot pri igralcu
 * (0,02, +0,006 v sprintu); ob koncu vodenja se vse vrne. V načinu "lastna hitrost" (D-042)
 * hodi kot vanilla: atribut × hitrost zahteve navigatorja, brez sprinta. Ko Baritone ne vodi, dela vanilla.
 */
public class BaritoneMoveHelper extends EntityMoveHelper {

    /** Igralčeva osnovna hitrost ({@code EntityPlayer.applyEntityAttributes}). */
    public static final double PLAYER_BASE_SPEED = 0.1D;
    private static final float SPEED_IN_AIR = 0.02F;

    private final Baritone baritone;
    private NpcbConfig.SpeedMode speedMode;

    private boolean controlling;
    private double savedBaseSpeed = Double.NaN;

    static {
        si.ladja.npcbaritone.core.PerfProfile.label(BaritoneMoveHelper.class, "update (skupaj)");
    }

    public BaritoneMoveHelper(EntityLiving entity, Baritone baritone, NpcbConfig.SpeedMode speedMode) {
        super(entity);
        this.baritone = baritone;
        this.speedMode = speedMode;
    }

    @Override
    public void onUpdateMoveHelper() {
        long t0 = System.nanoTime();
        long p = si.ladja.npcbaritone.core.PerfProfile.start();
        try {
            update();
        } finally {
            PerfMeter.INSTANCE.add(System.nanoTime() - t0); // M5.6
            si.ladja.npcbaritone.core.PerfProfile.add(BaritoneMoveHelper.class, 0, p);
        }
    }

    private void update() {
        InputOverrideHandler in = baritone.getInputOverrideHandler();
        if (!in.isInControl()) {
            if (controlling) {
                release();
            }
            super.onUpdateMoveHelper();
            return;
        }
        if (!controlling) {
            takeControl();
        }
        boolean sneak = in.isInputForcedDown(Input.SNEAK);
        // PathExecutor porabi in počisti vhod SPRINT, odločitev pa shrani za tekoči tick.
        PathExecutor path = baritone.getPathingBehavior().getCurrent();
        // M6.5: task s hitrostjo ≤ 1,0 (napad, tavanje) hodi; šprint samo nad 1,0 ali brez taska
        BaritonePathNavigate nav = entity.getNavigator() instanceof BaritonePathNavigate
                ? (BaritonePathNavigate) entity.getNavigator() : null;
        boolean navAllows = nav == null || nav.allowsSprint();
        boolean sprint = !sneak && sprintAllowed(speedMode, navAllows)
                && ((path != null && path.isSprinting()) || in.isInputForcedDown(Input.SPRINT));
        float forward = axis(in.isInputForcedDown(Input.MOVE_FORWARD), in.isInputForcedDown(Input.MOVE_BACK), sneak);
        float strafe = axis(in.isInputForcedDown(Input.MOVE_LEFT), in.isInputForcedDown(Input.MOVE_RIGHT), sneak);
        entity.setSneaking(sneak);
        entity.setSprinting(sprint); // najprej: spremeni atribut
        float appliedSpeed = moveSpeed(speedMode, speed().getAttributeValue(), nav == null ? 0 : nav.requestedSpeed());
        entity.setAIMoveSpeed(appliedSpeed); // EntityLiving nastavi tudi moveForward = appliedSpeed
        entity.setMoveForward(forwardInput(speedMode, forward, appliedSpeed));
        entity.setMoveStrafing(strafe);
        if (speedMode == NpcbConfig.SpeedMode.PLAYER) {
            entity.jumpMovementFactor = sprint ? SPEED_IN_AIR * 1.3F : SPEED_IN_AIR;
        }
        this.action = Action.WAIT; // vanilla ciljanje ne sme prevzeti ob naslednjem super
    }

    /**
     * D-042 (M7.6): hitrost gibanja. {@code own} kot vanilla {@code EntityMoveHelper}: atribut ×
     * hitrost zahteve navigatorja ({@code tryMoveTo*(…, speed)}; 0 = API/ukaz = 1,0).
     * {@code player} (D-010): atribut, ki je med vodenjem igralčeva osnova — cene veljajo.
     */
    static float moveSpeed(NpcbConfig.SpeedMode mode, double attribute, double requestedSpeed) {
        if (mode == NpcbConfig.SpeedMode.OWN && requestedSpeed > 0) {
            return (float) (attribute * requestedSpeed);
        }
        return (float) attribute;
    }

    /** Vanilla {@code EntityMoveHelper.MOVE_TO} pusti {@code moveForward = setAIMoveSpeed(speed)}.
     * V {@code own} ohranimo ta vhod; {@code player} uporablja Baritonov vhod ±1. */
    static float forwardInput(NpcbConfig.SpeedMode mode, float axis, float appliedSpeed) {
        return mode == NpcbConfig.SpeedMode.OWN ? axis * appliedSpeed : axis;
    }

    /**
     * D-042: sprint samo v načinu {@code player} (in če ga task dovoli, M6.5). V {@code own} bi
     * sprint (+30 %) NPC-ja pospešil čez vanilla hitrost, ki jo je porabnik nastavil.
     */
    static boolean sprintAllowed(NpcbConfig.SpeedMode mode, boolean navAllows) {
        return mode == NpcbConfig.SpeedMode.PLAYER && navAllows;
    }

    /**
     * Ena os vhoda kot pri {@code MovementInputFromOptions}: +1 / −1 / 0, sneak ×0,3.
     * Levo je pozitiven strafe (vanilla).
     */
    static float axis(boolean positive, boolean negative, boolean sneak) {
        float v = (positive ? 1 : 0) - (negative ? 1 : 0);
        return sneak ? v * 0.3F : v;
    }

    /** true, dokler Baritone vodi entiteto (za telemetrijo in teste). */
    public boolean isControlling() {
        return controlling;
    }

    /** Vrne entiteti vanilla stanje; kliče ga tudi {@link Attach#detach}. */
    public void release() {
        controlling = false;
        entity.setSprinting(false);
        entity.setSneaking(false);
        entity.setMoveForward(0);
        entity.setMoveStrafing(0);
        entity.jumpMovementFactor = SPEED_IN_AIR;
        if (!Double.isNaN(savedBaseSpeed)) {
            speed().setBaseValue(savedBaseSpeed);
            savedBaseSpeed = Double.NaN;
        }
    }

    /**
     * D-039 (CNPC U6): preklop načina hitrosti instance. Med vodenjem se osnovni
     * {@code MOVEMENT_SPEED} takoj vrne (v {@code own}) ali nastavi na igralčevega (v {@code player}).
     */
    public void setSpeedMode(NpcbConfig.SpeedMode mode) {
        if (mode == null || mode == speedMode) {
            return;
        }
        if (controlling) {
            if (speedMode == NpcbConfig.SpeedMode.PLAYER) {
                entity.jumpMovementFactor = SPEED_IN_AIR;
                if (!Double.isNaN(savedBaseSpeed)) {
                    speed().setBaseValue(savedBaseSpeed);
                    savedBaseSpeed = Double.NaN;
                }
            } else if (mode == NpcbConfig.SpeedMode.PLAYER) {
                IAttributeInstance attr = speed();
                savedBaseSpeed = attr.getBaseValue();
                attr.setBaseValue(PLAYER_BASE_SPEED);
            }
        }
        speedMode = mode;
    }

    public NpcbConfig.SpeedMode speedMode() {
        return speedMode;
    }

    void takeControl() {
        controlling = true;
        if (speedMode == NpcbConfig.SpeedMode.PLAYER) {
            IAttributeInstance attr = speed();
            savedBaseSpeed = attr.getBaseValue();
            attr.setBaseValue(PLAYER_BASE_SPEED);
        }
    }

    private IAttributeInstance speed() {
        return entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
    }
}
