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

package si.ladja.npcbaritone.core.api;

import si.ladja.npcbaritone.core.api.behavior.ILookBehavior;
import si.ladja.npcbaritone.core.api.behavior.IPathingBehavior;
import si.ladja.npcbaritone.core.api.event.listener.IEventBus;
import si.ladja.npcbaritone.core.api.pathing.calc.IPathingControlManager;
import si.ladja.npcbaritone.core.api.process.ICustomGoalProcess;
import si.ladja.npcbaritone.core.api.utils.IEntityContext;
import si.ladja.npcbaritone.core.api.utils.IInputOverrideHandler;

/**
 * Ena instanca Baritona na eno entiteto. NPC Baritone (D-020): samo iskanje in izvajanje
 * poti do cilja; brez rudarjenja, gradnje, farmanja, raziskovanja, sledenja, elytre,
 * ukazov in izbir.
 *
 * @author Brady
 * @since 9/29/2018
 */
public interface IBaritone {

    /**
     * @return The {@link IPathingBehavior} instance
     * @see IPathingBehavior
     */
    IPathingBehavior getPathingBehavior();

    /**
     * @return The {@link ILookBehavior} instance
     * @see ILookBehavior
     */
    ILookBehavior getLookBehavior();

    /**
     * @return The {@link ICustomGoalProcess} instance
     * @see ICustomGoalProcess
     */
    ICustomGoalProcess getCustomGoalProcess();

    /**
     * @return The {@link IPathingControlManager} for this {@link IBaritone}
     */
    IPathingControlManager getPathingControlManager();

    /**
     * @return The {@link IInputOverrideHandler} instance
     * @see IInputOverrideHandler
     */
    IInputOverrideHandler getInputOverrideHandler();

    /**
     * @return The {@link IEntityContext} instance
     * @see IEntityContext
     */
    IEntityContext getEntityContext();

    /**
     * @return The {@link IEventBus} instance
     * @see IEventBus
     */
    IEventBus getGameEventHandler();

    /**
     * Nastavitve te instance (D-016). Do M1.8 so vse instance na istem globalnem profilu.
     */
    Settings getSettings();
}
