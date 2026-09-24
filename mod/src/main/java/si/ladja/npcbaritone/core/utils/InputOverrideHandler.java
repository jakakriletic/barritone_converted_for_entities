/*
 * This file is part of Baritone.
 * Modified for NPC Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package si.ladja.npcbaritone.core.utils;

import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.utils.IInputOverrideHandler;
import si.ladja.npcbaritone.core.api.utils.input.Input;
import si.ladja.npcbaritone.core.behavior.Behavior;

import java.util.EnumMap;
import java.util.Map;

/**
 * NPC Baritone (D-010): samo stanje vhodov, ki ga premiki nastavijo v ticku. Na entiteto
 * ga preslika forge plast ({@code BaritoneMoveHelper}/{@code BaritoneJumpHelper}, M2) —
 * brez {@code MovementInput}, brez klikov miške, brez rušenja/postavljanja (D-015).
 *
 * @author Brady
 * @since 7/31/2018
 */
public final class InputOverrideHandler extends Behavior implements IInputOverrideHandler {

    /**
     * Maps inputs to whether or not we are forcing their state down.
     */
    private final Map<Input, Boolean> inputForceStateMap = new EnumMap<>(Input.class);

    public InputOverrideHandler(Baritone baritone) {
        super(baritone);
    }

    /**
     * Returns whether or not we are forcing down the specified {@link Input}.
     *
     * @param input The input
     * @return Whether or not it is being forced down
     */
    @Override
    public final boolean isInputForcedDown(Input input) {
        return input == null ? false : this.inputForceStateMap.getOrDefault(input, false);
    }

    /**
     * Sets whether or not the specified {@link Input} is being forced down.
     *
     * @param input  The {@link Input}
     * @param forced Whether or not the state is being forced
     */
    @Override
    public final void setInputForceState(Input input, boolean forced) {
        this.inputForceStateMap.put(input, forced);
    }

    /**
     * Clears the override state for all keys
     */
    @Override
    public final void clearAllKeys() {
        this.inputForceStateMap.clear();
    }

    /**
     * @return true, če kateri od gibalnih vhodov trenutno velja (forge plast vodi entiteto)
     */
    public boolean isInControl() {
        for (Input input : new Input[]{Input.MOVE_FORWARD, Input.MOVE_BACK, Input.MOVE_LEFT, Input.MOVE_RIGHT, Input.SNEAK, Input.JUMP}) {
            if (isInputForcedDown(input)) {
                return true;
            }
        }
        return baritone.getPathingBehavior().isPathing();
    }
}
