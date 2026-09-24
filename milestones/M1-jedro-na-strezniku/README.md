# M1 — Jedro na strežniku: iskanje poti brez igralca

**Velikost:** L · **Odvisen od:** M0 · **Odločitve:** D-001, D-002, D-003, D-006, D-012, D-013, D-014, D-016, D-020, D-021, D-022

## Cilj

Baritonov A\* in vsi premiki tečejo v `si.ladja.npcbaritone.core` nad posnetkom
strežniških chunkov, za `EntityLiving`, brez ene same klientske odvisnosti — dokazano z
golden testi brez zagona igre.

## Vrstni red (vsaka vrstica = en ali več majhnih commitov)

Automatone commit je **vzorec**, ne vir za kopiranje. `PORT-MAP.md` pove, kaj se zgodi s
posamezno datoteko.

| # | Naloga | Automatone vzorec | Opomba |
|---|---|---|---|
| M1.1 | **Mehanska relokacija:** `git mv mod/src/upstream/java/baritone` → `mod/src/main/java/si/ladja/npcbaritone/core` (+ `api`), zamenjava `package`/`import` s skripto; commit brez drugih sprememb | `e8b74bbe` | D-003, D-026; diff mora biti samo imena paketov |
| M1.2 | **Izrez:** izbriši vse `DROP` in `CLIENT` datoteke iz `PORT-MAP.md` (skripta bere tabelo) | `d386d440` | −221 datotek (217 DROP + 4 CLIENT); nato prevod pokaže, kaj je še vezano |
| M1.3 | MCP preimenovanja, ki ostanejo (`MCP-PREIMENOVANJA.md`, vrstice v KEEP/ADAPT) + `isSideSolid` v `BlockStateInterfaceAccessWrapper` | — | ~25 mest |
| M1.4 | `IEntityContext` / `EntityContext` nad `EntityLiving`: `entity()`, `world()` (WorldServer), `feetPos()`, `headPos()` (višina oči iz entitete), `rotation()`, `frame()` = identiteta | `808f2b99`, `e757b3e5`, `8a97376b`, `00c370f8`, `2ba848ca`, `076c44ff` | D-006, D-021; 227 + 117 + 83 klicev |
| M1.5 | `ChunkSnapshot` (omejena kopija `id2ChunkMap`, D-013) + `BlockStateInterface` nad njim; `isLoaded` = v posnetku | `4dae7a91`, `38c477f1` | Baritone že ima `copyLoadedChunks`; zamenja se vir. **M0.9:** `Chunk.getBlockState` rabi `World` (NPE brez njega) → beri iz `getBlockStorageArray()`; chunki `markLoaded(true)` (RAZISKAVA §4a) |
| M1.6 | **Lint test D-012:** skenira `core/**` in pade ob `World.getBlockState`, `getChunkFromChunkCoords`, `provideChunk`, `loadChunk` izven `world/` | — | poceni in trajno |
| M1.7 | `WorldData` minimalen (per dimenzija, brez diska); izbris `CachedWorld`/`CachedRegion` sklicev | — | D-014 |
| M1.8 | `Settings` na instanco + NPC profil (§8 raziskave) | `89b1174a`, `7bd582c2`, `a1bb2422` | D-016; statični `Baritone.settings()` ven |
| M1.9 | `Helper` → log4j; odstrani chat, obvestila | `d65e13c6` | |
| M1.10 | Vitka instanca (`NavigatorCore`): `PathingBehavior`, `CustomGoalProcess`, `PathingControlManager`, `LookBehavior`, `InputState`; register instanc po entiteti (šibke reference) | `891b9e10` (koncept komponente) | D-020 |
| M1.11 | Začasen executor: 1 nit, omejena vrsta (pravi v M5) | `a67d6901` | D-017 |
| M1.12 | **Golden testi** (glej spodaj) | — | D-022 |
| M1.13 | Test mej paketov: `core`/`api`/`forge` ne uvažajo `net.minecraft.client` | `d65e13c6` | D-024 |

## Golden testi (headless, `SyntheticWorld`)

Za vsak teren: A\* iz S v G z NPC profilom. Preverja se: ali je pot **cela**, katere vrste
premikov vsebuje, in cena v pričakovanem razponu.

| # | Teren | Pričakovano |
|---|---|---|
| G1 | ravnina 30 blokov | cela; samo `Traverse`/`Diagonal` |
| G2 | zid z režo širine 1 | cela; skozi režo |
| G3 | zid brez reže, obhod 10 blokov | cela; okoli |
| G4 | stopnica 1 | `Ascend` |
| G5 | stopnice gor 3 | 3× `Ascend` |
| G6 | padec 3 | `Descend`/`Fall` dovoljen |
| G7 | padec 4 brez vode | ne čez rob; obhod ali neuspeh (`maxFallHeightNoWater=3`) |
| G8 | bazen širine 5 | cela; skozi vodo z višjo ceno |
| G9 | hodnik 1×2, dolg 10 | cela |
| G10 | cilj v zaprti škatli | ni cele poti; iskanje se konča v `failureTimeoutMS` |
| G11 | cilj za robom posnetka | delna pot do roba (D-014) |
| G12 | isti teren, `allowBreak=false` z zidom iz kamna | nobena pot ne ruši |

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | `mod/src/main` se prevede brez napak (oblak + Windows) |
| A2 | G1–G12 zeleni |
| A3 | lint D-012 in test mej paketov zelena |
| A4 | `grep -ri "elytra\|net.minecraft.client" mod/src/main` = 0 (razen `client/`, ki v M1 ne obstaja) |
| A5 | dedicated server se zažene z modom (smoke) |
| A6 | `git log` ima ločena commita "relokacija" in "izrez" pred vsemi vsebinskimi spremembami |

**Vrata V1:** če bi M1.4–M1.10 zahtevali prepis več kot ~20 % vrstic v `pathing/**`,
ustavi in revidiraj D-001 (zapis v `02-ODLOCITVE.md`).

## Ni v obsegu

Premikanje entitete (M2), vrata (M4), pravi executor (M5), `PathNavigate` (M6).

## Stanje: **zaključen 2026-09-24** (merge `262c819`)

| # | Stanje |
|---|---|
| M1.1–M1.3 | **narejeno** (mehanski commiti ločeni) |
| M1.4 | **narejeno** — `IEntityContext`/`EntityContext` nad `EntityLiving`, `frame()` = identiteta |
| M1.5 | **narejeno** — `ChunkSnapshot`, BSI bere `ExtendedBlockStorage`, posnetek ob začetku iskanja |
| M1.6 | **narejeno** — `ArchitectureLintTest` |
| M1.7 | **narejeno drugače** — `WorldData` izbrisan (nič ga ne rabi) |
| M1.8 | **narejeno** — profil na instanco (prek BSI) |
| M1.9 | **narejeno** — `Helper` → log4j |
| M1.10 | **narejeno** — vitka instanca, šibek register |
| M1.11 | **narejeno** — `SearchExecutor` (1 nit, vrsta 256) |
| M1.12 | **narejeno** — G1–G12 zeleni v oblaku |
| M1.13 | **narejeno** — lint meja paketov |

| Merilo | Stanje |
|---|---|
| A1 | **zeleno** (oblak + Windows) |
| A2 | **zeleno** (oblak + Gradle na Windowsu) |
| A3 | zeleno |
| A4 | zeleno (`elytra` 0, `net.minecraft.client` 0) |
| A5 | **zeleno** |
| A6 | zeleno (`d99d792` relokacija, `b121b5c` izrez pred vsebinskimi) |
| V1 | 4,7 % vrstic `pathing/**` spremenjenih — D-001 drži |
