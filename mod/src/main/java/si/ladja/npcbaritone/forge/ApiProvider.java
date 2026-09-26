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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import si.ladja.npcbaritone.api.INpcBaritoneProvider;
import si.ladja.npcbaritone.api.INpcNavigator;
import si.ladja.npcbaritone.api.NavListener;
import si.ladja.npcbaritone.api.NavState;
import si.ladja.npcbaritone.core.api.pathing.goals.Goal;
import si.ladja.npcbaritone.core.api.pathing.goals.GoalBlock;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * M6.7: izvedba javnega API ({@link si.ladja.npcbaritone.api.NpcBaritone}). Ročaji so vezani na
 * entiteto (šibko); po {@code detach} ročaj ne dela več ({@code state()} = IDLE, ukazi {@code false}).
 */
public final class ApiProvider implements INpcBaritoneProvider {

    public static final ApiProvider INSTANCE = new ApiProvider();

    private final Map<EntityLiving, Handle> handles = new WeakHashMap<>();

    private ApiProvider() {
    }

    @Override
    public synchronized INpcNavigator attach(EntityLiving entity, String profile) {
        if (entity == null || entity.world.isRemote || !supports(entity)) {
            return null;
        }
        if (Attach.get(entity) == null) {
            try {
                Attach.attach(entity, false, NpcBaritoneMod.config(), profile == null ? NpcbConfig.DEFAULT_PROFILE : profile);
            } catch (IllegalArgumentException ex) {
                NpcBaritoneMod.LOG.warn("NpcBaritone.attach({}, {}): {}", entity, profile, ex.getMessage());
                return null;
            }
        }
        return get(entity);
    }

    @Override
    public synchronized INpcNavigator get(EntityLiving entity) {
        if (entity == null || Attach.get(entity) == null) {
            return null;
        }
        return handles.computeIfAbsent(entity, Handle::new);
    }

    @Override
    public synchronized boolean detach(EntityLiving entity) {
        handles.remove(entity);
        return Attach.detach(entity);
    }

    @Override
    public boolean supports(EntityLiving entity) {
        return entity != null && BaritonePathNavigate.fits(entity.width, entity.height);
    }

    static NavState map(NavStatus.State s) {
        return NavState.valueOf(s.name());
    }

    private static final class Handle implements INpcNavigator {
        private final EntityLiving entity;

        Handle(EntityLiving entity) {
            this.entity = entity;
        }

        private BaritonePathNavigate nav() {
            return entity.getNavigator() instanceof BaritonePathNavigate && Attach.get(entity) != null
                    ? (BaritonePathNavigate) entity.getNavigator() : null;
        }

        @Override
        public EntityLiving entity() {
            return entity;
        }

        @Override
        public boolean goTo(BlockPos pos) {
            BaritonePathNavigate n = nav();
            return n != null && pos != null && entity.world.isBlockLoaded(pos) && n.goTo(new GoalBlock(pos));
        }

        @Override
        public boolean goTo(Goal goal) {
            BaritonePathNavigate n = nav();
            return n != null && goal != null && n.goTo(goal);
        }

        @Override
        public boolean follow(Entity target, int range) {
            BaritonePathNavigate n = nav();
            return n != null && n.follow(target, range);
        }

        @Override
        public void stop() {
            BaritonePathNavigate n = nav();
            if (n != null) {
                n.stopNow();
            }
        }

        @Override
        public NavState state() {
            BaritonePathNavigate n = nav();
            return n == null ? NavState.IDLE : map(n.status().state());
        }

        @Override
        public String failReason() {
            BaritonePathNavigate n = nav();
            return n == null ? "" : n.status().failReason();
        }

        @Override
        public String profile() {
            String p = Attach.profile(entity);
            return p == null ? "" : p;
        }

        @Override
        public boolean setProfile(String profile) {
            try {
                return Attach.setProfile(entity, NpcBaritoneMod.config(), profile);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        @Override
        public void addListener(NavListener listener) {
            BaritonePathNavigate n = nav();
            if (n != null) {
                n.addListener(listener);
            }
        }

        @Override
        public void removeListener(NavListener listener) {
            BaritonePathNavigate n = nav();
            if (n != null) {
                n.removeListener(listener);
            }
        }
    }
}
