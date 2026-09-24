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
