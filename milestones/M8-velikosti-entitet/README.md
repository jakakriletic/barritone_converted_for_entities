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
