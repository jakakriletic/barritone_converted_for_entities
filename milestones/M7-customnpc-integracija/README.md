# M7 — Integracija v CustomNPC rework

**Velikost:** M · **Odvisen od:** M6 · **Odločitve:** D-005, D-018, D-019; v CNPC: D-007, D-012, D-016, M2.7

## Cilj

V CustomNPC rework je Baritone izbirno ozadje navigacije za posamezne NPC-je, privzeto
izklopljeno, in **z meritvijo dokazano boljše** od vanilla na merilih M2.7.

## Pogoj za začetek

Nova odločitev v CNPC (`docs/01-ARHITEKTURA.md`, dnevnik), predlog besedila:

> **D-0xx:** Stopnja D iz D-012 se odpre **samo kot izbirno ozadje** (`RwNavBackend`)
> prek zunanje knjižnice `npcbaritone`. Vanilla ostane privzeta. Race conditioni so
> naslovljeni v knjižnici (posnetek chunkov na glavni niti, ponovna preverba premikov)
> in izmerjeni v njenem M5. Sprejem po M2.7 s tremi ponovitvami (D-016).

Brez te odločitve M7 ne začne (D-005).

## Naloge

| # | Naloga | Kje |
|---|---|---|
| M7.1 | Revizija 21 AI datotek z navigatorjem: tabela klic → obnašanje pod adapterjem → ukrep | CNPC `docs/` |
| M7.2 | `NpcBaritoneBridge` v `noppes/npcs/rework/` (samo `api`, `Loader.isModLoaded`) | CNPC |
| M7.3 | Stikalo `RwNavBackend` (config globalno + nov NBT ključ na NPC), privzeto vanilla | CNPC (kompat. politika §3) |
| M7.4 | `EntityNPCInterface.updateTasks()`: veja pred `movementType` (01-ARHITEKTURA §7); samo `movementType == 0` in D-019 | CNPC `src/patch` |
| M7.5 | `NpcNavRange` in `getSpeed()` → Baritone profil (domet posnetka, način hitrosti) | CNPC + knjižnica |
| M7.6 | Scenarij M2.7 z izbiro ozadja (`nav-run.ps1 -Ozadje baritone`) | CNPC |
| M7.7 | A/B: vanilla vs. Baritone, po 3 ponovitve, tabela šestih veličin z razponi | CNPC `docs/meritve/` |
| M7.8 | Jahanje (CNPC D-018): NPC na nosilcu — Baritone se ne uporablja za jahača; preveri, da `RiderState` ostane pravilen | CNPC |
| M7.9 | Sled nedeterminizma na grlu B: razlog preklica/premora, zamik uporabe rezultata iskanja, gneča (`PathTrace` stolpci 18–24); serija 10 zagonov `nav-run -Sled`, analiza `tools/sled_grlo.py` | knjižnica + CNPC |

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | Brez knjižnice v `mods/` se CNPC obnaša bitno enako kot prej (`verify-package` + testi) |
| A2 | Z knjižnico in stikalom izklopljenim: enako kot A1 |
| A3 | M2.7 veličine 1 (delež celih poti), 2 (razmerje dolžine), 3 (čas skupine) boljše od vanilla čez izmerjeni razpon |
| A4 | veličini 5 (µs/iskanje na **glavni** niti) in 6 (iskanj/tick) ne slabši čez razpon |
| A5 | integracijska matrika CNPC M0.7 brez novih padcev |

**Vrata V4:** A3 in A4 ⇒ Baritone ostane izbiren v CNPC in se dokumentira za uporabnike.
Sicer ostane izklopljen in knjižnica služi drugim porabnikom (ladje, M9).

**Popravek A3 (D-043, 28. 9.).** Veličina 1 je bila v M7.8 pri vanilli že 8/8, zato »boljše«
ni bilo dosegljivo. Od D-043 velja: veličina 1 **ni slabša**; veličini 2 in 3 sta **boljši v
mediani** in Baritonov **najslabši** zagon ni slabši od vanilla (vanilla je deterministična,
razpon 0); za Baritona najmanj **5 ponovitev**. Po tej definiciji M7.8 še vedno ne prestane
(grlo B: najslabši zagon 4/8 proti vanilla 6/8). A4 ostane, a potrebuje enako definirana
dogodka v obeh ozadjih; dokler tega ni, se ne šteje ne kot padec ne kot prestano.

## Stanje (2026-09-28, nadaljevanje na vejah `codex/m7-next`)

| # | Stanje |
|---|---|
| pogoj | **izpolnjen**: CNPC D-022 (`customNPC_rework` `0cd2fa8`), stopnja A zmogljivosti (1,64 ms) |
| M7.1 | **narejeno**: `customNPC_rework/docs/07-BARITONE-OZADJE.md` — 105 klicev, ukrepi U1–U7 (U7 formacije); adapter popravljen za `getPath()` med iskanjem (`2ee0df2`) |
| M7.2–M7.4 | v CNPC kodi na veji `m7-cnpc-oblak`: most, stikalo, ponovna namestitev po `updateTasks()` |
| M7.5 | API 2, hitrost, vrata, voda in doseg v kodi; način `OWN` popravljen po primerljivi meritvi |
| M7.6 | scenarij in prvi posamezni A/B zagoni zeleni; [meritev 28. 9.](../../docs/04-STANJE.md) |
| M7.7 | 3 + 3 ponovitve zeleni; prihod na grlu A boljši, toda dolžina Baritonove poti in primerljiv strošek iskanj še manjkajo; V4 odprt |
| M7.8 | **narejeno 28. 9.**: A/B 3 + 3 z dejanskimi potmi (V4 ni prestan); R1 našel zastoj nosilca z jahačem pod Baritonom → izbira izključi jahača in nosilca s potnikom, ponovljeni R1 zelen (CNPC `docs/meritve/2026-09-28-M7.8-zakljucek.md`) |
| M7.10 | **zaključeno 28. 9.**: čakanje v gneči (`npcCrowdYield`, privzeto izklopljeno); različica c v 5 zagonih 8/8 na obeh grlih in odprtem (CNPC `docs/meritve/2026-09-28-M7.10-cakanje-v-gneci.md`), JUnit 141/141 |
| V4 | **sprejet 28. 9.** (D-043) z znano omejitvijo: pri več entitetah na istem ciljnem bloku zadnja prispe 2–6 s kasneje kot vanilla (`CROWD_END_MOVES`) |
| M7.9 | **zaključeno 28. 9.**: vzrok nestabilnosti grla B je gneča (`movement_timeout` → novo iskanje), zamik rezultata iskanja jo samo sproži; poprej: **v kodi 28. 9., čaka na build in serijo**: sled + `nav-run -Sled` + `tools/sled_grlo.py`; prevod in JUnit 135/135 v oblaku |
