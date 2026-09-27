# M13 — Farmanje

**Velikost:** S–M · **Odvisen od:** M12 (skener, odlaganje) · **Odločitve:** D-031–D-036

## Cilj

`worker.farm(območje)`: worker žanje zrele pridelke, ponovno seje iz inventarja, po želji
uporabi kostno moko in zorje zemljo, pridelke odlaga prek porabnika (kot M12).

## Naloge

| # | Naloga | Vir |
|---|---|---|
| M13.1 | `FarmProcess` port: pšenica, korenje, krompir, pesa (zrelost), bradavičnik, kakav, buča in melona (samo plod, steblo ostane), sladkorni trs in kaktus (od drugega bloka navzgor) | 1.2.19 `FarmProcess` (371 vr.) |
| M13.2 | Setev iz inventarja (`replantCrops`, `replantNetherWart`), kakav na džungelski hlod s pravilno stranjo (roke, D-032), kostna moka (stikalo, privzeto ne) | 1.2.19 |
| M13.3 | **Oranje** (Baritone ga nima): zemlja/trava v območju → njiva z motiko iz inventarja, samo na blokih, ki jih porabnik označi (`IWorkPermission.canTill`), privzeto ne | nov |
| M13.4 | Iskanje zrelih pridelkov prek `ServerBlockScanner` z filtrom stanja (zrelost), ne vsak tick | M12.1 |
| M13.5 | Tečaj **T7** in `/npcb worker farm` | — |

## Tečaj T7 — kmetija

Njive 9×9 s pšenico (polovica zrela), korenjem, krompirjem; vrsta buč in melon; sladkorni trs
ob vodi; 3 kaktusi; bradavičnik na duši pesku; kakav na hlodih; neorana zemlja (označena za
oranje); skrinja.

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | vsi zreli pridelki požeti, nezreli nedotaknjeni (primerjava stanj pred/po) |
| A2 | vsa požeta polja ponovno posejana, dokler je seme v inventarju; stebla buč in melon ter spodnji blok trsa in kaktusa ostanejo |
| A3 | označena zemlja zorana, neoznačena ne |
| A4 | invariante M11 (območje, ohranitev predmetov) |
| A5 | 2 cikla rasti (pospešeno z `randomTickSpeed` v tečaju) brez posega |
