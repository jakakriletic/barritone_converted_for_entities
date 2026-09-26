# M2 — Noge: izvajanje poti na vanilla mobu

**Velikost:** M · **Odvisen od:** M1 · **Odločitve:** D-008, D-009, D-010, D-011, D-012, D-025, D-003

## Cilj

Vanilla zombi s pripetim Baritonom (`/npcb attach … puppet`) prehodi tečaj T1 tako, kot
bi ga igralec z Baritonom: s pravo hitrostjo, skoki in brez tresenja.

## Naloge

| # | Naloga | Automatone vzorec | Opomba |
|---|---|---|---|
| M2.1 | `InputState` (EnumSet/BitSet, sinhroniziran kot pri Automatonu) | `6ddd4b14` | nadomesti `InputOverrideHandler` |
| M2.2 | `BaritoneMoveHelper`: vrstni red `setAIMoveSpeed` → `setMoveForward` → `setMoveStrafing`; sneak ×0,3; sprint prek `setSprinting`; ko ne vodi → `super` | `8adf38cf`, `f1a2d467`, `930085b5`, `33b707cf` | D-010 — **vrstni red je ključen** (`setAIMoveSpeed` povozi `moveForward`) |
| M2.3 | `BaritoneJumpHelper`: `setJumping(JUMP)`; ko ne vodi → `super` | — | |
| M2.4 | `LookBehavior` → `rotationYaw` + `renderYawOffset`, ≤ `maxTurnDegrees`/tick; glava ostane vanilla | `a8d793f9`, `fadc7082` | D-011 |
| M2.5 | Tick iz `BaritonePathNavigate.onUpdateNavigation()` (minimalna verzija; polna pogodba v M6) | — | D-009 |
| M2.6 | `Attach`: refleksija na `navigator`, `moveHelper`, `jumpHelper` (SRG imena preveri z `javap` na obfuskiranem jarju); `puppet` = počisti `tasks` in `targetTasks` | `548f9f25` | D-008, D-025 |
| M2.7 | Način hitrosti "kot igralec" (osnovni `MOVEMENT_SPEED` 0,1 med vodenjem, vrne se ob `detach`) in "lastna" (skaliranje cen, parkour izklopljen) | `31374839`, `446120d8` | D-010 |
| M2.8 | Minimalni ukazi: `/npcb attach [puppet]`, `/npcb goto x y z`, `/npcb stop`, `/npcb speedtest` (10 s ravno, izpiše m/s) | `c212980e` | OP 2 |
| M2.9 | Tečaj **T1** kot `.mcfunction` + `t1-run.ps1` (prisilno naloženi chunki, zombi na startu, zaporedni cilji, CSV) | — | vzorec CNPC `nav-run.ps1` |
| M2.10 | Števec `ChunkEvent.Load` med tekom (izključi prisilne) | — | D-012 |
| M2.11 | Soobstoj: enoigralski svet s pravim Baritone 1.2.19 na klientu + naš mod | — | D-003 |
| M2.12 | Dedicated server smoke | `fa380f68` | |

## Merila sprejema

| # | Merilo | Meja |
|---|---|---|
| A1 | T1: odsekov doseženih | ≥ 9/10 (odsek "nedosegljiv cilj" mora končati FAILED, ne tavanje) |
| A2 | `speedtest` hoja, način "kot igralec" | 4,32 ± 0,2 m/s |
| A3 | `speedtest` sprint | 5,6 ± 0,25 m/s |
| A4 | naloženih chunkov med T1 (brez prisilnih) | 0 |
| A5 | tresenje: menjav smeri yaw > 90° v 5 tickih na ravnem odseku | 0 |
| A6 | soobstoj s pravim Baritonom (M2.11) | ni `LinkageError`, zombi hodi |
| A7 | dedicated server: attach + goto deluje | da |

**Vrata V2:** A1–A3 v meji ⇒ nadaljuj. Če A2/A3 padeta, se pred M4 revidira D-010
(npr. merjenje dejanskega `travel` pospeška mob vs. igralec).

## Ni v obsegu

Vrata, voda kot cilj testiranja (samo kot del T1, če je), vanilla AI taski (M6), izris (M3).

## Stanje (2026-09-25, veja `m2-noge`)

| # | Stanje |
|---|---|
| M2.1 | preskočen — `InputOverrideHandler` je že samo stanje vhodov |
| M2.2–M2.8 | Windows build, JUnit in dedicated T1 zeleni; D-008 preverjen v klientu z `/npcb selftest` (2026-09-26: zombi brez `puppet` udari igralca po 90 tickih) |
| M2.9 | `CourseT1` + `CourseRunner` + `t1-run.ps1`; 3× dedicated 10/10 pričakovanih izidov |
| M2.10 | števec v `Telemetry`, stolpec `chunk_loads` v CSV |
| M2.11 | **A6 preverjen** (2026-09-26): Forge 14.23.5.2859 (CurseForge) s pravim Baritone 1.2.19 + naš izdani jar, `/npcb selftest` 6/6, brez `LinkageError` (v dev `runClient` Baritonov obfuskirani jar ne deluje — `MixinStateImplementation`) |
| M2.12 | `t1-run.ps1` trikrat exit 0 na dedicated strežniku (A1–A5, A7) |

### Ročni preverbi v klientu

1. **D-008:** `/summon zombie ~3 ~ ~` (noč ali `/difficulty easy`), `/npcb attach @e[type=zombie,c=1]` (brez `puppet`) → zombi te še vedno napade.
2. **M2.11 / A6:** v `mod/run/mods` daj pravi Baritone 1.2.19 za Forge, `.\dev.ps1 runClient --offline`, enoigralski svet, `/npcb attach @e[type=husk,c=1] puppet`, `/npcb goto @e[type=husk,c=1] ~10 ~ ~` → husk hodi, v logu ni `LinkageError`.
