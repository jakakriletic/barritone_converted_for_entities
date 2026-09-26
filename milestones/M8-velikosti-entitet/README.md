# M8 — Velikosti entitet

**Velikost:** M · **Odvisen od:** M6 · **Odločitve:** D-019 (ta milestone jo odpravi)

## Cilj

Baritone vodi entitete poljubne širine in višine (CNPC size 1–10), ne samo 1×2 stolpca.

## Naloge

| # | Naloga | Automatone vzorec |
|---|---|---|
| M8.1 | `CalculationContext`: `width`, `height`, `requiredSideSpace` iz entitete | `324bd259` |
| M8.2 | `MovementTraverse` z dimenzijami, pozicija rušenja | `324bd259`, `943ae1be`, `a9c929f0` |
| M8.3 | `MovementDiagonal` | `f3b5b24a` |
| M8.4 | `MovementAscend` | `39da8286` |
| M8.5 | `MovementParkour` | `5d9ddabe` |
| M8.6 | `MovementDownward`, `MovementPillar` | `0a6399f9`, `3a08d43e` |
| M8.7 | Majhni bloki in majhne entitete (širina < 1) | `7b3a8a43` |
| M8.8 | Golden testi G1–G12 za širine 0,3/0,6/1,2/2,0 in višine 0,9/1,8/2,6/3,6 | — |
| M8.9 | Tečaj **T3** (T1 + T2 za size 1, 3, 5, 7, 10) | — |
| M8.10 | D-019 se zamenja z novo odločitvijo (meje, ki jih M8 dokaže) | — |

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | T3: ≥ 9/10 odsekov za vsako velikost |
| A2 | nobena velikost se ne zatakne v 1-blokovni reži, v katero ne gre (FAILED/obhod, ne tavanje) |
| A3 | golden testi za vse kombinacije zeleni |

## Izid (2026-09-26): zaključen

T3 na dedicated strežniku (`t3-run.ps1`, tretji tek 15:16): **96/100**, A1 najslabše 9/10 za
vsako velikost, A2 19/19 pričakovanih neuspehov FAILED (0 TIMEOUT), A3 golden 16 velikosti.
Prva dva teka sta razkrila napaki tečaja (ne jedra): `EntityZombie.setSize` velikosti ne
uveljavi (`1f4417b`) in T3 mora teči z `movement.largeEntities=true` (`7946c65`).
Podrobnosti in opažanja (size 1/3 zdrs z lestve, size 10 škoda ob stebrih): `docs/04-STANJE.md`.
Meritve: `docs/meritve/m8/t3-20260926-151645*.csv`.

## Stanje (2026-09-25, veja `m8-velikosti` iz `m6-navigator`)

Model in meje: **D-028** (zamenja D-019). Prevedeno in headless preverjeno v oblaku
(106/106 JUnit); čaka `.\dev.ps1 build --offline` in `.\t3-run.ps1` na Windowsu.

| # | Stanje |
|---|---|
| M8.1 | `EntitySize` (`sideSpace`, `heightBlocks`, `forwardSpan`, `canDescend`) v `CalculationContext` (`size`, `requiredSideSpace`, `height`, `sizeAware`) in v vsakem `Movement`. Standardna velikost → upstream veja; `SizeAwareEquivalenceTest` dokaže, da je splošna veja zanjo enaka (6 terenov × 2 profila × vsi premiki, > 20 000 izvedljivih) |
| M8.2 | `MovementTraverse.costSized`: prednja ploskev okvira (`dest + smer·s ± s`, `h` blokov) + ciljni stolpec; `positionsToBreak` po velikosti; izvajalec cilja `dest.up()` namesto `positionsToBreak[0]` (Automatone `a9c929f0`) |
| M8.3 | `MovementDiagonal.costSized`: višina `h` za oba vogala in cilj; `sideSpace > 0` → brez diagonale |
| M8.4 | `MovementAscend.costSized`: strop nad celim okvirom na `y + h`, skok na blok pod prednjim robom (`positionToPlace`), prednja ploskev `y+1 … y+h`; `headBonkClear` na `src.up(h)` |
| M8.5 | `MovementParkour`: samo standardna velikost (Automatone `5d9ddabe` enako za velike) |
| M8.6 | `MovementPillar`: višina `h`, široke ne plezajo; `MovementDownward`: okvir pod entiteto, lestev samo ozke. **Descend/Fall** (Automatone ju ni prilagodil): stolpci pred ciljem `k = 0 … max(s, forwardSpan)`, vsaka vrsta padca; širina ≥ 2 se ne spušča |
| M8.7 | nizke entitete (višina ≤ 1) gredo skozi reže visoke 1 (G14) in pod ploščo 1,5 (T3 s1 T1/7); zaznava majhnih blokov (`7b3a8a43`) ni potrebna — ne postavljamo blokov |
| M8.8 | `GoldenSizeTest`: G1–G12 + G13 (odprtina 3×4) + G14 (luknja 1×1) za 16 velikosti; poročilo `build/reports/npcb/golden-sizes.txt` |
| M8.9 | `CourseT3` (T1 pri izhodišču, T2 +60 X), `/npcb course t3 build|run`, tekač nastavi velikost (`Entity.setSize`), prisili chunke, CSV `npc_size,width,height`; `CourseT3PathTest` (headless, superflat pod tečajem); `t3-run.ps1` |
| M8.10 | D-028; navigator `fits()` s stikalom `movement.largeEntities` (privzeto false) |

### Pričakovani neuspehi T3 (`CourseT3.EXPECT_FAIL`)

| size | odsek | razlog |
|---|---|---|
| 7 | T1/6 | hodnik visok 2, entiteta 2,52 (3 bloki) |
| 7 | T2/1 | vrata visoka 2 |
| 10 | T1/5 | okvir 3×3 na cilju zadene steber |
| 10 | T1/6 | hodnik 1×2 |
| 10 | T2/1, T2/3 | vrata / ograjna vrata široka 1 |
| 10 | T2/4, T2/5 | široke entitete ne plezajo; padec 5 > 3 |
| 10 | T2/9 | reže med kaktusi široke 1 |

Size 1 sme skozi režo 1,5 pod ploščo (T1/7). Size 10 na T1/2, T1/7, T1/9 obide zid čez
sosednjo progo (superflat med progami) — pravilen obhod, ne napaka.

### Zagon

```powershell
.\mod\gradlew.bat --stop; .\dev.ps1 build --offline; .\t3-run.ps1
```

Tek traja do ~1 h (100 odsekov × največ 30 s). Rezultati: `docs\meritve\m8\t3-*.csv`.
