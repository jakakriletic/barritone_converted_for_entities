# 07 — Register tveganj

Vsako tveganje ima **sprožilec** (merljiv znak, da se uresničuje), blažitev in milestone,
kjer se pokaže. Pregleda se na začetku vsakega milestona.

| ID | Tveganje | Verjetnost / vpliv | Sprožilec | Blažitev | Kje |
|---|---|---|---|---|---|
| R-01 | Baritonovi premiki so vezani na igralčevo fiziko (sneak ob robu, sprint skoki, `jumpMovementFactor`) in na mobu ne delujejo enako | srednja / visok | M2 A1 < 9/10 ali A2/A3 izven meje | D-010 način "kot igralec"; parkour izklopljen; sneak-rob se ne uporablja (brez postavljanja); vrata V2 | M2 |
| R-02 | Race condition: iskalna nit bere chunk, ki ga glavna nit spreminja ali razlaga | nizka / srednji | izjeme v stresnem testu; napačne poti skozi pravkar postavljen blok | omejen posnetek (D-013), Baritonova ponovna preverba premika, M5.8 stres z rušenjem | M5 |
| R-03 | Nalaganje ali generiranje chunkov zaradi branja sveta | srednja / visok | števec `ChunkEvent.Load` > 0 | D-012 + lint test v M1; M2 A4 | M1, M2 |
| R-04 | Glavna nit preobremenjena (posnetki + izvajanje × veliko NPC-jev) | srednja / visok | M5 A1 nad mejo | deljenje iskanj, manjši rob, stop za število Baritone NPC-jev; vrata V3 | M5 |
| R-05 | AI taski kličejo `clearPath`/`tryMoveTo` vsak tick → nevihta iskanj | visoka / srednji | telemetrija: iskanj/NPC/s > 1 pri istem cilju | debounce in "isti cilj" v adapterju (D-018); M6 A4 | M6, M7 |
| R-06 | Sinhroni `getPathTo*` klici pričakujejo pravo pot takoj | srednja / srednji | napadi/zig-zag v CNPC ne sprožijo | hibrid na vanilla `PathFinder` (D-018) | M6, M7 |
| R-07 | Soobstoj s klientskim Baritonom v enoigralskem načinu | srednja / visok | `LinkageError`, `NoSuchMethodError` | relokacija paketa (D-003); M2 A6 | M2 |
| R-08 | ForgeGradle 2.3 ne dobi assetov / odvisnosti | srednja / nizek | `runClient` pade pri assetih | CNPC `prepare-assets.ps1`, `--offline` po prvem zagonu | M0 |
| R-09 | CNPC governance (CNPC D-012) blokira integracijo | srednja / srednji | nova CNPC odločitev ni sprejeta | knjižnica je uporabna sama (vanilla mobi, ladje); M7 je vrata, ne predpogoj | M7 |
| R-10 | LGPL obveznosti pozabljene ob izdaji | nizka / srednji | izdaja brez source jarja ali NOTICE | kontrolni seznam izdaje v `05-SEJA-PROTOKOL.md` §6 | vsaka izdaja |
| R-11 | Obseg se razširi (rušenje, gradnja, letenje) | srednja / srednji | naloge izven README milestona | M10 je zaloga, ne načrt; vsaka ideja rabi odločitev | vedno |
| R-12 | Baritone 1.12.2 ima napake, ki so bile popravljene samo v novejših verzijah | nizka / nizek | napaka, ki je v Automatonu že popravljena | `AUTOMATONE-ROADMAP.md` + `git log` novejših vej Baritona za isto datoteko | M1–M4 |
| R-13 | Mapping razlike (stable_39 ↔ snapshot_20171003) skrijejo tiho napačno metodo z istim podpisom | nizka / srednji | test pade brez napake prevoda | M0.7 sonda; golden testi v M1 | M0, M1 |
| R-14 | Refleksija na `navigator`/`moveHelper` pri tujih entitetah odpove v obfuskiranem okolju | srednja / srednji | `attach` deluje v dev, ne v izdaji | SRG imena prek `ObfuscationReflectionHelper`; test na izdanem jarju v normalni instanci | M2 |
| R-15 | Nagib ladje (roll/pitch) zlomi predpostavko gravitacije po −Y | srednja / srednji | M9 A2 pade pri zavijanju | meja nagiba, čakanje nad mejo (M9.4) | M9 |
