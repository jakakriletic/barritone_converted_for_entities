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
import si.ladja.npcbaritone.core.api.Settings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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
    public static final String CAT_WORKER = "worker";

    public enum SpeedMode { PLAYER, OWN }

    public final int searchThreads;
    public final int searchQueueLimit;
    public final int snapshotMarginChunks;
    public final int shareRadiusChunks;
    public final SpeedMode speedMode;
    public final int maxTurnDegrees;
    public final boolean syncPathsToOps;
    /**
     * M8/D-028: navigator ({@link BaritonePathNavigate}, API) vodi tudi entitete izven 1×2 (do
     * širine 3,0 in višine 4,0). Privzeto false = D-019, obnašanje porabnika se ne spremeni.
     * Ukazi in tečaji ({@code /npcb attach}, T3) velikosti ne preverjajo.
     */
    public final boolean largeEntities;
    /**
     * M7.10: {@code Settings.npcCrowdYield} — v gneči počakaj namesto rinjenja in preklica po
     * {@code movementTimeoutTicks}. Ključ {@code movement.crowdYield} ali sistemska lastnost
     * {@code npcbcrowdyield=true} (za A/B zagone brez urejanja configa). Privzeto false.
     */
    public final boolean crowdYield;
    /**
     * M11.2 (D-030): {@code worker.enabled} — {@code false} izklopi plast 2 in 3 v celoti
     * ({@code NpcBaritone.worker()} vrne {@code null}). Privzeto true.
     */
    public final boolean workerEnabled;
    /**
     * D-016, M3.1: poimenovani profili — ime → prepisi nastavitev Baritona (ključ z malimi
     * črkami → vrednost). {@value #DEFAULT_PROFILE} je vedno prisoten in brez prepisov.
     */
    public final Map<String, Map<String, String>> profiles;

    public static final String DEFAULT_PROFILE = "default";
    /**
     * M11.5 (D-031): vgrajen profil workerja — rušenje in postavljanje. Velja samo za registrirane
     * workerje (ne-worker nima rok, {@code CalculationContext}); config ga lahko prepiše z istim imenom.
     */
    public static final String WORKER_PROFILE = "worker";
    static final String WORKER_PROFILE_LINE = WORKER_PROFILE + ": allowBreak=true, allowPlace=true";
    static final String[] DEFAULT_NAMED_PROFILES = {
            "walk: allowSprint=false",
            "parkour: allowParkour=true",
            "cautious: maxFallHeightNoWater=2, allowSprint=false",
            "avoid_water: npcWaterPenalty=37.06", // D-040: 8 blokov hoje = vanilla PathNodeType.WATER
    };

    NpcbConfig(int searchThreads, int searchQueueLimit, int snapshotMarginChunks, int shareRadiusChunks,
               SpeedMode speedMode, int maxTurnDegrees, boolean syncPathsToOps) {
        this(searchThreads, searchQueueLimit, snapshotMarginChunks, shareRadiusChunks, speedMode, maxTurnDegrees,
                syncPathsToOps, parseProfiles(DEFAULT_NAMED_PROFILES));
    }

    NpcbConfig(int searchThreads, int searchQueueLimit, int snapshotMarginChunks, int shareRadiusChunks,
               SpeedMode speedMode, int maxTurnDegrees, boolean syncPathsToOps,
               Map<String, Map<String, String>> profiles) {
        this(searchThreads, searchQueueLimit, snapshotMarginChunks, shareRadiusChunks, speedMode, maxTurnDegrees,
                syncPathsToOps, profiles, false);
    }

    NpcbConfig(int searchThreads, int searchQueueLimit, int snapshotMarginChunks, int shareRadiusChunks,
               SpeedMode speedMode, int maxTurnDegrees, boolean syncPathsToOps,
               Map<String, Map<String, String>> profiles, boolean largeEntities) {
        this(searchThreads, searchQueueLimit, snapshotMarginChunks, shareRadiusChunks, speedMode, maxTurnDegrees,
                syncPathsToOps, profiles, largeEntities, false);
    }

    NpcbConfig(int searchThreads, int searchQueueLimit, int snapshotMarginChunks, int shareRadiusChunks,
               SpeedMode speedMode, int maxTurnDegrees, boolean syncPathsToOps,
               Map<String, Map<String, String>> profiles, boolean largeEntities, boolean crowdYield) {
        this(searchThreads, searchQueueLimit, snapshotMarginChunks, shareRadiusChunks, speedMode, maxTurnDegrees,
                syncPathsToOps, profiles, largeEntities, crowdYield, true);
    }

    NpcbConfig(int searchThreads, int searchQueueLimit, int snapshotMarginChunks, int shareRadiusChunks,
               SpeedMode speedMode, int maxTurnDegrees, boolean syncPathsToOps,
               Map<String, Map<String, String>> profiles, boolean largeEntities, boolean crowdYield,
               boolean workerEnabled) {
        this.workerEnabled = workerEnabled;
        this.largeEntities = largeEntities;
        this.crowdYield = crowdYield;
        this.searchThreads = clamp(searchThreads, 1, 8);
        this.searchQueueLimit = clamp(searchQueueLimit, 8, 1024);
        this.snapshotMarginChunks = clamp(snapshotMarginChunks, 2, 32);
        this.shareRadiusChunks = clamp(shareRadiusChunks, 0, 4);
        this.speedMode = speedMode == null ? SpeedMode.PLAYER : speedMode;
        this.maxTurnDegrees = clamp(maxTurnDegrees, 5, 180);
        this.syncPathsToOps = syncPathsToOps;
        Map<String, Map<String, String>> p = new LinkedHashMap<>();
        p.put(DEFAULT_PROFILE, Collections.emptyMap());
        if (profiles != null) {
            profiles.forEach((k, v) -> p.putIfAbsent(k, Collections.unmodifiableMap(new LinkedHashMap<>(v))));
        }
        parseProfiles(new String[]{WORKER_PROFILE_LINE}).forEach((k, v) -> p.putIfAbsent(k, Collections.unmodifiableMap(v)));
        this.profiles = Collections.unmodifiableMap(p);
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
        cfg.setCategoryComment(CAT_WORKER, "Worker: rušenje, postavljanje, inventar (M11, D-030).");
        int threads = cfg.getInt("threads", CAT_SEARCH, d.searchThreads, 1, 8, "Število iskalnih niti.");
        int queue = cfg.getInt("queueLimit", CAT_SEARCH, d.searchQueueLimit, 8, 1024, "Največ čakajočih iskanj.");
        int margin = cfg.getInt("snapshotMarginChunks", CAT_SEARCH, d.snapshotMarginChunks, 2, 32, "Rob posnetka chunkov okoli začetka in cilja.");
        int share = cfg.getInt("shareRadiusChunks", CAT_SEARCH, d.shareRadiusChunks, 0, 4, "Iskanje se deli, če se začetka razlikujeta za največ toliko chunkov.");
        String speed = cfg.getString("speedMode", CAT_MOVEMENT, "player", "player = kot igralec (sprint, skok), own = lastna hitrost entitete.", new String[]{"player", "own"});
        int turn = cfg.getInt("maxTurnDegrees", CAT_MOVEMENT, d.maxTurnDegrees, 5, 180, "Največji obrat telesa v stopinjah na tick.");
        boolean large = cfg.getBoolean("largeEntities", CAT_MOVEMENT, d.largeEntities,
                "M8 (D-028): navigator vodi tudi entitete, širše od 1,0 ali višje od 2,0 (do 3,0 x 4,0). false = samo 1x2 (D-019).");
        boolean crowd = cfg.getBoolean("crowdYield", CAT_MOVEMENT, d.crowdYield,
                "M7.10: v gneči (druga entiteta pred nogami) počakaj namesto rinjenja; čakanje ne šteje v časovno omejitev premika. false = kot Baritone.")
                || Boolean.getBoolean("npcbcrowdyield");
        boolean worker = cfg.getBoolean("enabled", CAT_WORKER, d.workerEnabled,
                "M11 (D-030): workerji (API 3). false = NpcBaritone.worker() vrne null; navigacija (API 1-2) ostane.");
        boolean sync = cfg.getBoolean("syncPathsToOps", CAT_DEBUG, d.syncPathsToOps, "Pošlji poti vsem operaterjem z modom na klientu (debug prikaz). Brez tega jih dobi samo, kdor vklopi /npcb debug on.");
        String[] named = cfg.getStringList("named", CAT_PROFILE, DEFAULT_NAMED_PROFILES,
                "Poimenovani profili: 'ime: nastavitev=vrednost, nastavitev=vrednost'. Imena nastavitev so Baritonova (Settings). Profil 'default' je vedno prisoten.");
        if (cfg.hasChanged()) {
            cfg.save();
        }
        return new NpcbConfig(threads, queue, margin, share, parseSpeedMode(speed), turn, sync, parseProfiles(named), large,
                crowd, worker);
    }

    /**
     * Prepiše strežniške vrednosti v profil jedra (D-016). Iskalne meje (niti, vrsta) bere
     * forge plast sama (M5).
     */
    public Settings applyTo(Settings settings) {
        return applyTo(settings, speedMode);
    }

    /** Kot {@link #applyTo(Settings)} z načinom hitrosti instance (D-039, CNPC U6). */
    Settings applyTo(Settings settings, SpeedMode speedMode) {
        settings.npcSnapshotMarginChunks.value = snapshotMarginChunks;
        settings.npcMaxTurnDegrees.value = (float) maxTurnDegrees;
        if (crowdYield) {
            settings.npcCrowdYield.value = true; // profil ga lahko vklopi tudi sam; config ga ne izklaplja
        }
        if (speedMode == SpeedMode.OWN) {
            // D-010: v lastni hitrosti cene ne veljajo za skoke čez reže
            settings.allowParkour.value = false;
        }
        return settings;
    }

    /**
     * Razčleni vrstice {@code ime: kljuc=vrednost, kljuc=vrednost}. Neveljavne vrstice se
     * preskočijo; ali ključi obstajajo v {@code Settings}, preveri {@link Attach#profileFor}.
     */
    static Map<String, Map<String, String>> parseProfiles(String[] lines) {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty() || !name.matches("[a-z0-9_\\-]+")) {
                continue;
            }
            Map<String, String> overrides = new LinkedHashMap<>();
            for (String part : line.substring(colon + 1).split(",")) {
                int eq = part.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                String value = part.substring(eq + 1).trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    overrides.put(key, value);
                }
            }
            out.put(name, overrides);
        }
        return out;
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
                + ", maxTurn=" + maxTurnDegrees + ", syncPathsToOps=" + syncPathsToOps
                + ", largeEntities=" + largeEntities + ", crowdYield=" + crowdYield + ", workerEnabled=" + workerEnabled
                + ", profiles=" + profiles.keySet() + "}";
    }
}
