# M0 — Okolje, uvoz Baritona, headless harness

**Velikost:** S · **Odvisen od:** — · **Odločitve:** D-003, D-004, D-007, D-022, D-024, D-026

## Cilj

Prazen, a pravi Forge mod `npcbaritone`, ki se zgradi na Windowsu in naloži na klientu in
dedicated strežniku; nespremenjen Baritone v1.2.19 v repozitoriju kot en commit; in
dokaz, da se iskanje poti da testirati brez zagona igre.

## Zakaj najprej

Vse naslednje se preverja s temi tremi stvarmi. Brez harnessa je vsak korak M1
"prevede se, ne vem pa, ali dela".

## Naloge

| # | Naloga | Opomba |
|---|---|---|
| M0.1 | Gradle projekt v `mod/`: FG 2.3-SNAPSHOT, Gradle wrapper 4.9, Forge 14.23.5.2847, MCP snapshot_20171003, Java 8 | kopiraj pristop iz CNPC `dev/build.gradle` in `OKOLJE.md`; brez `--offline` samo prvič |
| M0.2 | `dev.ps1` (Java 8: `NPCB_JAVA8_HOME`, sicer CNPC `.tools/jdk8`, sicer `C:\Program Files\Java\jdk1.8.0_202`), `prepare-assets.ps1` iz CNPC | exit code se preveri |
| M0.3 | `@Mod(modid="npcbaritone")`, config (`npcbaritone.cfg`, prazne sekcije iz 01-ARHITEKTURA §6), `@NetworkCheckHandler` vedno `true` | vzorec `XaeroFleetCommandMod.acceptRemote` |
| M0.4 | **Uvoz upstream:** `references/baritone-1.12.2/src/{api,main}/java/baritone/**` → `mod/src/upstream/java/baritone/**` **nespremenjeno**, en commit `upstream import cabaletta/baritone v1.2.19 (d9cb2d9)`; `upstream` ni v `sourceSets` | D-026; M1 ga premakne v `core` z mehanskim commitom |
| M0.5 | `LICENSE` (LGPL-3.0 iz reference), `NOTICE.md` posodobljen z commitom uvoza | D-004 |
| M0.6 | Oblačno prevajanje: na Windowsu po `setupDecompWorkspace` poišči mapiran `forgeSrc-1.12.2-14.23.5.2847*.jar` v Gradle predpomnilniku in ga kopiraj v `tools/cache/` (gitignore); `tools/cloud-compile.sh` prevede `mod/src/main` + teste z `javac --release 8` | kot CNPC D-014 in `ladja_mod/.devsync` |
| M0.7 | **Sonda prevoda:** upstream proti `forgeSrc` (snapshot_20171003) — ali je profil napak enak §1 raziskave (81 / 38)? zapiši razliko v `docs/06-RAZISKAVA.md` | preveri, da so preimenovanja v tem mappingu enaka |
| M0.8 | **Headless harness:** `SyntheticWorld` (builder: `fill`, `set`, `wall`, `stairs` → `Long2ObjectMap<Chunk>`), `BootstrapOnce` (JUnit rule), test `HarnessTest` | `new Chunk(null,x,z)` + `ExtendedBlockStorage`; §4 raziskave |
| M0.9 | Sonda `IBlockAccess`: katere metode `BlockStateInterfaceAccessWrapper` in `MovementHelper` kličejo na pravem `World` (biome, svetloba, `isSideSolid`) → seznam v `docs/06-RAZISKAVA.md` §4 | odgovori odprto vprašanje iz §4 |
| M0.10 | `tools/portmap.py` in `tools/automatone_roadmap.py` v repozitoriju (generirata `docs/porting/*.md` iz referenc) | ponovljivost dokumentov |
| M0.11 | `runClient` in `runServer` smoke | log vsebuje `npcbaritone` |

## Merila sprejema

| # | Merilo | Kako |
|---|---|---|
| A1 | `.\dev.ps1 build` zelen na Windowsu | log v `docs/build-logs/` |
| A2 | `runClient` naloži mod | `Forge Mod Loader has successfully loaded N mods`, `npcbaritone` v seznamu |
| A3 | `runServer` (dedicated) naloži mod | isto |
| A4 | `HarnessTest` zelen v oblaku in na Windowsu | JUnit poročilo |
| A5 | Commit uvoza obstaja; `git diff d9cb2d9:src <uvoz>` je prazen za `api` in `main` | ukaz v `04-STANJE.md` |
| A6 | Profil napak upstream proti CNPC mappingu zapisan | §1 raziskave dopolnjen |

## Ni v obsegu

Nobena sprememba Baritonove kode. Nobena entiteta, ukaz ali izris.

## Tveganja

- ForgeGradle 2.3 potrebuje assets prek HTTPS popravka → uporabi CNPC `prepare-assets.ps1` (R-08).
- Git na mapi potrebuje brisanje lock datotek: seja mora imeti dovoljenje za brisanje v tej mapi.

## Stanje (2026-09-24)

| # | Stanje |
|---|---|
| M0.1–M0.3 | napisano, prevedeno v oblaku; čaka `.\dev.ps1 build` |
| M0.4, M0.5 | **narejeno** — `3a4ea58`, `NOTICE.md` |
| M0.6 | **narejeno** — `tools/cloud-compile.sh` |
| M0.7 | **narejeno** — RAZISKAVA §1a (A6) |
| M0.8 | zeleno v oblaku (16/16); čaka Gradle test na Windowsu (A4) |
| M0.9 | **narejeno** — RAZISKAVA §4a |
| M0.10 | **narejeno** (orodji v repozitoriju od priprave) |
| M0.11 | `smoke-server.ps1` napisan; čaka zagon (A2, A3) |
