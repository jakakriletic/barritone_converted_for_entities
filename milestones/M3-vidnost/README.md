# M3 — Vidnost: ukazi, debug prikaz poti, telemetrija

**Velikost:** S · **Odvisen od:** M2 · **Odločitve:** D-024

## Cilj

Vse, kar NPC z Baritonom misli, se da **videti** (pot v svetu) in **prebrati** (CSV), brez
tega pa vanilla klient še vedno normalno igra. Zaradi tega je M3 pred M4/M5: vsak
naslednji problem se razišče hitreje.

## Naloge

| # | Naloga | Automatone vzorec |
|---|---|---|
| M3.1 | `/npcb`: `attach [puppet] [profil]`, `detach`, `goto <x y z / @e>`, `stop`, `status [@e]`, `profile <ime>`, `debug on/off`, `speedtest`, `trace on/off/dump` | `c212980e`, `a893582a` |
| M3.2 | Paket poti (strežnik → klient z modom, samo OP-jem z vklopljenim debugom, največ 4 Hz na entiteto) | `2ebfbf0f`, `635f8aaf`, `32bf8032` |
| M3.3 | Klientski izris: pot kot črta + točke, trenutni premik poudarjen, cilj; prirejeno iz `PathRenderer` (CLIENT v port mapu) | `2bce8f85` |
| M3.4 | Telemetrija CSV (vzorec `FleetTrace` iz `ladja_mod`): tick, entiteta, stanje, pozicija, cilj, dolžina poti, premik, µs iskanja, ponovna iskanja, razlog neuspeha | — |
| M3.5 | Vanilla klient (brez moda) se poveže na strežnik z modom; NPC-ji hodijo | — |

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | OP klient z modom vidi pot zombija v T1 |
| A2 | vanilla klient se poveže in vidi zombija hoditi (D-024) |
| A3 | `trace dump` zapiše CSV z vsemi stolpci; en tek T1 = ena datoteka |
| A4 | debug paketi se ne pošiljajo, ko je debug izklopljen (števec 0) |

## Stanje (2026-09-25, veja `m3-vidnost`)

| # | Stanje |
|---|---|
| M3.1 | `/npcb attach <e> [puppet] [profil]`, `goto <e> <x y z \| cilj-entiteta>`, `status [e]`, `profile list \| <e> <profil>`, `debug [on\|off]`, `trace on [e] \| off \| dump`; prevedeno, JUnit zelen |
| M3.2 | `DebugSync` + `PathSyncMessage` (format 1): samo OP 2 + mod na klientu (FML seznam modov) + `debug on` ali `syncPathsToOps`, ≤ 128 blokov, ≤ 4 Hz, keepalive 1 s; prevedeno |
| M3.3 | `client/PathRenderer`: sivo prehojeno, rumeno trenutni premik, rdeče preostanek, magenta naslednji segment, zelen cilj (cian med iskanjem), bela črta NPC → naslednja točka; prevedeno, **ni še videno v igri** |
| M3.4 | `PathTrace` (17 stolpcev, glej spodaj); `/npcb course t1 run` samodejno zapiše `t1-<čas>-trace.csv` poleg izidov |
| M3.5 | čaka ročno preverbo (vanilla klient) |

**Stolpci sledi:** `world_tick, entity_id, entity, tag, state, x, y, z, goal, path_len, path_pos, movement, search_us, searches, replans, fail_reason, events`.
`tag` = odsek tečaja (`T1/3`), `events` = dogodki poti v tem ticku (`CALC_STARTED|…`),
`search_us` = trajanje zadnjega končanega iskanja, `fail_reason` = `no_path | queue_full | exception`.

**Poimenovani profili** (`config/npcbaritone.cfg`, `profile { S:named < … > }`):
`walk` (brez sprinta), `parkour` (skoki čez reže; v `speedMode=own` vedno izklopljen),
`cautious` (padec ≤ 2, brez sprinta). Neznana nastavitev v profilu da napako ob `attach`/`profile`.

### Preverbe pri uporabniku

1. **Windows build + dedicated (A3, A4):** `.\dev.ps1 build --offline; .\t1-run.ps1` → poleg
   M2 vrstic še `M3 A3 sled: 17 stolpcev`, `pokrije vse odseke: 10/10`, `M3 A4 … 0 (= 0)`.
2. **Klient z modom (A1):** `.\dev.ps1 runClient --offline`, enoigralski svet s cheati,
   `/npcb course t1 build`, `/summon husk ~2 ~ ~`, `/npcb debug on`,
   `/npcb course t1 run @e[type=husk,c=1]` → vidiš črte poti in zeleno škatlo cilja; po
   `/npcb debug off` črte izginejo v ≤ 3 s.
3. **Ukazi:** `/npcb status @e[type=husk,c=1]` (stanje, pot, µs iskanja), `/npcb profile list`,
   `/npcb profile @e[type=husk,c=1] walk`, `/npcb goto @e[type=husk,c=1] @p`.
4. **Vanilla klient (A2, M3.5):** dedicated strežnik z modom (`.\smoke-server.ps1`, nato ročno
   `runServer`), vanilla 1.12.2 klient se poveže, OP na strežniku da `/npcb attach` + `goto` →
   husk hodi, klient nima napak v logu.
