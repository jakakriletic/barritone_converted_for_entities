# M7 — Integracija v CustomNPC rework

**Velikost:** M · **Odvisen od:** M6 · **Odločitve:** D-005, D-018, D-019, D-039; v CNPC: D-007, D-012, D-016, M2.7

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

## Stanje (2026-09-27, veja `m7-cnpc` iz `main`)

| # | Stanje |
|---|---|
| pogoj | **izpolnjen**: CNPC D-022 (`customNPC_rework` `0cd2fa8`), stopnja A zmogljivosti (1,64 ms) |
| M7.1 | **narejeno**: `customNPC_rework/docs/07-BARITONE-OZADJE.md` — 105 klicev, ukrepi U1–U7 (U7 formacije); adapter popravljen za `getPath()` med iskanjem (`2ee0df2`) |
| API 2 | **prevedeno + JUnit** (D-039, `593a9b4`–`640b956`): `reinstall` (U2), hitrost (U6) in vrata (U3/U4) na instanco; čaka build na Windowsu in T2 |
| M7.2–M7.3 | odblokirani (API 2); niso začeti |
| M7.4 | predpogoj izpolnjen (CNPC M3.1 na `origin`); `reinstall()` po `updateTasks()` |
| M7.5–M7.8 | niso začeti |
