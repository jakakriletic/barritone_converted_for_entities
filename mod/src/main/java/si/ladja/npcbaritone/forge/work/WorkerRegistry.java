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

package si.ladja.npcbaritone.forge.work;

import net.minecraft.entity.EntityLiving;
import si.ladja.npcbaritone.api.INpcNavigator;
import si.ladja.npcbaritone.api.work.INpcWorker;
import si.ladja.npcbaritone.api.work.IWorkerListener;
import si.ladja.npcbaritone.api.work.WorkerSpec;
import si.ladja.npcbaritone.forge.ApiProvider;
import si.ladja.npcbaritone.forge.Attach;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * M11.2 (D-030): registrirani workerji. Okostje — ročaj hrani specifikacijo, poslušalce in stanje
 * premora; roke (M11.3), inventar (M11.4), varovala (M11.5) in prednost procesa (M11.7) se
 * priključijo nanj. Ročaj drži entiteto samo šibko, sicer vnos v {@link WeakHashMap} nikoli ne
 * bi izginil (R-25).
 */
public final class WorkerRegistry {

    public static final WorkerRegistry INSTANCE = new WorkerRegistry();

    private final Map<EntityLiving, Handle> workers = new WeakHashMap<>();

    private WorkerRegistry() {
    }

    /** Registrira ali zamenja {@code spec}; navigacija mora biti že pripeta. */
    public synchronized INpcWorker register(EntityLiving entity, WorkerSpec spec) {
        Handle h = workers.get(entity);
        if (h == null || h.released) {
            h = new Handle(entity, spec);
            workers.put(entity, h);
        } else {
            h.spec = spec;
        }
        return h;
    }

    @Nullable
    public synchronized INpcWorker get(EntityLiving entity) {
        Handle h = workers.get(entity);
        return h == null || !h.active() ? null : h;
    }

    public synchronized boolean release(EntityLiving entity) {
        Handle h = workers.remove(entity);
        if (h == null) {
            return false;
        }
        h.released = true;
        return true;
    }

    /** Za varovala in roke (M11.3+): specifikacija aktivnega workerja ali null. */
    @Nullable
    public synchronized WorkerSpec spec(EntityLiving entity) {
        Handle h = workers.get(entity);
        return h == null || !h.active() ? null : h.spec;
    }

    private static final class Handle implements INpcWorker {
        private final WeakReference<EntityLiving> entity;
        private final List<IWorkerListener> listeners = new CopyOnWriteArrayList<>();
        volatile WorkerSpec spec;
        volatile boolean paused;
        volatile boolean released;

        Handle(EntityLiving entity, WorkerSpec spec) {
            this.entity = new WeakReference<>(entity);
            this.spec = spec;
        }

        @Override
        public EntityLiving entity() {
            return entity.get();
        }

        @Override
        public INpcNavigator navigator() {
            EntityLiving e = entity.get();
            return e == null ? null : ApiProvider.INSTANCE.get(e);
        }

        @Override
        public WorkerSpec spec() {
            return spec;
        }

        @Override
        public void pause() {
            paused = true;
        }

        @Override
        public void resume() {
            paused = false;
        }

        @Override
        public boolean paused() {
            return paused;
        }

        @Override
        public boolean active() {
            EntityLiving e = entity.get();
            return !released && e != null && !e.isDead && Attach.get(e) != null;
        }

        @Override
        public void addListener(IWorkerListener listener) {
            if (listener != null && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        @Override
        public void removeListener(IWorkerListener listener) {
            listeners.remove(listener);
        }
    }
}
