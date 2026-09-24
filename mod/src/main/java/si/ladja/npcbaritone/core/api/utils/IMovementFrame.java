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

package si.ladja.npcbaritone.core.api.utils;

import net.minecraft.util.math.Vec3d;

/**
 * D-021: pretvorba med prostorom iskanja poti in svetom, v katerem entiteta stoji.
 * Do M9 je edina implementacija {@link #IDENTITY}; M9 doda ladijski okvir
 * (iskanje v shipyard koordinatah, izvedba prek {@code ShipTransform}).
 */
public interface IMovementFrame {

    IMovementFrame IDENTITY = new IMovementFrame() {
        @Override
        public Vec3d toWorld(Vec3d pathSpace) {
            return pathSpace;
        }

        @Override
        public Vec3d toPathSpace(Vec3d world) {
            return world;
        }

        @Override
        public float yawToWorld(float pathSpaceYaw) {
            return pathSpaceYaw;
        }

        @Override
        public boolean isIdentity() {
            return true;
        }
    };

    Vec3d toWorld(Vec3d pathSpace);

    Vec3d toPathSpace(Vec3d world);

    float yawToWorld(float pathSpaceYaw);

    default boolean isIdentity() {
        return false;
    }
}
