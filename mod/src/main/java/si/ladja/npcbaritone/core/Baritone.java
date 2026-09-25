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
import si.ladja.npcbaritone.core.api.BaritoneAPI;
import si.ladja.npcbaritone.core.api.IBaritone;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.api.behavior.IBehavior;
import si.ladja.npcbaritone.core.api.event.events.PlayerUpdateEvent;
import si.ladja.npcbaritone.core.api.event.events.TickEvent;
import si.ladja.npcbaritone.core.api.event.events.type.EventState;
import si.ladja.npcbaritone.core.api.event.listener.IEventBus;
import si.ladja.npcbaritone.core.api.process.IBaritoneProcess;
import si.ladja.npcbaritone.core.api.utils.IEntityContext;
import si.ladja.npcbaritone.core.behavior.InventoryBehavior;
import si.ladja.npcbaritone.core.behavior.LookBehavior;
import si.ladja.npcbaritone.core.behavior.PathingBehavior;
import si.ladja.npcbaritone.core.event.GameEventHandler;
import si.ladja.npcbaritone.core.process.CustomGoalProcess;
import si.ladja.npcbaritone.core.utils.BlockStateInterface;
import si.ladja.npcbaritone.core.utils.InputOverrideHandler;
import si.ladja.npcbaritone.core.utils.PathingControlManager;
import si.ladja.npcbaritone.core.utils.player.EntityContext;

import java.util.function.Function;

/**
 * Vitka instanca na entiteto (M1.10, D-020): PathingBehavior, LookBehavior, InputOverrideHandler,
 * CustomGoalProcess. Tiktaka jo navigator (D-009) prek {@link #tick()} in {@link #postTick()}.
 *
 * @author Brady
 * @since 7/31/2018
 */
public class Baritone implements IBaritone {

    private final GameEventHandler gameEventHandler;

    private final PathingBehavior pathingBehavior;
    private final LookBehavior lookBehavior;
    private final InventoryBehavior inventoryBehavior;
    private final InputOverrideHandler inputOverrideHandler;

    private final CustomGoalProcess customGoalProcess;

    private final PathingControlManager pathingControlManager;

    private final IEntityContext entityContext;

    public BlockStateInterface bsi;

    /** Profil nastavitev (D-016); privzeto skupni NPC profil {@link BaritoneAPI#getSettings()}. */
    private volatile Settings settings;

    private int tickCount;

    private boolean postTickPending;

    Baritone(EntityLiving entity) {
        this(entity, BaritoneAPI.getSettings());
    }

    Baritone(EntityLiving entity, Settings settings) {
        this.settings = java.util.Objects.requireNonNull(settings);
        this.gameEventHandler = new GameEventHandler(this);

        // Define this before behaviors try and get it, or else it will be null and the builds will fail!
        this.entityContext = new EntityContext(this, entity);

        {
            this.lookBehavior         = this.registerBehavior(LookBehavior::new);
            this.pathingBehavior      = this.registerBehavior(PathingBehavior::new);
            this.inventoryBehavior    = this.registerBehavior(InventoryBehavior::new);
            this.inputOverrideHandler = this.registerBehavior(InputOverrideHandler::new);
        }

        this.pathingControlManager = new PathingControlManager(this);
        {
            this.customGoalProcess = this.registerProcess(CustomGoalProcess::new); // very high iq
        }
    }

    /**
     * En tick na strežniški niti, pred premikom entitete (iz {@code PathNavigate.onUpdateNavigation},
     * D-009): obdelava poti, izbira premika, vhodi in ciljni yaw.
     */
    public void tick() {
        if (this.postTickPending) {
            // POST prejšnjega ticka: entiteta nima kljuke po travel(), zato ga sprožimo tu
            postTick();
        }
        this.postTickPending = true;
        TickEvent event = new TickEvent(EventState.PRE, TickEvent.Type.IN, this.tickCount++);
        this.gameEventHandler.onTick(event);
        this.gameEventHandler.onPlayerUpdate(new PlayerUpdateEvent(EventState.PRE));
    }

    /**
     * Po premiku entitete v istem ticku.
     */
    public void postTick() {
        this.postTickPending = false;
        this.gameEventHandler.onPlayerUpdate(new PlayerUpdateEvent(EventState.POST));
        this.gameEventHandler.onPostTick(new TickEvent(EventState.POST, TickEvent.Type.IN, this.tickCount - 1));
    }

    public void registerBehavior(IBehavior behavior) {
        this.gameEventHandler.registerEventListener(behavior);
    }

    public <T extends IBehavior> T registerBehavior(Function<Baritone, T> constructor) {
        final T behavior = constructor.apply(this);
        this.registerBehavior(behavior);
        return behavior;
    }

    public <T extends IBaritoneProcess> T registerProcess(Function<Baritone, T> constructor) {
        final T behavior = constructor.apply(this);
        this.pathingControlManager.registerProcess(behavior);
        return behavior;
    }

    @Override
    public PathingControlManager getPathingControlManager() {
        return this.pathingControlManager;
    }

    @Override
    public InputOverrideHandler getInputOverrideHandler() {
        return this.inputOverrideHandler;
    }

    @Override
    public CustomGoalProcess getCustomGoalProcess() {
        return this.customGoalProcess;
    }

    @Override
    public IEntityContext getEntityContext() {
        return this.entityContext;
    }

    public InventoryBehavior getInventoryBehavior() {
        return this.inventoryBehavior;
    }

    @Override
    public LookBehavior getLookBehavior() {
        return this.lookBehavior;
    }

    @Override
    public PathingBehavior getPathingBehavior() {
        return this.pathingBehavior;
    }

    @Override
    public IEventBus getGameEventHandler() {
        return this.gameEventHandler;
    }

    @Override
    public Settings getSettings() {
        return this.settings;
    }

    /**
     * Zamenja profil te instance (D-016). Velja za naslednje iskanje; trenutna pot se ne
     * preračuna.
     */
    public void setSettings(Settings settings) {
        this.settings = java.util.Objects.requireNonNull(settings);
    }

    /** M5.1: iskanja gredo prek {@link SearchExecutor#submit} (prednost, zgornja meja). */
    public static void submitSearch(Runnable search, long priority) {
        SearchExecutor.submit(search, priority);
    }
}
