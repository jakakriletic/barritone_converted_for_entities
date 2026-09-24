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

import net.minecraftforge.common.config.Configuration;

import java.util.Locale;

/**
 * {@code config/npcbaritone.cfg} (01-ARHITEKTURA §6). Nespremenljiv posnetek vrednosti.
 *
 * <p>Nesmiselne vrednosti se obrežejo na mejo, ne zavrnejo (vzorec {@code FleetCommandLimits}
 * iz ladja_mod). V M0 se vrednosti samo preberejo; uporabljati jih začnejo M2 (movement),
 * M3 (debug) in M5 (search). Sekcija {@code profile} je prazna do M1 (D-016).
 */
public final class NpcbConfig {

    public static final String CAT_SEARCH = "search";
    public static final String CAT_PROFILE = "profile";
    public static final String CAT_MOVEMENT = "movement";
    public static final String CAT_DEBUG = "debug";

    public enum SpeedMode { PLAYER, OWN }

    public final int searchThreads;
    public final int searchQueueLimit;
    public final int snapshotMarginChunks;
    public final int shareRadiusChunks;
    public final SpeedMode speedMode;
    public final int maxTurnDegrees;
    public final boolean syncPathsToOps;

    NpcbConfig(int searchThreads, int searchQueueLimit, int snapshotMarginChunks, int shareRadiusChunks,
               SpeedMode speedMode, int maxTurnDegrees, boolean syncPathsToOps) {
        this.searchThreads = clamp(searchThreads, 1, 8);
        this.searchQueueLimit = clamp(searchQueueLimit, 8, 1024);
        this.snapshotMarginChunks = clamp(snapshotMarginChunks, 2, 32);
        this.shareRadiusChunks = clamp(shareRadiusChunks, 0, 4);
        this.speedMode = speedMode == null ? SpeedMode.PLAYER : speedMode;
        this.maxTurnDegrees = clamp(maxTurnDegrees, 5, 180);
        this.syncPathsToOps = syncPathsToOps;
    }

    public static NpcbConfig defaults() {
        return new NpcbConfig(2, 64, 8, 1, SpeedMode.PLAYER, 30, false);
    }

    /** Prebere (in po potrebi ustvari) datoteko. Manjkajoči ključi dobijo privzete vrednosti. */
    public static NpcbConfig load(Configuration cfg) {
        NpcbConfig d = defaults();
        cfg.load();
        cfg.setCategoryComment(CAT_SEARCH, "Iskalne niti, vrsta in posnetek chunkov (D-013, D-017). Uporablja se od M5.");
        cfg.setCategoryComment(CAT_PROFILE, "Profili nastavitev Baritona na instanco (D-016). Ključi pridejo v M1.");
        cfg.setCategoryComment(CAT_MOVEMENT, "Vhodi, hitrost in obrat telesa (D-010, D-011). Uporablja se od M2.");
        cfg.setCategoryComment(CAT_DEBUG, "Diagnostika (M3).");
        int threads = cfg.getInt("threads", CAT_SEARCH, d.searchThreads, 1, 8, "Število iskalnih niti.");
        int queue = cfg.getInt("queueLimit", CAT_SEARCH, d.searchQueueLimit, 8, 1024, "Največ čakajočih iskanj.");
        int margin = cfg.getInt("snapshotMarginChunks", CAT_SEARCH, d.snapshotMarginChunks, 2, 32, "Rob posnetka chunkov okoli začetka in cilja.");
        int share = cfg.getInt("shareRadiusChunks", CAT_SEARCH, d.shareRadiusChunks, 0, 4, "Iskanje se deli, če se začetka razlikujeta za največ toliko chunkov.");
        String speed = cfg.getString("speedMode", CAT_MOVEMENT, "player", "player = kot igralec (sprint, skok), own = lastna hitrost entitete.", new String[]{"player", "own"});
        int turn = cfg.getInt("maxTurnDegrees", CAT_MOVEMENT, d.maxTurnDegrees, 5, 180, "Največji obrat telesa v stopinjah na tick.");
        boolean sync = cfg.getBoolean("syncPathsToOps", CAT_DEBUG, d.syncPathsToOps, "Pošlji poti operaterjem z modom na klientu (debug prikaz).");
        if (cfg.hasChanged()) {
            cfg.save();
        }
        return new NpcbConfig(threads, queue, margin, share, parseSpeedMode(speed), turn, sync);
    }

    static SpeedMode parseSpeedMode(String value) {
        if (value == null) {
            return SpeedMode.PLAYER;
        }
        return "own".equals(value.trim().toLowerCase(Locale.ROOT)) ? SpeedMode.OWN : SpeedMode.PLAYER;
    }

    static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    @Override
    public String toString() {
        return "config{threads=" + searchThreads + ", queueLimit=" + searchQueueLimit
                + ", snapshotMargin=" + snapshotMarginChunks + ", shareRadius=" + shareRadiusChunks
                + ", speedMode=" + speedMode.name().toLowerCase(Locale.ROOT)
                + ", maxTurn=" + maxTurnDegrees + ", syncPathsToOps=" + syncPathsToOps + "}";
    }
}
