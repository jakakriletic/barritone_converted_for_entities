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

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityJumpHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.pathfinding.PathNavigate;
import si.ladja.npcbaritone.core.Baritone;
import si.ladja.npcbaritone.core.api.BaritoneAPI;
import si.ladja.npcbaritone.core.api.NpcProfile;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.api.utils.SettingsUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * D-008, D-025, M2.6: pripne Baritona na obstoječo entiteto brez mixinov — z refleksijo
 * zamenja {@code navigator}, {@code moveHelper} in {@code jumpHelper} ({@code protected} v
 * {@link EntityLiving}). {@code puppet} odstrani AI taske, da entiteto vodijo samo ukazi.
 * {@link #detach} vrne vse, kot je bilo.
 *
 * <p>Polja se iščejo po SRG imenu (izdan, obfuskiran jar) in po MCP imenu (razvojno okolje);
 * imena so iz MCP snapshot_20171003 {@code fields.csv} (R-14).
 */
public final class Attach {

    static final String[] NAVIGATOR = {"field_70699_by", "navigator"};
    static final String[] MOVE_HELPER = {"field_70765_h", "moveHelper"};
    static final String[] JUMP_HELPER = {"field_70767_i", "jumpHelper"};

    private static final Field F_NAVIGATOR = field(NAVIGATOR);
    private static final Field F_MOVE_HELPER = field(MOVE_HELPER);
    private static final Field F_JUMP_HELPER = field(JUMP_HELPER);

    /** Kar je bilo na entiteti pred pripenjanjem. */
    private static final class Saved {
        final PathNavigate navigator;
        final EntityMoveHelper moveHelper;
        final EntityJumpHelper jumpHelper;
        final List<EntityAITasks.EntityAITaskEntry> tasks = new ArrayList<>();
        final List<EntityAITasks.EntityAITaskEntry> targetTasks = new ArrayList<>();
        final boolean puppet;
        final Baritone baritone;
        final BaritoneMoveHelper ourMove;
        final NavStatus status;
        final EntityInteractions interactions;
        String profile;

        Saved(EntityLiving e, boolean puppet, Baritone baritone, BaritoneMoveHelper ourMove, String profile) {
            this.navigator = e.getNavigator();
            this.moveHelper = e.getMoveHelper();
            this.jumpHelper = e.getJumpHelper();
            this.puppet = puppet;
            this.baritone = baritone;
            this.ourMove = ourMove;
            this.status = NavStatus.install(baritone);
            this.interactions = new EntityInteractions(e, baritone);
            this.profile = profile;
        }
    }

    private static final Map<EntityLiving, Saved> ATTACHED = new WeakHashMap<>();

    private Attach() {
    }

    /**
     * Pripne Baritona (ali vrne obstoječega). Klicati na strežniški niti.
     *
     * @param puppet odstrani AI taske (in jih ob {@link #detach} vrne)
     */
    public static Baritone attach(EntityLiving entity, boolean puppet, NpcbConfig config) {
        return attach(entity, puppet, config, NpcbConfig.DEFAULT_PROFILE);
    }

    /**
     * Kot {@link #attach(EntityLiving, boolean, NpcbConfig)} s poimenovanim profilom (D-016, M3.1).
     * Pri že pripeti entiteti se profil ne spremeni (za to je {@link #setProfile}).
     *
     * @throws IllegalArgumentException neznan profil ali neveljaven prepis v configu
     */
    public static synchronized Baritone attach(EntityLiving entity, boolean puppet, NpcbConfig config, String profile) {
        if (entity.world.isRemote) {
            throw new IllegalStateException("attach on the server only (D-024)");
        }
        Saved existing = ATTACHED.get(entity);
        if (existing != null) {
            return existing.baritone;
        }
        Settings settings = profileFor(config, profile);
        Baritone baritone = (Baritone) BaritoneAPI.getProvider().createBaritone(entity);
        baritone.setSettings(settings);
        BaritoneMoveHelper move = new BaritoneMoveHelper(entity, baritone, config.speedMode);
        Saved saved = new Saved(entity, puppet, baritone, move, profile.toLowerCase(java.util.Locale.ROOT));
        if (puppet) {
            saved.tasks.addAll(entity.tasks.taskEntries);
            saved.targetTasks.addAll(entity.targetTasks.taskEntries);
            saved.tasks.forEach(t -> entity.tasks.removeTask(t.action));
            saved.targetTasks.forEach(t -> entity.targetTasks.removeTask(t.action));
        }
        BaritonePathNavigate ours = new BaritonePathNavigate(entity, entity.world, baritone, saved.interactions, saved.status);
        set(F_NAVIGATOR, entity, ours);
        // taski, ki so si navigator shranili v konstruktorju (EntityAIFollowOwner, EntityAIAvoidEntity, ...)
        rewireNavigators(actions(entity), saved.navigator, ours);
        set(F_MOVE_HELPER, entity, move);
        set(F_JUMP_HELPER, entity, new BaritoneJumpHelper(entity, baritone));
        ATTACHED.put(entity, saved);
        return baritone;
    }

    /** @return true, če je bila entiteta pripeta */
    public static synchronized boolean detach(EntityLiving entity) {
        Saved saved = ATTACHED.remove(entity);
        if (saved == null) {
            return false;
        }
        saved.baritone.getPathingBehavior().forceCancel();
        saved.baritone.getInputOverrideHandler().clearAllKeys();
        saved.ourMove.release();
        saved.interactions.release();
        entity.setJumping(false);
        rewireNavigators(actions(entity), entity.getNavigator(), saved.navigator);
        set(F_NAVIGATOR, entity, saved.navigator);
        set(F_MOVE_HELPER, entity, saved.moveHelper);
        set(F_JUMP_HELPER, entity, saved.jumpHelper);
        if (saved.puppet) {
            saved.tasks.forEach(t -> entity.tasks.addTask(t.priority, t.action));
            saved.targetTasks.forEach(t -> entity.targetTasks.addTask(t.priority, t.action));
        }
        BaritoneAPI.getProvider().destroyBaritone(saved.baritone);
        return true;
    }

    public static synchronized Baritone get(EntityLiving entity) {
        Saved saved = ATTACHED.get(entity);
        return saved == null ? null : saved.baritone;
    }

    public static synchronized NavStatus status(EntityLiving entity) {
        Saved saved = ATTACHED.get(entity);
        return saved == null ? null : saved.status;
    }

    public static synchronized EntityInteractions interactions(EntityLiving entity) {
        Saved saved = ATTACHED.get(entity);
        return saved == null ? null : saved.interactions;
    }

    public static synchronized String profile(EntityLiving entity) {
        Saved saved = ATTACHED.get(entity);
        return saved == null ? null : saved.profile;
    }

    /**
     * Zamenja profil pripete entitete. Tekoča pot se prekine (cene so se lahko spremenile);
     * cilj ostane, zato se iskanje začne znova.
     *
     * @return false, če entiteta ni pripeta
     * @throws IllegalArgumentException neznan profil
     */
    public static synchronized boolean setProfile(EntityLiving entity, NpcbConfig config, String profile) {
        Saved saved = ATTACHED.get(entity);
        if (saved == null) {
            return false;
        }
        Settings settings = profileFor(config, profile);
        saved.baritone.getPathingBehavior().softCancelIfSafe();
        saved.baritone.setSettings(settings);
        saved.profile = profile.toLowerCase(java.util.Locale.ROOT);
        return true;
    }

    public static synchronized boolean isPuppet(EntityLiving entity) {
        Saved saved = ATTACHED.get(entity);
        return saved != null && saved.puppet;
    }

    public static synchronized List<EntityLiving> attached() {
        return new ArrayList<>(ATTACHED.keySet());
    }

    /** Nov profil instance: NPC privzete vrednosti + strežniški config (D-016). */
    static Settings profileFor(NpcbConfig config) {
        return config.applyTo(NpcProfile.create());
    }

    /**
     * Poimenovan profil: NPC privzete vrednosti + strežniški config + prepisi profila.
     * Prepisi pridejo zadnji, razen {@code allowParkour} v načinu hitrosti {@code own}
     * (D-010: cene skokov tam ne veljajo).
     *
     * @throws IllegalArgumentException neznan profil ali neveljaven prepis
     */
    static Settings profileFor(NpcbConfig config, String name) {
        String key = name == null ? NpcbConfig.DEFAULT_PROFILE : name.toLowerCase(java.util.Locale.ROOT);
        Map<String, String> overrides = config.profiles.get(key);
        if (overrides == null) {
            throw new IllegalArgumentException("neznan profil '" + name + "' (na voljo: " + config.profiles.keySet() + ")");
        }
        Settings s = profileFor(config);
        for (Map.Entry<String, String> o : overrides.entrySet()) {
            try {
                SettingsUtil.parseAndApply(s, o.getKey(), o.getValue());
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("profil '" + key + "': " + o.getKey() + "=" + o.getValue() + ": " + ex.getMessage(), ex);
            }
        }
        if (config.speedMode == NpcbConfig.SpeedMode.OWN) {
            s.allowParkour.value = false;
        }
        return s;
    }

    private static List<Object> actions(EntityLiving entity) {
        List<Object> out = new ArrayList<>();
        for (EntityAITasks.EntityAITaskEntry t : entity.tasks.taskEntries) {
            out.add(t.action);
        }
        for (EntityAITasks.EntityAITaskEntry t : entity.targetTasks.taskEntries) {
            out.add(t.action);
        }
        return out;
    }

    /**
     * Vanilla {@code EntityAIFollowOwner}, {@code EntityAIAvoidEntity} in {@code EntityAIFollow}
     * (in marsikateri modded task) si navigator shranijo v konstruktorju. Po zamenjavi bi
     * ukazovali navigatorju, ki ga nihče ne tiktaka (volk bi stal in se samo teleportiral).
     * Vsako nestatično polje tipa {@link PathNavigate}, ki kaže na {@code from}, preusmeri na
     * {@code to} — po vrednosti, ne po imenu, zato deluje z SRG in MCP imeni ter z modded taski.
     *
     * @return število preusmerjenih polj
     */
    static int rewireNavigators(Iterable<?> actions, PathNavigate from, PathNavigate to) {
        int n = 0;
        for (Object action : actions) {
            for (Class<?> c = action.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || !PathNavigate.class.isAssignableFrom(f.getType())
                            || !f.getType().isInstance(to)) {
                        continue;
                    }
                    try {
                        f.setAccessible(true);
                        if (f.get(action) == from) {
                            f.set(action, to);
                            n++;
                        }
                    } catch (IllegalAccessException | RuntimeException e) {
                        NpcBaritoneMod.LOG.warn("cannot rewire navigator in {}.{}: {}", c.getName(), f.getName(), e.toString());
                    }
                }
            }
        }
        return n;
    }

    private static Field field(String[] names) {
        for (String name : names) {
            try {
                Field f = EntityLiving.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // naslednje ime
            }
        }
        throw new IllegalStateException("EntityLiving field not found: " + String.join("/", names));
    }

    private static void set(Field f, EntityLiving entity, Object value) {
        try {
            f.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("cannot set " + f.getName(), e);
        }
    }
}
