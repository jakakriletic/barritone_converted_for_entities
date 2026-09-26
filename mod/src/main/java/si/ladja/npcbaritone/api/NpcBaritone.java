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

package si.ladja.npcbaritone.api;

import net.minecraft.entity.EntityLiving;

import javax.annotation.Nullable;

/**
 * Vstopna točka javnega API NPC Baritone, <b>različica {@value #API_VERSION}</b> (M6.7).
 *
 * <pre>{@code
 * if (NpcBaritone.available()) {
 *     INpcNavigator nav = NpcBaritone.attach(npc, "default");   // vanilla AI ostane
 *     nav.addListener(new NavListener() {
 *         public void onArrived(EntityLiving e) { ... }
 *     });
 *     nav.goTo(new BlockPos(100, 64, -20));
 * }
 * }</pre>
 *
 * <p>Porabnik prevaja proti {@code npcbaritone-<ver>-api.jar} ({@code compileOnly}); ob zagonu
 * brez moda {@link #available()} vrne {@code false} in vse ostalo {@code null}/{@code false}.
 * Pripenjanje zamenja {@code navigator}, {@code moveHelper} in {@code jumpHelper} entitete
 * (D-008); AI taski ostanejo in delujejo prek adapterja (D-018). Samo strežniška nit.
 */
public final class NpcBaritone {

    public static final int API_VERSION = 1;

    private static volatile INpcBaritoneProvider provider;

    private NpcBaritone() {
    }

    /** Kliče mod {@code npcbaritone} ob inicializaciji; porabniki tega ne kličejo. */
    public static void setProvider(INpcBaritoneProvider p) {
        provider = p;
    }

    public static boolean available() {
        return provider != null;
    }

    public static int apiVersion() {
        return API_VERSION;
    }

    /**
     * Pripne Baritona (ali vrne obstoječi navigator).
     *
     * @return null, če mod ni naložen, entiteta ni podprta (D-019) ali profil ne obstaja
     */
    @Nullable
    public static INpcNavigator attach(EntityLiving entity, String profile) {
        INpcBaritoneProvider p = provider;
        return p == null ? null : p.attach(entity, profile);
    }

    @Nullable
    public static INpcNavigator get(EntityLiving entity) {
        INpcBaritoneProvider p = provider;
        return p == null ? null : p.get(entity);
    }

    public static boolean detach(EntityLiving entity) {
        INpcBaritoneProvider p = provider;
        return p != null && p.detach(entity);
    }

    public static boolean supports(EntityLiving entity) {
        INpcBaritoneProvider p = provider;
        return p != null && p.supports(entity);
    }
}
