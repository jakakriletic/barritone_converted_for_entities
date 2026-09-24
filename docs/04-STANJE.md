# 04 — Stanje (živ dnevnik)

Najnovejši zapis je na vrhu. Vsaka seja doda zapis ob začetku in koncu.

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
