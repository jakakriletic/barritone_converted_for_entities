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

package si.ladja.npcbaritone.core.api.utils;

import net.minecraft.block.BlockSlab;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import si.ladja.npcbaritone.core.utils.BlockStateInterface;

import java.util.Optional;

/**
 * Kontekst ene entitete (D-006): {@link EntityLiving} na strežniku namesto klientskega igralca.
 * Svet se ne bere neposredno ({@code World.getBlockState} lahko naloži chunk, D-012),
 * ampak prek {@link BlockStateInterface}.
 *
 * @author Brady
 * @since 11/12/2018
 */
public interface IEntityContext {

    EntityLiving entity();

    /** Strežniški svet entitete ({@code WorldServer} v igri). */
    World world();

    IPlayerController playerController();

    /**
     * D-021: referenčni okvir iskanja. Do M9 vedno {@link IMovementFrame#IDENTITY}.
     */
    default IMovementFrame frame() {
        return IMovementFrame.IDENTITY;
    }

    RayTraceResult objectMouseOver();

    default BetterBlockPos feetPos() {
        // TODO find a better way to deal with soul sand!!!!!
        BetterBlockPos feet = new BetterBlockPos(entity().posX, entity().posY + 0.1251, entity().posZ);
        // NPC Baritone: brez try/catch NPE (klient) in brez World.getBlockState (D-012)
        if (BlockStateInterface.get(this, feet).getBlock() instanceof BlockSlab) {
            return feet.up();
        }
        return feet;
    }

    default Vec3d feetPosAsVec() {
        return new Vec3d(entity().posX, entity().posY, entity().posZ);
    }

    default Vec3d headPos() {
        return new Vec3d(entity().posX, entity().posY + entity().getEyeHeight(), entity().posZ);
    }

    default Vec3d playerMotion() {
        return new Vec3d(entity().motionX, entity().motionY, entity().motionZ);
    }

    default Rotation entityRotations() {
        return new Rotation(entity().rotationYaw, entity().rotationPitch);
    }

    static double eyeHeight(boolean ifSneaking) {
        return ifSneaking ? 1.54 : 1.62;
    }

    /**
     * Returns the block that the crosshair is currently placed over. Updated once per tick.
     *
     * @return The position of the highlighted block
     */
    default Optional<BlockPos> getSelectedBlock() {
        RayTraceResult result = objectMouseOver();
        if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
            return Optional.of(result.getBlockPos());
        }
        return Optional.empty();
    }

    default boolean isLookingAt(BlockPos pos) {
        return getSelectedBlock().equals(Optional.of(pos));
    }
}
