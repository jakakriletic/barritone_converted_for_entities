# M14 — Gradnja po shemah (en ali več workerjev)

**Velikost:** L–XL · **Odvisen od:** M11 (roke, inventar), M12.4 (odlaganje/jemanje)
· **Odločitve:** D-032–D-034, D-036, D-037, D-038

## Cilj

Worker ali skupina workerjev zgradi shemo, ki jo porabnik poda **programsko** (npr.
generator vasi s predogledom) ali iz datoteke, z materialom iz inventarja, pravilno
orientacijo blokov in napredkom, ki ga porabnik lahko prikaže.

## Kaj ostane porabniku (ni v tem milestonu)

Generiranje stavb in vasi, predogled (ghost bloki na klientu), dobava materiala iz skrinj,
odločanje, kdo gradi kaj. Knjižnica mu za to da `ISchematic`, `requiredMaterials()` in
dogodke.

## Naloge

| # | Naloga | Vir |
|---|---|---|
| M14.1 | Sheme v API 3 (`api/work/schematic`): `ISchematic`, `IStaticSchematic`, `Fill`, `Walls`, `Shell`, `Composite`, `Mask`, `Replace`, `Substitute`, maske (`Sphere`, `Cylinder`, operatorji); JUnit: `desiredState` za vsako vrsto na majhnem primeru | 1.2.19 `api/schematic` |
| M14.2 | Bralniki datotek iz `config/npcbaritone/schematics`: MCEdit, Sponge, Litematica (1.2.19 `format/defaults`) + vanilla `.nbt` (`Template`); JUnit: ena majhna datoteka na format → pričakovana stanja | 1.2.19 `DefaultSchematicFormats` |
| M14.3 | `BuilderProcess` port nad rokami in inventarjem: `placementPlausible` z `FakePlayer` rotacijo, `buildIgnoreExisting`, `buildIgnoreDirection`, `buildIgnoreProperties`, nadomestki, `buildInLayers`, `layerOrder`, `layerHeight`, `breakFromAbove`, `buildRepeat`, `mapArtMode`; rušenje napačnih blokov v shemi | 1.2.19 `BuilderProcess` (1137 vr.); Automatone `b3d431f8` (lestve) |
| M14.4 | Bloki z oporo in dvodelni: vrstni red (trdni → odvisni: baklje, lestve, znaki, vrata, postelje, preproge), `canPlaceBlockAt` pred poskusom, dvodelni bloki prek vanilla predmeta (`ItemDoor`, `ItemBed` postavita oba dela) | nov (R-20) |
| M14.5 | Material: `IBuildJob.requiredMaterials()` (seznam predmetov za celo shemo in za preostanek), `onMissingMaterials`, čakanje na dostavo, profil `creativeBuild` | nov |
| M14.6 | `IBuildJob` za več workerjev (D-037): pasovi, rezervacije blokov, plasti po vrsti, sproščanje ob zatiku ali `release`; JUnit samo za logiko rezervacij in pasov (deterministična, brez sveta) | nov (R-22) |
| M14.7 | Dogodki `onBlockPlaced`, `onProgress(%)`, `onStuck(pos, razlog)`, `onDone`; `/npcb worker build <datoteka> | fill …` | — |
| M14.8 | Tečaj **T8** (hiše) | — |
| M14.9 | Tečaj **T9** (stres mešanica, stopnja W) | — |

## Tečaj T8 — hiše

| # | Shema | Preverja |
|---|---|---|
| H1 | kamnita kocka 7×5×7 z vrati in dvema oknoma | osnova, vrata (dvodelni) |
| H2 | lesena hiša: tla, stene iz hlodov (os), streha iz stopnic (orientacija, kotne), plošče, ograja, lestev, bakle na steni, postelja, preproga | orientacija in bloki z oporo |
| H3 | H1 na neravnem terenu (hrib, luknja, drevo v tlorisu) | rušenje napačnih blokov, gradnja iz zraka (steber, most) |
| H4 | H2 z manjkajočim materialom (brez stopnic) | `onMissingMaterials` s pravilnim seznamom; nadaljuje po dostavi |
| H5 | H2 programsko sestavljena (ne iz datoteke), 3 workerji | `IBuildJob`, rezervacije |

Vsaka hiša z 1 workerjem, H1–H3 in H5 še s 3 workerji. Primerjava stanja sveta s shemo po
koncu (vse lastnosti razen `buildIgnoreProperties`).

## Tečaj T9 — stres workerjev (stopnja W)

20 workerjev: 10 `mine` (T6 kamnolom), 5 `farm` (T7), 5 `build` (H2 × 5), 10 min, prisilno
naloženi chunki, `PerfMeter` z razliko proti isti sceni brez procesov.

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | H1–H3 z 1 workerjem: 100 % blokov po shemi, brez ročnega posega |
| A2 | H1–H5: `onStuck` se razreši sam (ponovni poskus, drug pristop) ali ga worker javi in sprosti rezervacije; 0 neskončnih zank (vsak `onStuck` ≤ 30 s) |
| A3 | 3 workerji: 0 podvojeno postavljenih ali porušenih blokov; čas ≤ 0,6 × čas enega workerja |
| A4 | H4: seznam manjkajočega = dejansko manjkajoče; po dostavi konča |
| A5 | T9: stopnja W (D-038), 3 ponovitve, mediana in razpon |
| A6 | invariante M11 (območje, ohranitev predmetov, 0 izjem) na vseh hišah |
