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
import net.minecraft.entity.ai.EntityJumpHelper;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.utils.input.Input;
import si.ladja.npcbaritone.core.utils.InputOverrideHandler;

/** D-010: skok iz Baritonovega vhoda JUMP; ko Baritone ne vodi, vanilla. */
public class BaritoneJumpHelper extends EntityJumpHelper {

    private final EntityLiving entity;
    private final Baritone baritone;

    public BaritoneJumpHelper(EntityLiving entity, Baritone baritone) {
        super(entity);
        this.entity = entity;
        this.baritone = baritone;
    }

    @Override
    public void doJump() {
        InputOverrideHandler in = baritone.getInputOverrideHandler();
        if (!in.isInControl()) {
            super.doJump();
            return;
        }
        entity.setJumping(in.isInputForcedDown(Input.JUMP));
        this.isJumping = false; // morebitna vanilla zahteva iz taska se zavrže, vodi Baritone
    }
}
