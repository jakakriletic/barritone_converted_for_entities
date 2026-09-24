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

package si.ladja.npcbaritone.core.behavior;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.MathHelper;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.behavior.ILookBehavior;
import si.ladja.npcbaritone.core.api.event.events.PlayerUpdateEvent;
import si.ladja.npcbaritone.core.api.utils.Rotation;

import java.util.Optional;

/**
 * NPC Baritone (D-011): premik v ticku nastavi ciljno rotacijo; pred premikom entitete
 * (PRE) se telo obrne proti njej za največ {@code npcMaxTurnDegrees}, po premiku (POST)
 * se cilj pozabi. Glava ostane vanilla ({@code EntityLookHelper}).
 *
 * @author Brady
 * @since 8/1/2018
 */
public final class LookBehavior extends Behavior implements ILookBehavior {

    /**
     * Target's values are as follows:
     * <p>
     * getFirst() -> yaw
     * getSecond() -> pitch
     */
    private Rotation target;

    public LookBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void updateTarget(Rotation rotation, boolean blockInteract) {
        this.target = rotation;
    }

    @Override
    public Optional<Rotation> getTarget() {
        return Optional.ofNullable(this.target);
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.target == null) {
            return;
        }
        switch (event.getState()) {
            case PRE: {
                // D-011: telo (rotationYaw, ki ga bere travel, in renderYawOffset) se obrne proti
                // cilju za največ npcMaxTurnDegrees na tick; glava in pitch ostaneta vanilla.
                EntityLiving entity = ctx.entity();
                float max = baritone.getSettings().npcMaxTurnDegrees.value;
                float delta = MathHelper.wrapDegrees(this.target.getYaw() - entity.rotationYaw);
                if (delta > max) {
                    delta = max;
                } else if (delta < -max) {
                    delta = -max;
                }
                float yaw = entity.rotationYaw + delta;
                entity.rotationYaw = yaw;
                entity.renderYawOffset = yaw;
                break;
            }
            case POST: {
                // The target is done being used for this game tick, so it can be invalidated
                this.target = null;
                break;
            }
            default:
                break;
        }
    }

    /**
     * Ni prostega pogleda (freeLook): učinkovita rotacija je vedno dejanska rotacija entitete.
     */
    public Optional<Rotation> getEffectiveRotation() {
        return Optional.empty();
    }
}
