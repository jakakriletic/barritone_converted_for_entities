# M5 — Strežniška zmogljivost in skaliranje

**Velikost:** M · **Odvisen od:** M2 (priporočeno M3) · **Odločitve:** D-013, D-017

## Cilj

Dokazati s številkami, koliko NPC-jev z Baritonom strežnik prenese, in da glavna nit
nikoli ne čaka na iskanje.

## Naloge

| # | Naloga | Automatone vzorec |
|---|---|---|
| M5.1 | `SearchExecutor`: fiksen bazen (config), prednostna vrsta, strop vrste, zavrnitev `queue_full` | `a67d6901` |
| M5.2 | Deljenje iskanj: isti cilj (±1) + začetek v istem chunku → en rezultat za vse | — (CNPC M5.6) |
| M5.3 | NPC časovne omejitve in `planAhead` iz profila; nastavitev po meritvah | — |
| M5.4 | `ChunkSnapshot` meritev: µs kopije vs. rob 4/8/16/32 | D-013 |
| M5.5 | Ugašanje: `FMLServerStoppingEvent` prekine iskanja, bazen se zapre | `e3d476d5` |
| M5.6 | Števci: iskanj/s, µs/iskanje p50/p95, čakanje v vrsti, delež deljenih, µs na glavni niti na tick (posnetek + izvajanje) | — |
| M5.7 | Tečaj **T4** (stres) + `t4-run.ps1`, 3 ponovitve v svežem svetu (CNPC D-016) | — |
| M5.8 | Stres z rušenjem blokov ob poteh: 0 izjem, pravilna ponovna iskanja | D-013 |

## Merila sprejema

| # | Merilo | Meja (predlog, potrdi uporabnik) |
|---|---|---|
| A1 | prispevek glavne niti pri 200 mobih | p95 ≤ 2 ms/tick |
| A2 | µs kopije posnetka pri robu 8 | p95 < 100 µs |
| A3 | niti izven bazena | 0 |
| A4 | izjeme v 1 h stresa z rušenjem | 0 |
| A5 | tabela 50/200 mobov: iskanj/s, µs p50/p95, delež deljenih | zapisana v `docs/meritve/` |

**Vrata V3:** A1 v meji ⇒ M6. Sicer: manj niti, krajše časovne omejitve, večje deljenje;
če še vedno ne, se meja za CNPC (M7) omeji na število NPC-jev z Baritonom.
