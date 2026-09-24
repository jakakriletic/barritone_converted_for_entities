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

import si.ladja.npcbaritone.core.BaritoneProvider;

/**
 * Globalni vhod v jedro. NPC Baritone: brez {@code ServiceLoader} in brez branja
 * {@code .minecraft/baritone/settings.txt}; privzete nastavitve so NPC profil (§8 raziskave,
 * D-016), ki ga forge plast lahko prepiše iz strežniškega configa.
 *
 * @author Brady
 * @since 9/23/2018
 */
public final class BaritoneAPI {

    private static final IBaritoneProvider provider;
    private static final Settings settings;

    static {
        settings = new Settings();
        NpcProfile.applyDefaults(settings);
        provider = new BaritoneProvider();
    }

    private BaritoneAPI() {
    }

    public static IBaritoneProvider getProvider() {
        return BaritoneAPI.provider;
    }

    public static Settings getSettings() {
        return BaritoneAPI.settings;
    }
}
