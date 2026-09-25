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
 * (0,02, +0,006 v sprintu); ob koncu vodenja se vse vrne. Ko Baritone ne vodi, dela vanilla.
 */
public class BaritoneMoveHelper extends EntityMoveHelper {

    /** Igralčeva osnovna hitrost ({@code EntityPlayer.applyEntityAttributes}). */
    public static final double PLAYER_BASE_SPEED = 0.1D;
    private static final float SPEED_IN_AIR = 0.02F;

    private final Baritone baritone;
    private final NpcbConfig.SpeedMode speedMode;

    private boolean controlling;
    private double savedBaseSpeed = Double.NaN;

    public BaritoneMoveHelper(EntityLiving entity, Baritone baritone, NpcbConfig.SpeedMode speedMode) {
        super(entity);
        this.baritone = baritone;
        this.speedMode = speedMode;
    }

    @Override
    public void onUpdateMoveHelper() {
        long t0 = System.nanoTime();
        try {
            update();
        } finally {
            PerfMeter.INSTANCE.add(System.nanoTime() - t0); // M5.6
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
        boolean navAllows = !(entity.getNavigator() instanceof BaritonePathNavigate)
                || ((BaritonePathNavigate) entity.getNavigator()).allowsSprint();
        boolean sprint = !sneak && navAllows && ((path != null && path.isSprinting()) || in.isInputForcedDown(Input.SPRINT));
        float forward = axis(in.isInputForcedDown(Input.MOVE_FORWARD), in.isInputForcedDown(Input.MOVE_BACK), sneak);
        float strafe = axis(in.isInputForcedDown(Input.MOVE_LEFT), in.isInputForcedDown(Input.MOVE_RIGHT), sneak);
        entity.setSneaking(sneak);
        entity.setSprinting(sprint); // najprej: spremeni atribut
        entity.setAIMoveSpeed((float) speed().getAttributeValue()); // pokliče tudi setMoveForward
        entity.setMoveForward(forward);
        entity.setMoveStrafing(strafe);
        if (speedMode == NpcbConfig.SpeedMode.PLAYER) {
            entity.jumpMovementFactor = sprint ? SPEED_IN_AIR * 1.3F : SPEED_IN_AIR;
        }
        this.action = Action.WAIT; // vanilla ciljanje ne sme prevzeti ob naslednjem super
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

    private void takeControl() {
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
