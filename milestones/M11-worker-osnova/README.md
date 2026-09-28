# M11 — Worker osnova: roke, inventar, varovala

**Velikost:** L · **Odvisen od:** faza 0 (merge v `main`; stopnja A priporočena, ker se
dotika `LookBehavior`/izvajalca) · **Odločitve:** D-015, D-029–D-034, D-036

## Cilj

Registriran worker hodi z rušenjem in postavljanjem (tunel, steber, most) **znotraj
delovnega območja**, z orodji in bloki iz porabnikovega inventarja, brez podvajanja ali
izgube predmetov, brez sprememb izven območja. Neregistriran NPC se obnaša bitno enako kot
pred M11.

## Kaj prinese Automatone in česa ne

Automatone roke ima samo za igralce (`ServerPlayerController` nad vanilla
`interactionManager` + mixin za stanje kopanja); ne-igralci dobijo `DummyEntityController`
(glej uvod k razdelku H v `02-ODLOCITVE.md`). Vzorci za branje pred delom:
`0f8a664b` (`IPlayerController` kot komponenta — samo oblika vmesnika), `df9a13fe`
(zaščita v `CalculationContext`), `d3828043` (zaporedno rušenje), `43268b09`
(`MovementDownward` ruši napačen blok), `2fec595b` (postavljanje v `MovementAscend`),
`4a94645c` (air bridging), `72695f8c` (sprostitev uporabljenih predmetov), `06176954` (MLG).

## Naloge

| # | Naloga | Izhod |
|---|---|---|
| M11.1 | **Sonda** `FakePlayer` v 1.12.2 (javap + kratek test v dev strežniku): `connection == null` ob preklicanem `BreakEvent`; `tryHarvestBlock` in obraba orodja; `HarvestDropsEvent.getHarvester()`; `processRightClickBlock` in orientacija stopnic/hlodov/vrat po rotaciji; `getPlayerRelativeBlockHardness` z orodjem v roki | zapis v `docs/06-RAZISKAVA.md`; odločitev o praznem `NetHandlerPlayServer` (D-032) |
| M11.2 | API 3 okostje (D-039): `api/work` (`INpcWorker`, `WorkerSpec` z graditeljem, `IWorkPermission`, `IWorkerListener`, `WorkArea`), `NpcBaritone.worker(entity, spec)` / `release(entity)`, manifest `NpcBaritone-Api-Version: 3`, config `worker.enabled`; `apiJar` in `ApiJarTest` razširjena | porabnik API 1 se prevede nespremenjen |
| M11.3 | `IPlayerController` razširjen (clickBlock, onPlayerDamageBlock, hasBrokenBlock, resetBlockRemoving, processRightClickBlock, getGameType); `DummyEntityController` za ne-workerje; `forge/work/EntityHands` po D-032; port `BlockBreakHelper`, `BlockPlaceHelper`; `EntityInteractions` (vrata) ostane za vse | roke, razpoke, zamah |
| M11.4 | `WorkerInventory` nad `IItemHandler` (D-034): izbira orodja (`ToolSet` iz inventarja), throwaway bloki (`InventoryBehavior`), vračilo v `finally`, zajem dropov prek `HarvestDropsEvent`, `onInventoryFull`, `onToolBroken` | en vir resnice |
| M11.5 | Varovala (D-033): `CalculationContext.isProtected/canPlaceAgainst` (območje, D-031 seznam, spawn, meja sveta, `IWorkPermission`), izvedbena preverba + eventi; NPC privzeti `blocksToDisallowBreaking` (vsi `TileEntity`, postelje, vrata, spawner, portal) | cena INF, ne zatikanje |
| M11.6 | Premiki z rušenjem/postavljanjem v profilu `worker` (standardna veja, D-031): Traverse (most, rušenje), Ascend (postavljanje pod noge), Pillar, Descend/Downward, Parkour s postavljanjem; padajoči bloki (pesek, gramoz); porti Automatone commitov zgoraj | premiki iz jedra, ki so bili izklopljeni |
| M11.7 | Prednost procesa (D-036): `BaritonePathNavigate` vrne `false` in `BUSY`, ko proces teče; `INpcWorker.pause()/resume()`; prioritete v `PathingControlManager` | AI taski ne kradejo |
| M11.8 | `/npcb worker register|release|area|goto` (inventar = skrinja ob entiteti, za test); `/npcb selftest` dobi korak "worker tunel" (razpoke in orodje v roki vidni v klientu) | ročna pot za razvoj |
| M11.9 | Tečaj **T5** (`CourseT5`, `t5-run.ps1`) z invariantami v runnerju (spodaj); JUnit samo za: ceno rušenja z orodji (`ToolSet`), obvoz zaščitenega bloka (golden), ohranitev predmetov z lažnim `IItemHandler` pri prekinitvi | dokaz |
| M11.10 | Jar-in-jar preverba (D-029): `launcher-test.ps1` z dvema porabnikoma, ki nosita knjižnico (lahko z dvema testnima jarjema brez vsebine) | da/ne za jar-in-jar |

