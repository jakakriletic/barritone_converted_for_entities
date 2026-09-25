# M4 — Interakcije in varnost

**Velikost:** M · **Odvisen od:** M2 (priporočeno M3) · **Odločitve:** D-015, D-016

## Cilj

NPC gre skozi vrata, po lestvah, čez vodo in se varno spusti — ne da bi kadarkoli porušil
ali postavil blok.

## Naloge

| # | Naloga | Automatone vzorec |
|---|---|---|
| M4.1 | `EntityInteractions` (namesto `IPlayerController`): odpri/zapri lesena vrata, ograjna vrata, lopute neposredno (`BlockDoor.toggleDoor` …); železna vrata so neprehodna | `3216de48`, `b1899f30` |
| M4.2 | `MovementTraverse`: vrata brez desnega klika igralca (vhod `CLICK_RIGHT` → `EntityInteractions`) | `3216de48` |
| M4.3 | Lestve gor/dol (`MovementPillar`/`Downward`), trte izklopljene v profilu | `d5ff55ce` |
| M4.4 | Voda: cene, plavanje, sprint plavanje, padec v vodo | `77aadb8a`, `64c680a9`, `7f6f8034`, `1fb7ad15`, `885027f1`, `456aa6bf`, `8c12848f` |
| M4.5 | Kisik: cena poti pod vodo iz `getAir()`; brez utopitve | `46a3b4d1`, `9b018aaa`, `51694ee9` |
| M4.6 | Nevarnosti: lava, ogenj, kaktus, magma — Baritone jih že izogiba; test, da ostane tako | — |
| M4.7 | Izogibanje mobom (`Avoidance`) iz strežniških entitet, izklopljeno privzeto | `7dd013f0` |
| M4.8 | Tečaj **T2** + `t2-run.ps1` | — |
| M4.9 | Varnostni test: med T1+T2 števec sprememb blokov, ki jih povzroči entiteta, razen vrat = 0 | D-015 |

## Merila sprejema

| # | Merilo | Meja |
|---|---|---|
| A1 | T2 odsekov doseženih | ≥ 9/10; železna vrata = obhod ali FAILED |
| A2 | porušenih/postavljenih blokov | 0 |
| A3 | škode od padca ali utopitve v T1+T2 | 0 |
| A4 | vrata za NPC-jem | zaprta (kot jih je našel) |

## Stanje (2026-09-25, veja `m4-interakcije` iz `m3-vidnost`)

| # | Stanje |
|---|---|
| M4.1 | `forge/EntityInteractions`: `CLICK_RIGHT` odpre lesena vrata ali ograjna vrata na blokih trenutnega premika (ne v žarku pogleda — obrat je omejen, D-011); ko jih NPC zapusti (> 1,5 bloka, škatla jih ne seka), jih zapre; ob `detach` zapre vse svoje. Nič drugega ne spremeni (D-015). Prevedeno |
| M4.2 | jedro, Automatone `3216de48` + `b1899f30`: `MovementTraverse.tryOpenDoors` za vrata na cilju **in** izhodišču; prehodna samo lesena vrata in ograjna vrata. **Upstream 1.12.2 je imel zamenjana argumenta `isDoorPassable(ctx, src, dest)` in zaprtih vrat pred sabo ni nikoli odprl.** Prevedeno |
| M4.3 | lestve: headless A* uporabi `Pillar` gor in `Downward` dol (T2/4, T2/5); v igri čaka tek. Trte: nespremenjeno (upstream `allowVines` privzeto izklopljen) |
| M4.4, M4.5 | voda in kisik: **najprej meritev** na T2/6, T2/7, T2/10; headless A* gre čez vodo z `Descend/Ascend` po površini — ali mob (ki v vodi tone) to zmore, pokaže tek |
| M4.6 | lava, kaktus: headless A* se jima izogne (T2/8, T2/9); ogenj in magma še brez testa |
| M4.7 | izogibanje mobom: ni začeto (privzeto izklopljeno, ni v merilih) |
| M4.8 | `CourseT2` + `t2-run.ps1`; headless: vseh 10 izidov pravilnih, poti gredo skozi vrata/lestve, velik padec samo v vodo |
| M4.9 | `CourseRunner`: podpis blokov območja tečaja ob začetku in koncu (odpiranje vrat ni sprememba) → `blocks_changed` v `NPCB-COURSE-DONE`; stolpca `damage` in `openables_closed`; velja za T1 in T2 |

**T2** (vsaka proga zaprta s stenami višine 4, sicer bi NPC v superflatu oviro obšel):

| # | Odsek | Pričakovano |
|---|---|---|
| 1 | lesena vrata v zidu | REACH, vrata po prehodu zaprta |
| 2 | železna vrata v zidu | FAILED, nič porušenega |
| 3 | ograjna vrata v ograji | REACH, zaprta |
| 4 | lestev gor 5 | REACH |
| 5 | lestev dol 5 | REACH, padec ≤ 3,5 |
| 6 | vodni jarek 1 globok | REACH |
| 7 | vodni jarek 3 globok | REACH (plavanje) |
| 8 | lava z mostom ob strani | REACH, 0 škode |
| 9 | kaktusi z režami širine 1 | REACH, 0 škode |
| 10 | padec 10 v bazen globine 2 | REACH, 0 škode |

### Rezultat (2026-09-25 11:46)

**Zaključen:** T2 10/10, 0 spremenjenih blokov, 0 škode, vrata 2/2 zaprta; T1 regresija zelena. Podrobnosti v `docs/04-STANJE.md` (5).

### Preverba pri uporabniku

`.\dev.ps1 build --offline; .\t2-run.ps1; .\t1-run.ps1` → vse vrstice OK (T1 dobi še
`M4 A2 spremenjenih blokov v T1: 0`). Odseki, ki padejo, so podatek za M4.3–M4.5 (voda,
lestve), ne napaka skripta.
