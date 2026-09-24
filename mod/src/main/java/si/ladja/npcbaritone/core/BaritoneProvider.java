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

package si.ladja.npcbaritone.core;

import net.minecraft.entity.EntityLiving;
import si.ladja.npcbaritone.core.api.IBaritone;
import si.ladja.npcbaritone.core.api.IBaritoneProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Register instanc po entiteti s šibkimi ključi: instanca izgine z entiteto (M1.10).
 *
 * @author Brady
 * @since 9/29/2018
 */
public final class BaritoneProvider implements IBaritoneProvider {

    private final Map<EntityLiving, Baritone> byEntity = new WeakHashMap<>();

    @Override
    public synchronized List<IBaritone> getAllBaritones() {
        return new ArrayList<>(this.byEntity.values());
    }

    @Override
    public synchronized IBaritone getBaritone(EntityLiving entity) {
        return this.byEntity.get(entity);
    }

    @Override
    public synchronized IBaritone createBaritone(EntityLiving entity) {
        return this.byEntity.computeIfAbsent(entity, Baritone::new);
    }

    @Override
    public synchronized boolean destroyBaritone(IBaritone baritone) {
        if (!(baritone instanceof Baritone)) {
            return false;
        }
        Baritone b = (Baritone) baritone;
        if (this.byEntity.get(b.getEntityContext().entity()) != b) {
            return false;
        }
        b.getPathingBehavior().forceCancel();
        this.byEntity.remove(b.getEntityContext().entity());
        return true;
    }
}