## Tečaj T5 — worker osnove

Superflat, prisilno naloženi chunki, vsak odsek ima svoje območje in skrinjo z inventarjem.

| # | Odsek | Pričakovano |
|---|---|---|
| 1 | tunel 5 blokov kamna; ponovi z leseno, kamnito, železno krampo | skozi; čas kopanja na blok ± 1 tick od vanilla formule |
| 2 | cilj 6 blokov nad tlemi, ob zidu (steber) | steber iz throwaway blokov |
| 3 | luknja širine 4 (most) | most, brez padca |
| 4 | cilj pod tlemi (3 bloki) | spust z rušenjem |
| 5 | gramoz nad tunelom | brez zadušitve, gramoz pobran |
| 6 | zaščiten steber (`IWorkPermission` = ne) na ravni poti | obvoz, 0 porušenih |
| 7 | skrinja in postelja v poti (D-031 seznam) | obvoz, 0 porušenih |
| 8 | cilj izven območja za zidom | FAILED ali pot brez rušenja izven območja |
| 9 | prazen inventar (brez throwaway) pred luknjo | pot brez postavljanja ali FAILED, ne tavanje |
| 10 | `EntityAIWander` aktiven med odsekom 1 | proces konča, AI ga ne prekine |

**Invariante (runner jih preveri pri vsakem teku, ne ločen test):** vse spremembe blokov
(prek `BlockEvent` in podpisa območja pred/po) so znotraj območja; vsota predmetov
(inventar + skrinja + `EntityItem` v območju) = pričakovana; 0 izjem v logu.

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | T5 ≥ 9/10 odsekov (en tek na dedicated strežniku) |
| A2 | 0 spremenjenih blokov izven območja in v zaščitenih (vsi odseki) |
| A3 | testni handler prekliče `BreakEvent`/`PlaceEvent` → blok nespremenjen, worker ponovno načrtuje, 0 izjem |
| A4 | ohranitev predmetov na T5 in pri treh prekinitvah (entiteta ubita sredi kopanja, `release` sredi dejanja, unload chunka) |
| A5 | T1 + T2 z navigacijskim profilom po M11: rezultat ≥ zadnji in 0 spremenjenih blokov (en tek, ker se deljena koda premikov spremeni) |
| A6 | odsek 10 in `pause/resume` sredi kopanja |
| A7 | `/npcb selftest` v dev klientu in vanilla klientu na dedicated strežniku: korak "worker tunel" OK |

## Stanje (2026-09-28, veja `m11-worker` iz `m7-next`)

| # | Stanje |
|---|---|
| M11.1 | **javap narejen** (RAZISKAVA §10): prazna povezava je potrebna (`HandsNetHandler`), identiteta rok ne sme biti UUID igralca (**D-044**, `HandsIdentityTest`), trdota rabi `onGround` in položaj rok (R-24). Sonda v igri `/npcb probe hands` — **prevedeno, čaka na zagon v dev strežniku** |
| M11.2 | **prevedeno + JUnit 150/150 v oblaku**: `api.work` (`INpcWorker`, `WorkerSpec`, `WorkArea` vključno na obeh koncih, `IWorkPermission.AREA_ONLY`, `IWorkerListener`), `NpcBaritone.worker/getWorker/release`, `WorkerRegistry` (šibko na entiteto), config `worker.enabled`, manifest 3; `ApiJarTest` prevede porabnika API 1–3. `api/` sme uvažati še `net.minecraftforge.items` (D-034) in `GameProfile` (D-044). Profil workerja je do M11.6 `default` (brez rušenja). Čaka Windows build |
| M11.3–M11.10 | niso začeti |

