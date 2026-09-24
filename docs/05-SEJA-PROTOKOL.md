# 05 — Protokol seje

Kako seja (človek ali agent) začne, dela in konča. Namen: da naslednja seja nadaljuje
brez ugibanja in da nič ni "narejeno", kar ni preverjeno.

---

## 1. Začetek

1. `README.md` → `docs/04-STANJE.md` (zadnji zapis) → README trenutnega milestona.
2. Preveri, da sta reference na pravih commitih:
   `git -C references/baritone-1.12.2 rev-parse HEAD` = `d9cb2d91a06501c5bcba2181509d0df80361f413`,
   `git -C references/automatone rev-parse HEAD` = `843b8397a92efdf0c934a9e892ae34d286face03`.
3. Preberi odločitve, ki jih milestone navaja v glavi.
4. V `04-STANJE.md` zapiši, kaj ta seja namerava (en odstavek).

## 2. Med delom

- **En commit = ena stvar.** Mehanske spremembe (relokacija, izrez, preimenovanja) so vedno
  ločen commit brez vsebinskih sprememb (D-026).
- Automatonov commit iz `AUTOMATONE-ROADMAP.md` se **prebere pred** delom na isti
  datoteki: `git -C references/automatone show <hash>`.
- Nobena koda v `core/` ne kliče sveta mimo `BlockStateInterface` (D-012); lint test to
  lovi, ne zanašaj se nanj.
- Vsak nov JUnit test najprej pade (dokaz, da testira), nato gre skozi.
- Ko odločitev ne drži več: nova odločitev v `02-ODLOCITVE.md`, stara dobi status
  "zamenjana z D-xxx". Nikoli tiho.

## 3. Preverjanje (kaj pomeni "narejeno")

| Raven | Kdo | Kaj dokaže |
|---|---|---|
| prevod + JUnit v oblaku | seja | sintaksa, API podpisi, headless logika |
| `.\dev.ps1 build` na Windowsu | uporabnik ali seja z dostopom do računalnika | mappingi, shadow/reobf, pravi Forge |
| scenarij v igri (`*-run.ps1`) | uporabnik | obnašanje, merila milestona |
| dedicated server smoke | uporabnik | stran strežnika, D-024 |

Naloga je **narejena**, ko gre čez vse ravni, ki jih zahteva njen milestone. Do takrat je
v `04-STANJE.md` označena kot "prevedeno, čaka na zagon".

## 4. Meritve

- Pogoj meritve: prisilno naloženi chunki, svež svet, en tek = ena ponovitev (CNPC D-011,
  D-016).
- Tri ponovitve; poroča se mediana in razpon; "izboljšava" je samo čez izmerjeni razpon.
- Zapis strojno berljiv (CSV/JSON), tabela v `.md` je samo prikaz.

## 5. Konec seje

1. `04-STANJE.md`: kaj je narejeno, kaj prevedeno a ne preverjeno, kaj je blokirano, in
   **naslednji korak kot en ukaz ali ena naloga**.
2. Hitri status v `README.md`, če se je faza spremenila.
3. Commit samo sprememb te seje (glej `AGENTS.md`).

## 6. Izdaja (kontrolni seznam)

- [ ] `LICENSE` (LGPL-3.0) in `NOTICE.md` v jarju in ob njem (D-004)
- [ ] source jar ob izdaji
- [ ] `api` jar z različico API
- [ ] SHA-256 vsota, zapis commita
- [ ] dedicated server smoke na izdanem jarju (ne dev) — refleksija, SRG (R-14)
- [ ] vanilla klient se poveže (D-024)
