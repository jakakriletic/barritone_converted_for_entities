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

package si.ladja.npcbaritone.core.utils.player;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.IBaritone;
import si.ladja.npcbaritone.core.api.utils.IEntityContext;
import si.ladja.npcbaritone.core.api.utils.IPlayerController;
import si.ladja.npcbaritone.core.api.utils.RayTraceUtils;
import si.ladja.npcbaritone.core.api.utils.Rotation;

/**
 * {@link IEntityContext} za eno {@link EntityLiving} na strežniku (D-006).
 *
 * @author Brady
 * @since 11/12/2018
 */
public final class EntityContext implements IEntityContext {

    private final Baritone baritone;
    private final EntityLiving entity;

    public EntityContext(Baritone baritone, EntityLiving entity) {
        this.baritone = baritone;
        this.entity = entity;
    }

    @Override
    public EntityLiving entity() {
        return this.entity;
    }

    @Override
    public IBaritone baritone() {
        return this.baritone;
    }

    @Override
    public IPlayerController playerController() {
        return () -> this.baritone.getSettings().blockReachDistance.value;
    }

    @Override
    public World world() {
        return this.entity.world;
    }

    @Override
    public Rotation entityRotations() {
        return this.baritone.getLookBehavior().getEffectiveRotation().orElseGet(IEntityContext.super::entityRotations);
    }

    @Override
    public RayTraceResult objectMouseOver() {
        return RayTraceUtils.rayTraceTowards(entity(), entityRotations(), playerController().getBlockReachDistance());
    }
}
