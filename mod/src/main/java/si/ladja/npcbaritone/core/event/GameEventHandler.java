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

package si.ladja.npcbaritone.core.event;

import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.event.events.BlockChangeEvent;
import si.ladja.npcbaritone.core.api.event.events.ChunkEvent;
import si.ladja.npcbaritone.core.api.event.events.PathEvent;
import si.ladja.npcbaritone.core.api.event.events.PlayerUpdateEvent;
import si.ladja.npcbaritone.core.api.event.events.TickEvent;
import si.ladja.npcbaritone.core.api.event.listener.IEventBus;
import si.ladja.npcbaritone.core.api.event.listener.IGameEventListener;
import si.ladja.npcbaritone.core.api.utils.Helper;
import si.ladja.npcbaritone.core.utils.BlockStateInterface;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NPC Baritone: brez predpomnilnika chunkov/regij (D-014) in brez klientskih dogodkov.
 * {@code baritone.bsi} je BSI nad živimi naloženimi chunki brez kopije (samo glavna nit);
 * Baritone na klientu je tu vsak tick kopiral celotno mapo chunkov.
 *
 * @author Brady
 * @since 7/31/2018
 */
public final class GameEventHandler implements IEventBus, Helper {

    private final Baritone baritone;

    private final List<IGameEventListener> listeners = new CopyOnWriteArrayList<>();

    public GameEventHandler(Baritone baritone) {
        this.baritone = baritone;
    }

    @Override
    public final void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.IN) {
            try {
                baritone.bsi = new BlockStateInterface(baritone.getEntityContext());
            } catch (Exception ex) {
                baritone.bsi = null;
            }
        } else {
            baritone.bsi = null;
        }

        listeners.forEach(l -> l.onTick(event));
    }

    @Override
    public void onPostTick(TickEvent event) {
        listeners.forEach(l -> l.onPostTick(event));
    }

    @Override
    public final void onPlayerUpdate(PlayerUpdateEvent event) {
        listeners.forEach(l -> l.onPlayerUpdate(event));
    }

    @Override
    public void onChunkEvent(ChunkEvent event) {
        listeners.forEach(l -> l.onChunkEvent(event));
    }

    @Override
    public void onBlockChange(BlockChangeEvent event) {
        listeners.forEach(l -> l.onBlockChange(event));
    }

    @Override
    public void onPlayerDeath() {
        listeners.forEach(IGameEventListener::onPlayerDeath);
    }

    @Override
    public void onPathEvent(PathEvent event) {
        listeners.forEach(l -> l.onPathEvent(event));
    }

    @Override
    public final void registerEventListener(IGameEventListener listener) {
        this.listeners.add(listener);
    }
}
