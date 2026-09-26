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

| # | Merilo | Meja (D-027) |
|---|---|---|
| A1 | prispevek glavne niti pri 200 mobih — **stopnja B (zdaj)** | p95 ≤ 5 ms/tick |
| A1+ | isto — **stopnja A (izpolnjena 2026-09-27: 1,64 ms)**, pogoj za M7 | p95 ≤ 2 ms/tick |
| A2 | µs kopije posnetka pri robu 8 | p95 < 100 µs |
| A3 | niti izven bazena | 0 |
| A4 | izjeme v 1 h stresa z rušenjem | 0 |
| A5 | tabela 50/200 mobov: iskanj/s, µs p50/p95, delež deljenih | zapisana v `docs/meritve/` |

**Vrata V3:** A1 (stopnja B) v meji ⇒ M6. A1+ ni pogoj za M6, je pa pogoj za M7 (D-027). Sicer: manj niti, krajše časovne omejitve, večje deljenje;
če še vedno ne, se meja za CNPC (M7) omeji na število NPC-jev z Baritonom.

## Stanje (2026-09-25, veja `m5-zmogljivost` iz `m4-interakcije`)

| # | Stanje |
|---|---|
| M5.1 | `core/SearchExecutor`: N niti iz configa (`search.threads`, privzeto 2), `PriorityBlockingQueue` (kvadrat razdalje do najbližjega igralca, nato FIFO), meja `search.queueLimit` (64) → `queue_full`; poimenovane niti `npcbaritone-search-*`. JUnit 5/5 |
| M5.2 | deljenje iskanj: **ni narejeno** — najprej T4 pokaže, ali je potrebno (števec `shared` je pripravljen, zaenkrat 0) |
| M5.3 | časovne omejitve ostanejo iz `NpcProfile` (150/500/300/800 ms) do meritev |
| M5.4 | `SearchStats.SNAPSHOT_NANOS` in `SNAPSHOT_CHUNKS` ob vsakem iskanju (A2) |
| M5.5 | bazen se zažene v `FMLServerStartingEvent` in zapre v `FMLServerStoppingEvent` (čakajoča iskanja zavržena, tečaj in stres prekinjena) |
| M5.6 | `SearchStats` (iskanja, zavrnjena, µs iskanja, čakanje v vrsti, posnetek) + `forge/PerfMeter` (µs knjižnice na tick = A1, MSPT); `/npcb perf [reset]` |
| M5.7 | `forge/StressRunner` (`/npcb stress start <n> [polmer] [s] [rušenje-s] [x y z]`) + `t4-run.ps1` |
| M5.8 | rušenje: vsakih 5 s kamen na točko 3–8 naprej po poti naključnega NPC-ja (prejšnji odstranjen) |

**Kaj meri A1:** samo čas, ki ga na glavni niti porabi knjižnica (`baritone.tick()` s kopijo
posnetka in izvajalcem poti, `EntityInteractions`, `BaritoneMoveHelper`), seštet čez vse NPC-je v
ticku. Vanilla posodobitev moba (fizika, trki) ni vključena — ta bi bila tudi brez Baritona;
celoten MSPT je poročan poleg za kontekst.

**T4:** ravni svet, stebri 1×1×(2–3) na ~4 % površine in zidovi 5–12 × 3 (seme 20260925),
husk-i kot puppet, naključni cilj v polmeru 48 vsakič, ko NPC miruje ≥ 10 tickov (ali po 60 s),
10 s ogrevanja (JIT), nato meritev.

### Preverba pri uporabniku

```powershell
.\dev.ps1 build --offline
.\t4-run.ps1 -Repeats 1 -Seconds 60     # hitra preverba (~6 min)
.\t4-run.ps1                            # A1, A2, A3, A5: 3 × (50, 200), 120 s (~25 min)
.\t4-run.ps1 -Mobs 200 -Repeats 1 -Seconds 3600   # A4: 1 h z rušenjem
```

### Rezultat (2026-09-25 17:15)

**Stopnja B izpolnjena:** 200 mobov, glavna nit p95 mediana 3,27 ms [3,15–4,08] v 3 ponovitvah; A2, A3, A5 da. Podrobnosti v `docs/04-STANJE.md` (10).

**Stopnja A izpolnjena (2026-09-27):** po profilu (`t4-run.ps1 -Profile`) in dveh optimizacijah
izvajalca poti z enakim obnašanjem (`c9d5238`, `8deda36`): 200 mobov, glavna nit p95 mediana
**1,64 ms [1,56–1,66]** v 3 ponovitvah (prej 3,27), p50 1,37 ms, MSPT p95 3,3 ms; T1 in T2 10/10.
Meritve: `docs/meritve/m5/t4-200-20260927-00{2609,2837,3104}.csv`, `t4-summary-20260927-003110.csv`,
profila `profile-200-20260926-233438.csv` (pred) in `profile-200-20260927-001405.csv` (po prvi).

**A4 izpolnjen (2026-09-26, 16:34–17:34):** `.\t4-run.ps1 -Mobs 200 -Repeats 1 -Seconds 3600` —
**0 izjem** v 72 046 tickih, 84 754 iskanj (23,5/s, 0 zavrnjenih, 7308 neuspelih = nedosegljivi
cilji), 1440 preklopov blokov (rušenje), 2 niti; glavna nit µs/tick p50 2884, **p95 3895**, p99 6256,
max 59 532 (en tick); MSPT p95 6,35 ms; doseženih ciljev 95 % (69 819 / 73 492). Stopnja B drži tudi
po 1 h. Meritve: `docs/meritve/m5/t4-200-20260926-173405.csv`, `t4-summary-20260926-173410.csv`.
