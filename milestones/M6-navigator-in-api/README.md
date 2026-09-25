# M6 — `PathNavigate` adapter in javni API

**Velikost:** M · **Odvisen od:** M4, M5 · **Odločitve:** D-005, D-018, D-021

## Cilj

Kateri koli `EntityLiving` z zamenjanim navigatorjem hodi z Baritonom, **njegovi obstoječi
AI taski pa delujejo brez sprememb**. Stabilen API za porabnike.

## Naloge

| # | Naloga | Automatone vzorec |
|---|---|---|
| M6.1 | `BaritonePathNavigate extends PathNavigateGround` po D-018: optimističen `tryMoveTo*`, `noPath` po stanjih, debounce istega cilja, `clearPath` | — |
| M6.2 | Hibrid: `getPathToPos`, `getPathToEntityLiving` → vanilla `PathFinder` (sinhrono, kratek domet) | — |
| M6.3 | `setPath(vanillaPath, speed)` → Baritone cilj = zadnja točka poti (`GoalNear`, r=1); pogost vzorec vanilla `EntityAIAttackMelee` | — |
| M6.4 | `getPath()` → vanilla `Path` iz Baritonove poti (točke blokov) | — |
| M6.5 | `setSpeed(s)`: `s > 1,0` dovoli sprint, sicer hoja; zapiši v javadoc | — |
| M6.6 | `tryMoveToEntityLiving` = cilj, ki sledi entiteti (posodobi cilj, ko se premakne > 2 bloka) | (FollowProcess koncept) |
| M6.7 | API paket (`api/`): `NpcBaritone`, `INpcNavigator`, `NavState`, `Profile`, `IMovementFrame`, cilji; povratni klici `onArrived/onFailed` | `843b8397`, `e4d493a9`, `7dd013f0` |
| M6.8 | `apiJar` Gradle naloga + javadoc; različica API 1 | — |
| M6.9 | Testi v igri: vanilla zombi (napad, tavanje), vaščan (`MoveIndoors`, `Wander`), volk (sledi lastniku 0,6×0,85) — vsak z adapterjem na tečaju T1 | — |

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | zombi z adapterjem napade igralca čez T1 teren (`EntityAIAttackMelee` nespremenjen) |
| A2 | vaščan se ponoči vrne v hišo z vrati (T2 hiša) |
| A3 | volk sledi lastniku čez stopnice in vodo |
| A4 | ponovni `tryMoveToXYZ` z istim ciljem 20× na sekundo = 1 iskanje (telemetrija) |
| A5 | `api` jar se prevede brez `core` na classpathu (razen ciljev) |

## Stanje (2026-09-25, veja `m6-navigator` iz `m5-zmogljivost`)

| # | Stanje |
|---|---|
| M6.1 | `NavDebounce`: isti cilj (±1, pri sledenju ±2) med iskanjem/hojo ali po prihodu = brez iskanja; `clearPath` = pavza, isti cilj v 10 tickih nadaljuje, sicer pravi preklic; isti cilj po neuspehu 20 tickov zavrnjen (`false`). `noPath` po stanjih; aktiven proces ima prednost pred starim izidom (`NavStatus.decide`). JUnit |
| M6.2 | `getPathToPos`/`getPathToEntityLiving` ostaneta vanilla (podedovano) |
| M6.3 | `setPath(vanilla)` → `GoalNear(zadnja točka, 1)` |
| M6.4 | `getPath()` → vanilla `Path` iz Baritonove poti (predpomnjen na izvajalec, indeks = trenutni premik) |
| M6.5 | `setSpeed(s)`: `s > 1,0` sprint, `0 < s ≤ 1,0` hoja, `s ≤ 0` (ukazi, `goTo`) po profilu — `BaritoneMoveHelper` bere `allowsSprint()` |
| M6.6 | `tryMoveToEntityLiving`: sledi; nov cilj, ko se tarča premakne > 2 bloka (največ vsakih 10 tickov) |
| D-019 | širše od 1,0 ali višje od 2,0 → vse metode `super` (vanilla), Baritone preklican |
| M6.7, M6.8 | API paket in `apiJar`: **naslednji korak** |
| M6.9 | `/npcb aitest attack`: husk z **vanilla AI** (brez puppet) napade vaščana (`NoAI`) za zidom z režo 20 blokov stran; `t1-run.ps1` preveri HIT in ≤ 3 nova iskanja. Vaščan domov (A2) in volk (A3) potrebujeta igralca/noč → ročno ali v M7 |
