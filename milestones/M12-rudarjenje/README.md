# M12 — Rudarjenje, pot do bloka, odlaganje

**Velikost:** M · **Odvisen od:** M11 · **Odločitve:** D-033–D-036, D-038 (številko potrdi
uporabnik ob začetku)

## Cilj

`worker.mine(100, stone)` v območju: worker poišče, izkoplje, pobere, ob polnem inventarju
porabniku javi, po odlaganju nadaljuje; več workerjev si deli iskanje blokov.

## Naloge

| # | Naloga | Vir / vzorec |
|---|---|---|
| M12.1 | `core/work/ServerBlockScanner` (D-035): naloženi chunki v območju, prek posnetka, v bazenu, izpostavljeni bloki (privzeto) ali vsi (`legitMine=false`), deljenje rezultata med workerji z istim filtrom in območjem; JUnit na `SyntheticWorld` (najde, ne naloži, spoštuje območje) | 1.2.19 `WorldScanner`; Automatone `b3da3410`, `a3081aac` |
| M12.2 | `MineProcess` port brez `CachedWorld` veje: `mine(količina, bloki)`, `mine(območje)`; `coalesce` ciljev; `minYLevelWhileMining`; pobiranje ostankov `EntityItem` (`mineScanDroppedItems`); količina se šteje po inventarju + odloženem (porabnik javi prek `IWorkerListener`) | 1.2.19 `MineProcess` (537 vr.); Automatone `678ecb02`, `d0970ec6` |
| M12.3 | `GetToBlockProcess` port: "pojdi ob najbližji blok tipa X" (skrinja, peč, delovna miza) | 1.2.19 `GetToBlockProcess` (255 vr.) |
| M12.4 | `INpcWorker.deposit(pos, filter)` / `withdraw(pos, filter, količina)`: prenos prek `IItemHandler` capability bloka, samo ob dosegu | nov |
| M12.5 | `BackfillProcess` (stikalo `backfill`): zasuje luknje, ki jih je worker naredil med hojo in niso del naloge | 1.2.19 `BackfillProcess` (143 vr.) |
| M12.6 | Dogodki `onProgress`, `onInventoryFull`, `onToolBroken`, `onDone`, `onFailed(razlog)`; `/npcb worker mine <blok> <n>` | — |
| M12.7 | Tečaj **T6** + meritev skenerja in 10 workerjev | — |

## Tečaj T6 — kamnolom

Območje 24×24×10 kamna z žilami premoga in železa, lava žep v sredini kamna, gramozni strop
v enem kotu, zaščiten steber (porabnik), skrinja ob robu. Testni runner igra porabnika: ob
`onInventoryFull` pokliče `deposit` v skrinjo in `resume`.

| # | Scenarij | Pričakovano |
|---|---|---|
| 1 | 1 worker, `mine(100, stone)`, inventar 9 mest | ≥ 100 kamna v inventarju + skrinji; vsaj eno odlaganje |
| 2 | 1 worker, `mine(20, coal_ore)`, `legitMine` privzeto | samo izpostavljene žile, nato `onDone` ali `onFailed(no_more)` |
| 3 | lava žep na poti | 0 škode, 0 smrti |
| 4 | krampa z 10 obrabe | `onToolBroken`, ne koplje z roko, dokler porabnik ne dostavi |
| 5 | `backfill=true`, cilj na drugi strani zidu | 0 lukenj izven ciljnih blokov |
| 6 | 10 workerjev, `mine(64, stone)` vsak | meritev A3/A4; 0 izjem |

Invariante iz M11 (območje, ohranitev predmetov, 0 izjem) veljajo za vse scenarije.

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | T6 scenariji 1–5 izpolnjeni (en tek) |
| A2 | 0 naloženih chunkov med T6 (`ChunkEvent.Load` števec) |
| A3 | skener: p95 µs na zahtevo zapisan; 0 µs na glavni niti (teče v bazenu) |
| A4 | scenarij 6: dodatek glavne niti p95 ≤ polovica stopnje W (D-038), 3 ponovitve, mediana in razpon |
