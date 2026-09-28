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

package si.ladja.npcbaritone.api.work;

import com.mojang.authlib.GameProfile;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * API 3 (D-030): kar porabnik poda ob registraciji workerja. Inventar in območje sta obvezna
 * (brez območja plast 2 ni aktivna, D-033). Nespremenljivo.
 *
 * <pre>{@code
 * WorkerSpec spec = WorkerSpec.builder()
 *     .inventory(chestHandler)                       // D-034: edini vir resnice
 *     .area(WorkArea.box(corner1, corner2))          // D-033
 *     .owner(captainProfile)                         // D-044: roke dobijo izpeljan UUID
 *     .permission((pos, state) -> !ship.isHull(pos)) // varno za niti
 *     .build();
 * }</pre>
 */
public final class WorkerSpec {

    private final IItemHandler inventory;
    private final WorkArea area;
    @Nullable
    private final GameProfile owner;
    private final IWorkPermission permission;
    private final String profile;

    private WorkerSpec(Builder b) {
        this.inventory = b.inventory;
        this.area = b.area;
        this.owner = b.owner;
        this.permission = b.permission;
        this.profile = b.profile;
    }

    public static Builder builder() {
        return new Builder();
    }

    public IItemHandler inventory() {
        return inventory;
    }

    public WorkArea area() {
        return area;
    }

    /** Lastnik (npr. kapitan ladje); roke delujejo pod izpeljano identiteto (D-044). */
    @Nullable
    public GameProfile owner() {
        return owner;
    }

    public IWorkPermission permission() {
        return permission;
    }

    /** Profil navigacije (D-016). Od M11.6 privzeto {@code worker} (D-031). */
    public String profile() {
        return profile;
    }

    public static final class Builder {
        private IItemHandler inventory;
        private WorkArea area;
        private GameProfile owner;
        private IWorkPermission permission = IWorkPermission.AREA_ONLY;
        private String profile = "default";

        private Builder() {
        }

        public Builder inventory(IItemHandler inventory) {
            this.inventory = inventory;
            return this;
        }

        public Builder area(WorkArea area) {
            this.area = area;
            return this;
        }

        public Builder owner(@Nullable GameProfile owner) {
            this.owner = owner;
            return this;
        }

        public Builder permission(IWorkPermission permission) {
            this.permission = permission == null ? IWorkPermission.AREA_ONLY : permission;
            return this;
        }

        public Builder profile(String profile) {
            this.profile = profile == null ? "default" : profile;
            return this;
        }

        /** @throws IllegalStateException brez inventarja ali območja */
        public WorkerSpec build() {
            if (inventory == null) {
                throw new IllegalStateException("WorkerSpec: inventory je obvezen (D-034)");
            }
            if (area == null || area.boxes().isEmpty()) {
                throw new IllegalStateException("WorkerSpec: area je obvezno (D-033)");
            }
            return new WorkerSpec(this);
        }
    }
}
