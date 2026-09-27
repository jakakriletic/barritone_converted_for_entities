# M15 — Sledenje kot proces

**Velikost:** S · **Odvisen od:** M11.7 (prednost procesa) · **Odločitve:** D-036, D-037

## Cilj

Worker (ali navigacijski NPC) sledi entiteti ali skupini entitet kot **proces**: ohrani
prednost pred AI taski (D-036) in ob workerju sme med sledenjem rušiti/postavljati v
območju. Razlika do M6.6 (cilj, ki sledi, v navigatorju): proces sam izbira tarčo po filtru
in razdalji in ga AI taski ne prepišejo.

## Naloge

| # | Naloga | Vir |
|---|---|---|
| M15.1 | `FollowProcess` port: tarča po entiteti ali filtru (tip, lastnik), razdalja, `onFailed(target_lost)` | 1.2.19 `FollowProcess` (126 vr.) |
| M15.2 | API: `INpcWorker.follow(...)` in `NpcBaritone.follow(entity, ...)` za ne-workerje (samo plast 1 + proces) | nov |
| M15.3 | Korak v `/npcb selftest`: sledi igralcu 100 blokov čez T1 teren, igralec izgine → `onFailed` | — |

## Kaj se ne prenese

`ExploreProcess` (D-037, D-014): NPC ne hodi v nenaložen svet, entiteta izven dosega
igralca se ne tiktaka.

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | selftest korak OK v dev klientu in na dedicated strežniku |
| A2 | `EntityAIWander` med sledenjem ne prevzame navigacije |
