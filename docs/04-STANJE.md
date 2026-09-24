# 04 — Stanje (živ dnevnik)

Najnovejši zapis je na vrhu. Vsaka seja doda zapis ob začetku in koncu.

---

## 2026-09-24 (2) — M0: okolje, uvoz, headless harness

**Namen seje:** M0 do točke, ko ostane samo Windows build in zagon v igri.

**Narejeno (preverjeno v oblaku)**

- **Git:** `3a4ea58` upstream import (M0.4, D-026) — 311 datotek, bitno enakih
  `src/{api,main}` v `d9cb2d9` (A5: `bash tools/check-upstream-import.sh`);
  `1a38f97` dokumentacija priprave + submoduli. Lokalni git `user.name=jaka`.
- **M0.6:** `tools/cloud-compile.sh` — prevod `mod/src/main` + `mod/src/test` z
  `javac --release 8` in JUnit brez Gradla. `tools/cache/` (gitignore) ima
  `forgeSrc-1.12.2-14.23.5.2847.jar` (snapshot_20171003) in 42 knjižnic iz Gradle
  predpomnilnika (seznam: `versionJsons/1.12.2.json` + junit 4.13.2, hamcrest 1.3,
  jsr305 3.0.1, launchwrapper, asm 5.2, …).
- **M0.7 (A6):** profil napak upstream proti snapshot_20171003 = 81 / 43 / 38, istih 38
  mest kot v `MCP-PREIMENOVANJA.md` → RAZISKAVA §1a.
- **M0.9:** sonda `IBlockAccessProbeTest` → RAZISKAVA §4a. Iskanje poti bere samo
  `getBlockState`; **past za M1.5:** `Chunk.getBlockState` rabi `World` (NPE brez njega).
- **JUnit v oblaku: 16/16 zelenih** (`HarnessTest` 8, `IBlockAccessProbeTest` 3,
  `NpcbConfigTest` 5). Mutacijski preverbi: pokvarjen `SyntheticWorld.set` → 6 padcev,
  odstranjen clamp v configu → 1 padec (testi res testirajo).

**Prevedeno, čaka na zagon (Windows)**

- **M0.1** `mod/` Gradle projekt (FG 2.3-SNAPSHOT, wrapper 4.9 iz CNPC, Forge
  14.23.5.2847, snapshot_20171003); `upstream` ni v `sourceSets`.
- **M0.2** `dev.ps1` (Java 8: `NPCB_JAVA8_HOME` → `..\customNPC_rework\.tools\jdk8` →
  `C:\Program Files\Java\jdk1.8.0_202`), `prepare-assets.ps1` (kopija iz CNPC).
- **M0.3** `NpcBaritoneMod` (`@Mod npcbaritone`, `@NetworkCheckHandler` vedno `true`),
  `NpcbConfig` (`config/npcbaritone.cfg`: sekcije search/profile/movement/debug, obrezovanje mej).
- **M0.8** `SyntheticWorld`, `BootstrapOnce`, `HarnessTest` — zeleni v oblaku, na Windowsu še ne (A4).
- **M0.11** `smoke-server.ps1` (A3); runClient (A2) ročno.

**Blokirano:** nič.

**Naslednji korak (uporabnik, PowerShell v korenu repozitorija):**

```powershell
.\dev.ps1 setupDecompWorkspace --offline   # če pade zaradi odvisnosti: brez --offline
.\dev.ps1 build --offline                  # A1 + A4 (JUnit v Gradlu)
.\smoke-server.ps1 -AcceptEula             # A3
.\dev.ps1 runClient --offline              # A2: v logu 'npcbaritone' in 'successfully loaded'
```

Ko je vse zeleno: M0 zaključen → M1.1 (mehanska relokacija).

---

## 2026-09-24 — Priprava projekta

**Narejeno**

- Raziskava: prevod Baritona v1.2.19 proti Forge 1.12.2 (81 napak, 43 v Elytri, 38 drugod),
  velikost in klientske odvisnosti, vanilla mehanika z `javap`, headless sonda
  (`Bootstrap` + sintetični chunk), CustomNPC rework okolje in uporaba navigatorja,
  Automatonova časovnica (167 commitov), `ladja_mod` shipyard. → `docs/06-RAZISKAVA.md`
- 26 odločitev z dokazi in preverbami → `docs/02-ODLOCITVE.md`
- Arhitektura, faze M0–M10, register tveganj, protokol seje
- `docs/porting/`: port map vseh 348 izvornih datotek, Automatone roadmap, MCP preimenovanja
  (generirano s `tools/*.py`)
- Git repozitorij inicializiran (`main`, brez commita); reference pripete kot submoduli:
  - `references/baritone-1.12.2` @ `d9cb2d9` (v1.2.19)
  - `references/automatone` @ `843b8397`

**Prevedeno, čaka na zagon:** nič (kode še ni).

**Blokirano:** nič.

**Odprto (namenoma, rešuje M0)**

- Ali ima CNPC mapping (snapshot_20171003) enak profil napak kot §1 (M0.7).
- Katere `IBlockAccess` metode potrebujejo pravi `World` (M0.9).

**Naslednji korak:** M0.1 — Gradle projekt v `mod/` po vzoru CNPC okolja
([`milestones/M0-okolje-in-uvoz`](../milestones/M0-okolje-in-uvoz/README.md)).
Pred prvim commitom preglej `git status` (submoduli in dokumenti so pripravljeni, niso commitani).
