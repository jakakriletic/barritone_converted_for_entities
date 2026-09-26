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
| M6.7 | `api/`: `NpcBaritone` (vstop, `API_VERSION = 1`, `available/attach/get/detach/supports`), `INpcNavigator` (`goTo(BlockPos/Goal)`, `follow(entity, range)`, `stop`, `state`, `failReason`, profil, poslušalci), `NavState`, `NavListener` (`onArrived`, `onFailed(reason)` — enkrat na prehod), `INpcBaritoneProvider`; izvedba `forge/ApiProvider`. `attach` prek API **ne** odstrani AI taskov |
| M6.8 | `apiJar` → `mod/build/libs/npcbaritone-<ver>-api.jar` (deobf, viri, LICENSE/NOTICE, manifest `NpcBaritone-Api-Version: 1`); A5: `ApiJarTest` prevede vzorčnega porabnika samo proti razredom API jarja, lint dovoli v `api/` samo MC, Javo in cilje |
| M6.9 | `/npcb aitest attack`: husk z **vanilla AI** (brez puppet) napade vaščana (`NoAI`) za zidom z režo 20 blokov stran; `t1-run.ps1` preveri HIT in ≤ 3 nova iskanja. Vaščan domov (A2) in volk (A3) potrebujeta igralca/noč → ročno ali v M7 |

### Uporaba API (porabnik, npr. CustomNPC rework)

```groovy
dependencies { compileOnly files('libs/npcbaritone-0.0.1-m0-api.jar') }
```

```java
if (NpcBaritone.available() && NpcBaritone.supports(npc)) {
    INpcNavigator nav = NpcBaritone.attach(npc, "default");   // vanilla AI ostane (D-018)
    nav.addListener(new NavListener() {
        @Override public void onArrived(EntityLiving e) { /* ... */ }
        @Override public void onFailed(EntityLiving e, String reason) { /* no_path, queue_full, ... */ }
    });
    nav.goTo(new BlockPos(100, 64, -20));
}
```

Obnašanje, ki se razlikuje od vanilla: cilj, do katerega ni poti (npr. vanilla tavanje izbere
točko za zaprto steno), konča s `FAILED` in `noPath() = true`; vanilla bi naredil nekaj korakov
proti njej. Ponovni isti cilj je 20 tickov zavrnjen.

**A1–A3 preverjeni v klientu (2026-09-26, `/npcb selftest`, 6/6):** zombi brez `puppet` udari
igralca za zidom z režo (90 tickov, kontrola 175), ukročen volk pride peš za zidom (51 tickov,
0 teleportov), vaščan ponoči gre skozi vrata v kolibo (146 tickov, kontrola 130). Pred tem je
bil popravljen `Attach`: taski s shranjenim navigatorjem (`EntityAIFollowOwner` …) so ukazovali
staremu navigatorju (`55f4245`). Podrobnosti: `docs/04-STANJE.md`.

**A1–A3 (20:26):** v brezglavem testu vanilla `EntityAIZombieAttack` ne začne niti pri kontroli brez
Baritona (pot do tarče obstaja) → A1–A3 so **ročna preverba v klientu** (`/npcb attach` na zombija ponoči
ob igralcu; vaščan domov; ukročen volk sledi). `aitest` ostane kot diagnostika.
