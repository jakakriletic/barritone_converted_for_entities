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

package si.ladja.npcbaritone.core.api.event.listener;

import si.ladja.npcbaritone.core.api.event.events.BlockChangeEvent;
import si.ladja.npcbaritone.core.api.event.events.ChunkEvent;
import si.ladja.npcbaritone.core.api.event.events.PathEvent;
import si.ladja.npcbaritone.core.api.event.events.PlayerUpdateEvent;
import si.ladja.npcbaritone.core.api.event.events.TickEvent;

/**
 * NPC Baritone (D-009): dogodke proži navigator entitete, ne klient. Klientski dogodki
 * (klepet, paketi, izris, svet, rotacija kamere, sprint, interakcija) so odstranjeni.
 *
 * @author Brady
 * @since 7/31/2018
 */
public interface IGameEventListener {

    /**
     * Run once per tick of the entity, before movement.
     */
    void onTick(TickEvent event);

    /**
     * Run once per tick of the entity, after movement.
     */
    void onPostTick(TickEvent event);

    /**
     * PRE: tik pred premikom entitete (vhodi in yaw); POST: takoj po njem.
     */
    void onPlayerUpdate(PlayerUpdateEvent event);

    /**
     * Chunk naložen ali odstranjen (M5: iz {@code ChunkEvent} na strežniku).
     */
    void onChunkEvent(ChunkEvent event);

    /**
     * Blok v svetu se je spremenil (M4/M5: iz {@code BlockEvent} na strežniku).
     */
    void onBlockChange(BlockChangeEvent event);

    /**
     * Entiteta je umrla.
     */
    void onPlayerDeath();

    /**
     * When the pathfinder's state changes
     */
    void onPathEvent(PathEvent event);
}
