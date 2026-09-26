# 04 — Stanje (živ dnevnik)

Najnovejši zapis je na vrhu. Vsaka seja doda zapis ob začetku in koncu.

---

## 2026-09-26 (3) — ročne preverbe v klientu samodejno: `/npcb selftest` 6/6

**Zakaj:** ročne preverbe (M3 A1, D-008, M6 A1–A3) so bile za uporabnika preveč korakov.
`/npcb selftest` (`09560c8`) jih izvede sam: igralec stoji v novem superflat svetu, okoli njega
se postavi arena, koraki tečejo zaporedno, izid v klepetu in CSV; igralec je zaščiten, način,
težavnost, čas in pravila se vrnejo.

**Napaka, najdena ob pripravi (`55f4245`):** `EntityAIFollowOwner`, `EntityAIAvoidEntity` in
`EntityAIFollow` si navigator shranijo v konstruktorju (javap). Po `attach` so ukazovali
staremu vanilla navigatorju, ki ga nihče ne tiktaka — ukročen volk bi stal (in se le
teleportiral), vaščan ne bi bežal pred zombijem; enako bi veljalo za CNPC taske s shranjenim
navigatorjem. `Attach` zdaj polja tipa `PathNavigate`, ki kažejo na stari navigator,
preusmeri na našega (po vrednosti, ne po imenu), ob `detach` nazaj. `AttachRewireTest`
(mutacija ujeta), 110/110 JUnit.

**Preverjeno (dev klient, 15:48, `docs/meritve/klient/selftest-20260926-154839.*`): 6/6.**

| # | Preverba | Izid |
|---|---|---|
| M3 A1 | izris poti z modom na klientu | OK — narisana po 2 tickih |
| M3 | `status`, `profile list`, `profile walk`, `goto @igralec` | OK — 4/4, husk pri igralcu po 48 tickih |
| M3 A1 | po `debug off` črte izginejo | OK — 2841 ms (klient pozabi pot po 3 s) |
| D-008 + M6 A1 | zombi brez `puppet` (vanilla AI + Baritonov navigator) najde in udari igralca za zidom z režo | OK — udarec po 90 tickih, 1 iskanje; kontrola brez Baritona po 175 |
| M6 A3 | ukročen volk 11 blokov za zidom pride peš | OK — 51 tickov, 1 iskanje, 0 teleportov |
| M6 A2 | vaščan ponoči skozi vrata v kolibo | OK — 146 tickov; kontrola 130 |

**M2 A6 preverjen (16:17–16:19, CurseForge instanca, Forge 14.23.5.2859):** v `mods` pravi
`baritone-standalone-forge-1.2.19.jar` (Mixin 0.7.11 naložen) + naš **izdani (reobf) jar**
(`launcher-test.ps1`), `/npcb selftest` **6/6**, brez `LinkageError` in brez izjem; edina
napaka v logu je Baritonova lastna (`mixins.baritone.json does not specify "minVersion"`).
Hkrati potrjena SRG imena izdanega jarja (R-14: `Attach` polja, preusmeritev taskov, izris).
Zombi z Baritonom je udaril po 216 tickih (tarčo je izbral šele pri ~140); kontrolni zombi v
45 s tarče ni izbral (vanilla naključje/vidnost, ne naš adapter). Meritve:
`docs/meritve/klient/selftest-forge-20260926-161900.*`.

**Še ročno:** M3 A2 (vanilla klient na dedicated strežnik z modom): `.\launcher-test.ps1 -Server`
— prvi poskus se ni povezal, ker strežnik ni bil zagnan (ni `m3-vanilla`, ni "Done").

**Naslednji korak:** M3 A2 (`.\launcher-test.ps1 -Server`, počakaj na "Done", vanilla 1.12.2 →
`localhost`); nato stopnja A zmogljivosti (pogoj za M7).

---

## 2026-09-26 (2) — M8 zaključen: tretji tek T3 96/100

**Preverjeno (15:08–15:16, `7946c65`, `movement.largeEntities=true` samo med tekom — v logu
`largeEntities=true`, config po teku vrnjen na `false`):** T3 **96/100**, `blocks_changed=0`,
0 izjem, 0 TIMEOUT. Velikosti uveljavljene na vseh 100 odsekih.

| size (š × v) | T1 | T2 | neuspeli odseki |
|---|---|---|---|
| 1 (0,12 × 0,36) | 10/10 | 9/10 | T2/5 lestev dol: zdrs iz stolpca lestve, padec 3,94, škoda 1 |
| 3 (0,36 × 1,08) | 10/10 | 9/10 | T2/5 enako |
| 5 (0,60 × 1,80) | 10/10 | 10/10 | — |
| 7 (0,84 × 2,52) | 10/10 | 10/10 | — |
| 10 (1,20 × 3,60) | 9/10 | 9/10 | T1/5, T2/5 (glej spodaj) |

**Merila M8:**

| # | Merilo | Izid |
|---|---|---|
| A1 | T3 ≥ 9/10 za vsako velikost | **da** — najslabše 9/10 (size 1, 3 T2; size 10 T1 in T2) |
| A2 | pričakovani neuspehi FAILED, ne tavanje | **da** — 19/19 FAILED, 0 TIMEOUT; najdaljši 254 tickov (s10 T2/5) |
| A3 | golden testi za vse kombinacije | **da** — `GoldenSizeTest` 16 velikosti (108/108 JUnit) |

**Size 10, oba neuspeha sta pričakovana FAILED, kršita pa dodatno omejitev odseka:**
- **T1/5 diagonala ob stebrih:** A* najprej vrne delno pot proti cilju (okvir 3×3 na cilju
  zadene steber), entiteta ob stebrih dobi **škodo 1** (verjetno zadušitev — glava 3,6 visoke,
  1,2 široke entitete zareže v steber ob sekanju vogala med zaporednimi Traverse), nato
  `no_path` v 29 tickih. Izvajalec za široke entitete ob ovirah ne sme sekati vogalov → M10.
- **T2/5 lestev dol:** obhod čez sosednjo progo, `MovementFall` 4 bloke v vodo proge T2/6
  (škoda 0), nato do x = 80 in `no_path` (cilj v ozki progi). Pravilno po pravilih Baritona
  (padec v vodo je varen), omejitev padca odseka pa je pisana za lestev.

**Opažanje size 1/3 (lestev dol)** je nespremenjeno od drugega teka (spodaj); kandidat za
izvajalca `MovementDownward` pri širini < 0,6.

Meritve: `docs/meritve/m8/t3-20260926-151645*.csv`.

**Odprto (ne blokira M8):** ročne preverbe v klientu (M2 A6/D-008, M3 A1/A2, M6 A1–A3),
M5 A4 (1 h stres), stopnja A zmogljivosti (≤ 2 ms p95 pri 200 mobih, pogoj za M7, D-027),
merge vej v `main`.

**Naslednji korak:** stopnja A (pogoj za M7): profil `baritone.tick()` na glavni niti pri
200 mobih (`.\t4-run.ps1 -Mobs 200`) — kaj od izvajalca poti, `PathingControlManager` in
`LookBehavior` je drago; ali M9 (ladje, neobvezno), po odločitvi uporabnika.

---

## 2026-09-26 — M8: prva dva teka T3; velikost in stikalo largeEntities

**Preverjeno (13:29–13:37):** `dev.ps1 build --offline` zelen, T3 v 7,5 min: **91/100**,
`blocks_changed=0`, 0 izjem, noben TIMEOUT. A1 po velikostih: 1, 3, 5 → 10/10 + 10/10;
7 → 9 + 9; 10 → 8 + 5. Vseh 9 "neuspehov" je pričakovanih neuspehov, ki so **dosegli cilj**
(s7 T1/6, T2/1; s10 T1/5, T1/6, T2/1, T2/3, T2/4, T2/5, T2/9).

**Vzrok:** CSV ima za vse velikosti `width,height = 0.60,1.95` — husk velikosti nikoli ni
spremenil. `EntityZombie.setSize` (javap) si po prvem klicu (konstruktor) velikost samo
zapomni (`zombieWidth/Height`), uveljavi jo šele `multiplySize`. Tveganje iz (16) se je
uresničilo. Tek zato o velikostih ne pove ničesar; potrdi pa, da je standardna veja na T1+T2
petkrat zapored pravilna (vseh 10 FAILED pričakovanih izidov za 1×2 je FAILED).

**Popravek (`1f4417b`):** `CourseRunner.resize` za zombije pokliče še `multiplySize(1)`
(SRG `func_146069_a`, sicer edina `(float)` metoda razreda) in preveri uveljavljeno velikost
— če se ne ujema, tečaj pade z izjemo namesto tihega teka. `CourseResizeTest` (husk prek
`Unsafe`, brez sveta) je najprej padel (0,6 namesto 0,12), zdaj 107/107. `t3-run.ps1` med
tekom izklopi QuickEdit (`e40ad21`).

Meritve prvega teka: `docs/meritve/m8/t3-20260926-133727*.csv` (ostanejo kot dokaz napake).

**Drugi tek (14:05–14:10, po `1f4417b`): 71/100.** Velikosti so zdaj uveljavljene (CSV:
0,12×0,36 … 1,20×3,60, vseh 20 odsekov na velikost). A1:

| size | T1 | T2 | |
|---|---|---|---|
| 1 | 10/10 | 9/10 | T2/5 lestev dol: padec 3,94, škoda 1 |
| 3 | 10/10 | 9/10 | T2/5 enako |
| 5 | 10/10 | 10/10 | |
| 7 | 2/10 | 2/10 | samo pričakovani FAILED |
| 10 | 3/10 | 6/10 | samo pričakovani FAILED |

**Vzrok za 7 in 10:** tečaj je tekel s `movement.largeEntities=false` (privzeto, D-019), zato
`BaritonePathNavigate.onUpdateNavigation` za entiteto > 1×2 vsak tick pokliče `dropBaritone`
in prekliče cilj tečaja — v sledi ni niti enega iskanja, odsek je FAILED v 10 tickih. Tek zato
o D-028 za velike entitete še ne pove ničesar. **Popravek (`7946c65`):** `t3-run.ps1` stikalo
med tekom vklopi (in ga vrne), preveri `largeEntities=true` v logu; `/npcb course run` zavrne
tečaj z velikostjo, ki je navigator ne bi vodil (`CourseRunner.sizeNotLed`). 108/108 JUnit.

**Opažanje (ne krši A1):** size 1 in 3 na T2/5 med `MovementDownward` po lestvi zdrsneta iz
stolpca lestve (size 1: x = 70,06, lestev v stolpcu 69) → ponovno načrtovanje, `MovementFall`
s 3,3 bloka. Ozka entiteta ob cilju sredine bloka preleti rob; size 5 (0,6) ostane v stolpcu.
Kandidat za popravek v izvajalcu `MovementDownward` za širino < 0,6 (M8 ali M10).

Meritve drugega teka: `docs/meritve/m8/t3-20260926-141047*.csv`.

**Naslednji korak:** tretji tek T3 → zapis (2) zgoraj.

---

## 2026-09-25 (16) — M8: velikosti entitet (veja `m8-velikosti`)

**Namen seje:** M8 do točke, ko ostaneta build in tek T3 na Windowsu.

**Narejeno (oblak, 106/106 JUnit; prej 87)**

| Commit | Kaj |
|---|---|
| `2ef4af5` | M8.1–M8.7: `EntitySize`, velikost v `CalculationContext` in premikih; splošna (size-aware) veja za Traverse, Ascend, Descend/Fall, Diagonal, Pillar, Downward, Parkour; bližnjice sprinta v `PathExecutor` samo za 1×2; `SizeAwareEquivalenceTest` |
| `57bdf5a` | M8.8: `GoldenSizeTest` — G1–G14 za 16 velikosti (0,3–2,0 × 0,9–3,6) |
| `227c3e0` | `cloud-compile.sh`: `NPCB_JAVA_OPTS` |
| `4af1781` | M8.9: tečaj T3 (`CourseT3`, `/npcb course t3`, velikost pred odsekom, prisiljeni chunki, CSV `npc_size`), `CourseT3PathTest`, `t3-run.ps1` |
| `ce280ce` | M8.10: navigator s stikalom `movement.largeEntities` (privzeto false = D-019) |

**Odločitev D-028** (zamenja D-019): okvir blokov na sredini bloka nog (`⌈višina⌉` blokov,
`⌈(širina−1)/2⌉` stolpcev na stran); standardna velikost ostane na upstream veji; široke
entitete brez diagonal, lestev in parkourja; spust samo za širino < 2 (stolpci pred ciljem,
ker kolizija entiteto porine naprej — tega Automatone ni rešil); meje navigatorja 3 × 4.

**Kako je preverjeno (headless):**
- enakost vej za 0,6 × 1,8: 6 naključnih terenov × 2 profila (NPC in z rušenjem/parkourjem)
  × vsi premiki iz vsakega položaja; mutacije (prednja ploskev, strop, vrh spusta, vogal
  diagonale) test ujame (5 od 6; zastavica padajočih blokov na vrhu diagonale ne — redek primer);
- `GoldenSizeTest` je najprej padel na G9 (široka entiteta, zagnana v hodniku 1×2, je "ušla"
  skozi steno ob sebi) → ciljni stolpec se preveri tudi pri Traverse/Ascend;
- T3 headless: 100 odsekov, 9 pričakovanih neuspehov z razlogom (tabela v README M8), size 1
  sme skozi režo 1,5; size 10 obide zidove čez sosednjo progo.

**Prevedeno, čaka na zagon:** `.\mod\gradlew.bat --stop; .\dev.ps1 build --offline; .\t3-run.ps1`
(M8 A1–A2; do ~1 h). Regresija standardne velikosti: `.\t1-run.ps1`, `.\t2-run.ps1`
(pričakovano nespremenjeno — upstream veja).

**Tveganja za tek v igri:** `Entity.setSize` prek refleksije na husku (zombi si velikost
zapomni); široka entiteta ob spustu visi na robu, dokler je kolizija ne porine naprej;
izvajalec poti cilja sredino bloka — pri širini 1,2 v prehodu širine 3 brez rezerve.

**Naslednji korak:** `.\mod\gradlew.bat --stop; .\dev.ps1 build --offline; .\t3-run.ps1`.

---

## 2026-09-25 (15) — M6: peti aitest; A1 na ročno preverbo, začetek M8

**Preverjeno (20:26):** T1 10/10 (`docs/meritve/m2/t1-20260925-202615.csv`, hitrosti `speed-20260925-202719.csv`).
`aitest attack`: tarča nastavljena, vanilla `getPathToEntityLiving` vrne pot dolžine 27 — a
`EntityAIZombieAttack` se **ne začne ne pri husku z Baritonom ne pri kontroli brez njega**
(`docs/meritve/m6/ai-attack-20260925-202714.csv`: `TIMEOUT` pri obeh). Ker kontrola brez Baritona
ravna enako, je vzrok okolje brezglavega strežnika, ne adapter.

**Odločitev (uporabnik, "nadaljuj"):** M6 A1–A3 (napad, vaščan domov, volk) se preverijo ročno v
klientu skupaj z M2 A6 in M3 A1/A2; `aitest` ostane kot diagnostika. M7 čaka na stopnjo A (D-027),
zato naprej **M8 (velikosti entitet)** na veji `m8-velikosti`.

---

## 2026-09-25 (14) — M6: tretji aitest, javni API in apiJar

**Preverjeno (19:52, po popravku `extractNatives` v `build.gradle`):** build in T1 10/10. `aitest`: arena pri `-60 4 40` s prisilno naloženimi chunki, **vsi štirje spawnani** — pa **nobeden od huskov (tudi kontrola brez Baritona) ne izbere vaščana** z `EntityAINearestAttackableTarget` v 30 s. Koda 1.12.2 (javap) pogoja, ki bi to preprečil, ne kaže; ker velja tudi brez Baritona, ni naša napaka in ni predmet M6. **Ukrep:** test tarčo nastavi neposredno (`setAttackTarget`), `EntityAIZombieAttack` ostane vanilla.

Iz sledi: husk z Baritonom je dvakrat dobil cilj od vanilla tavanja (`-65,2,38` pod tlemi, `-70,4,31` za steno arene) → `FAILED(no_path)` → `noPath()=true` → task odneha. Pravilno po D-018; vanilla bi naredil nekaj korakov proti nedosegljivi točki. Zapisano kot razlika v README M6.

**Narejeno (oblak, 87/87 JUnit):**

| Commit | Kaj |
|---|---|
| `1996b7f` | `build.gradle`: `extractNatives` izklopljen, ko teče samo `runServer` (padal je na odprtem jar-ju v Gradle cache) |
| `4ec54de` | M6.7 javni API 1 (`NpcBaritone`, `INpcNavigator`, `NavState`, `NavListener`, `ApiProvider`), A5 `ApiJarTest`, aitest s tarčo |
| `f127303` | M6.8 `apiJar` |

**Naslednji korak:** `.\mod\gradlew.bat --stop; .\dev.ps1 build --offline; .\t1-run.ps1` → M6 A1/A4 (husk z vanilla napadom prek Baritona), v `mod\build\libs` mora biti tudi `*-api.jar`.

---

## 2026-09-25 (13) — M6: drugi aitest — okolje in "hoja na mestu"

**Preverjeno (18:53):** T1 10/10. `aitest attack` s kontrolo: **ne husk z Baritonom ne kontrolni husk brez Baritona nista nikoli dobila tarče** (`target=-` 30 s) → problem je okolje testa. Arena pri z=150 je izven spawn območja (128 blokov), ki ostane naloženo na strežniku brez igralca; vaščan se je morda razložil ali sploh ni bil spawnan (`spawnEntity` tega ne javi).

**Druga ugotovitev (prava napaka navigatorja):** husk z Baritonom je 9 s izvajal `EntityAIWanderAvoidWater` z `noPath()=false`, premaknil pa se je za 0,1 bloka; kontrola je v istem času normalno tavala.

**Ukrep:** arena pri `-60 4 40` (znotraj spawn območja) + Forge vozovnice; sporočilo ob začetku pove, ali sta vaščan in husk spawnana; diagnostika vsako sekundo kaže stanje navigacije, cilj, pot, `inControl`, move helper; sled husk-a v `ai-attack-<čas>-trace.csv`.

**Naslednji korak:** `.\dev.ps1 build --offline; .\t1-run.ps1`.

---

## 2026-09-25 (12) — M6: prvi aitest spodletel (AI napada ni začel)

**Preverjeno (17:42):** T1 10/10, `blocks_changed=0`, hitrosti nespremenjene — adapter ne pokvari tečaja. **`aitest attack`: TIMEOUT v 30 s, 0 klicev navigatorja** — husk z vanilla AI sploh ni začel napada (brez `setPath`/`tryMoveToEntityLiving`), zato navigator ni dobil ukaza. Vzrok iz loga ni razviden (tarča? `EntityAIAttackMelee.shouldExecute`? okolje brez igralca?).

**Ukrep:** test ima zdaj kontrolni husk **brez** Baritona v sosednji areni (isti teren, isti vaščan z `NoAI`) in vsako sekundo `NPCB-AITEST-DBG` (tarča, aktivni taski, položaj, tla, `noPath`, klici navigatorja). Če tudi kontrola ne napade, je problem v okolju testa, ne v adapterju.

**Naslednji korak:** `.\dev.ps1 build --offline; .\t1-run.ps1`.

---

## 2026-09-25 (11) — M6: PathNavigate adapter (veja `m6-navigator`)

**Narejeno (oblak, 85/85 JUnit):** navigator po D-018/D-019 (debounce, pavza po `clearPath`, zavrnitev po neuspehu, sledenje, `setPath`/`getPath`/`setSpeed`, vanilla za prevelike entitete) in samodejni test vanilla AI `/npcb aitest attack` (A1, A4) v `t1-run.ps1`. Popravljen vrstni red stanj v `NavStatus`: takoj po novem cilju je SEARCHING, ne stari FAILED/ARRIVED (sicer bi `noPath()` vanilla task takoj ustavil).

**Prevedeno, čaka na zagon:** `.\dev.ps1 build --offline; .\t1-run.ps1` (T1 regresija + M6 A1/A4).

**Naslednji korak:** M6.7/M6.8 javni API (`NpcBaritone`, `INpcNavigator`, `NavState`, povratni klici) in `apiJar`.

---

## 2026-09-25 (10) — M5 zaključen za stopnjo B (razen 1 h, A4)

**Preverjeno (17:08–17:15, po utišanju logov):** `t4-run.ps1 -Mobs 200`, 3 ponovitve × 120 s, svež svet vsakič; log: 0 "Pathing complete", 0 `STDOUT`, 0 izjem.

| 200 mobov | mediana [razpon] |
|---|---|
| glavna nit µs/tick p95 | **3265** [3154, 4084] |
| glavna nit p50 / p99 | 2472 [2351, 3129] / 5083 [4389, 6146] |
| MSPT p95 | 4,65 [4,51, 6,16] ms |
| iskanj/s, µs p50 / p95 | 23,5, 562 / 13 456 |
| čakanje v vrsti µs p95 | 226 [27, 254] |
| posnetek µs p95 | 29 [27, 36] |
| doseženih ciljev | 95 % (2304–2358 / 2430–2476), zavrnjenih 0 |

**Merila M5 (D-027):**

| # | Merilo | Izid |
|---|---|---|
| A1 | stopnja B: p95 ≤ 5 ms pri 200 | **da** — 3,27 ms (tudi najslabša ponovitev 4,08) |
| A1+ | stopnja A: p95 ≤ 2 ms pri 200 | **ne** — odprto, pogoj za M7; pri 50 mobih 1,36 ms |
| A2 | posnetek p95 < 100 µs | **da** — 29 µs (200), 72–85 µs (50) |
| A3 | niti izven bazena | **0** |
| A4 | izjeme v 1 h stresa z rušenjem | **čaka** (`.\t4-run.ps1 -Mobs 200 -Repeats 1 -Seconds 3600`) |
| A5 | tabela 50/200 | `docs/meritve/m5/t4-summary-20260925-171507.csv` + CSV posameznih tekov |

**Za stopnjo A (pred M7):** ~12 µs na NPC na tick na glavni niti ostane; kandidati po vrsti: profil (kaj v `baritone.tick()` je drago — izvajalec poti, `PathingControlManager`, `LookBehavior`), redkejši tick NPC-jev daleč od igralcev, deljenje iskanj (M5.2 — glavne niti skoraj ne obremenjuje, iskanja so na nitih). Vrata V3 za M6 so odprta.

**Naslednji korak:** 1 h tek (A4) lahko teče v ozadju; razvoj gre na M6 (`PathNavigate` adapter in javni API).

---

## 2026-09-25 (9) — M5: T4 3×50 in 1×200; prihod potrjen; tek se je "zataknil" v konzoli

**Preverjeno (16:16–16:27):** T1 10/10 z `nav_state`: **9/9 doseženih ARRIVED, odsek 10 FAILED** — popravka iz (7) in (8) delujeta. T4 (120 s meritve, rušenje vsakih 5 s):

| | 50 mobov (3 ponovitve) | 200 mobov (1 ponovitev) |
|---|---|---|
| glavna nit µs/tick p95 — mediana [razpon] | **1363** [1341, 1456] | **3810** |
| glavna nit p50 / p99 | 934–978 / 1942–2052 | 3073 / 6666 |
| MSPT p95 | 2,12–2,39 ms | 5,81 ms |
| doseženih ciljev | 573–589 / 608–623 (94–95 %) | 2311 / 2430 (95 %) |
| iskanj/s, µs p95 | 6,1–6,2, 14 457–17 732 | 23,4, 13 367 |
| posnetek µs p95 (chunkov) | 72–85 (81) | 30 (83) |
| niti ustvarjenih | 2 | 2 |

**Merila:** A1 stopnja B (≤ 5 ms pri 200) **da** (3,8 ms, ena ponovitev); A1+ stopnja A (≤ 2 ms) **ne** — odprto do M7 (D-027); A2 (posnetek < 100 µs) da; A3 (0 niti izven bazena) da; izjem 0. Manjkata še 2 ponovitvi 200 in 1 h (A4).

**"Zataknjen" tek:** 200/1 je meritev končal ob 16:27, `npcb perf` in `stop` pa sta prišla v strežnik šele ob 17:03 — strežnik je vmes normalno tekel (45 851 tickov). Vzrok je Windows konzola (QuickEdit: klik v okno zamrzne PowerShell skript). `t4-run.ps1` zdaj QuickEdit med tekom izklopi.

**Popravek iz loga:** pri 200 NPC-jih 2422× "Pathing complete" na glavni niti in 1861 vrstic A* statistike prek `System.out` v 2 min → `notificationOnPathComplete=false` v `NpcProfile`, izpisi prek `logDebug`. Pričakovan manjši padec µs glavne niti.

**Naslednji korak:** `.\dev.ps1 build --offline; .\t4-run.ps1 -Mobs 200` (3 ponovitve po popravku, ~9 min); nato 1 h (A4) in odločitev, ali gremo v M6 (stopnja B) in stopnjo A pustimo za pred M7.

---

## 2026-09-25 (8) — T1 po popravku prihoda; tečaj čaka na končno stanje

**Preverjeno (16:11):** T1 10/10, `blocks_changed=0`, hoja 4,317, sprint 5,612 m/s. Sled pa **še vedno brez ARRIVED**: `CourseRunner` je razglasil REACHED, ko so bile noge v ciljnem bloku, in odsek v istem ticku preklical — proces cilja prihoda ni nikoli videl. Preverba "ARRIVED v sledi" v T1 zato ni mogla potrditi popravka iz (7).

**Popravek:** `CourseRunner` po izidu odseka počaka največ 20 tickov, da `NavStatus` ni več MOVING/SEARCHING, in zapiše `nav_state` v CSV. `t1-run.ps1` in `t2-run.ps1` zahtevata ARRIVED za vse dosežene in FAILED za neuspele odseke. Vsi trije skripti ob zaklenjenem logu (prejšnji strežnik še teče) povedo, kaj narediti.

Ob tem: `extractNatives` je padel, ker so ostali trije Java procesi prejšnjega teka; po `Stop-Process` je šlo.

**Naslednji korak:** `.\dev.ps1 build --offline; .\t1-run.ps1; .\t4-run.ps1`.

---

## 2026-09-25 (7) — M5: prvi (hitri) T4 in popravek prihoda

**Preverjeno (Windows, `t4-run.ps1 -Repeats 1 -Seconds 60`, 12:01–12:04):** oba teka brez izjem in brez "Can't keep up"; 2 niti, 0 zavrnjenih iskanj.

| | 50 mobov | 200 mobov |
|---|---|---|
| glavna nit µs/tick p50 / p95 / p99 | 962 / **1645** / 3586 | 3201 / **3590** / 8546 |
| MSPT strežnika p50 / p95 | 1,47 / 2,72 ms | 4,55 / 5,32 ms |
| iskanj/s, µs p50 / p95 | 6,4, 665 / 67 387 | 24,0, 580 / 67 093 |
| čakanje v vrsti µs p95 | 60 | 32 |
| posnetek µs p50 / p95 (chunkov) | 48 / 109 (306) | 20 / 43 (306) |

Ena ponovitev, zato samo smer: **stopnja B (≤ 5 ms pri 200) izpolnjena z rezervo, stopnja A (≤ 2 ms) ne** — ~16 µs na NPC na tick; A2 (posnetek < 100 µs) na meji pri 50 (manj vzorcev, JIT). Iskanja p95 ≈ 67 ms so iskanja do nedosegljivih ciljev (cilji v stebrih/zidovih) — tečejo na iskalnih nitih, glavne niti ne obremenjujejo.

**Napaka, ki jo je T4 razkril: `reached = 0`.** `CustomGoalProcess` opazi prihod (noge v cilju) pred izvajalcem poti, izgubi nadzor in prekliče pot — namesto `AT_GOAL` pride `CANCELED`, ki je v `NavStatus` pobrisal stanje (enako `CANCELED` po `CALC_FAILED`). Zato tudi sledi T1/T2 nikoli niso pokazale ARRIVED. Popravek `NavStatus.Flags`: zapomni si zadnji cilj, ob `CANCELED` preveri noge, ponastavi samo ob `CALC_STARTED`; + števec neuspelih iskanj. `t1-run.ps1` zdaj zahteva ARRIVED v sledi, `t4-run.ps1` vsaj en dosežen cilj na tek. JUnit 77/77.

**Opažanje:** šumniki v logu dedicated strežnika so pokvarjeni (kodna stran Windows konzole) — samo kozmetika, CSV so UTF-8.

**Naslednji korak:** `.\dev.ps1 build --offline; .\t1-run.ps1` (ARRIVED v sledi), nato polni `.\t4-run.ps1` (3 × 50/200, 120 s) za A1/A2 z mediano in razponom.

---

## 2026-09-25 (6) — M5: zmogljivost (veja `m5-zmogljivost` iz `m4-interakcije`)

**Odločitev:** D-027 — meja A1 v dveh stopnjah: 5 ms/tick p95 pri 200 mobih zdaj (drug mod z manj entitetami), 2 ms ostane odprta kot pogoj za CustomNPC (M7). Merge v `main` počaka na ročne preverbe v klientu.

**Narejeno (oblak, 73/73 JUnit)**

| Commit | Kaj |
|---|---|
| `b37c044` | jedro: prednostni iskalni bazen z mejo in življenjskim ciklom, `SearchStats` (µs iskanja, vrsta, posnetek) |
| `b4c14d3` | `PerfMeter` (µs knjižnice na tick, MSPT), `/npcb perf`, `StressRunner` (`/npcb stress`), `t4-run.ps1` |

Ni narejeno namenoma: M5.2 deljenje iskanj in M5.3 umerjanje časovnih omejitev — najprej številke iz T4.

**Prevedeno, čaka na zagon:** `t4-run.ps1` (najprej `-Repeats 1 -Seconds 60`), nato polni tek in 1 h (A4).

**Naslednji korak:** `.\dev.ps1 build --offline; .\t4-run.ps1 -Repeats 1 -Seconds 60`; po številkah odločitev o M5.2/M5.3.

---

## 2026-09-25 (5) — M4 zaključen v prvem teku

**Preverjeno (Windows, veja `m4-interakcije`, `cd58424`):** `t2-run.ps1` in `t1-run.ps1` zelena.

| # | Merilo | Rezultat |
|---|---|---|
| A1 | T2 odsekov OK ≥ 9/10, železna vrata FAILED | **10/10**, odsek 2 FAILED po 35 tickih (3 iskanja) |
| A2 | porušenih/postavljenih blokov | **0** v T2 in 0 v T1 (`blocks_changed`) |
| A3 | škoda T1+T2 | **0,0** (tudi padec 10 v vodo, lava, kaktusi) |
| A4 | vrata za NPC-jem zaprta | **2/2** (lesena in ograjna) |

T1 regresija: 10/10, hoja 4,317, sprint 5,612 m/s, 0 debug paketov. Meritve: `docs/meritve/m4/t2-20260925-114638*.csv`, `docs/meritve/m2/*-1147*`, `docs/meritve/m3/t1-20260925-114742-trace.csv`.

Iz sledi T2: lestev gor = `Pillar`, dol = `Downward`, voda 1 in 3 = `Descend/Ascend` po površini (mob ni potonil, 0 škode), padec = `Fall`. Kisik (M4.5) zato ni bil potreben — Baritone ne vodi pod vodo.

**Opažanja (ne kršijo meril, za kasneje):**
- T2/9 kaktusi: 5 iskanj in 2 naložena chunka — NPC je zašel s poti ob reži; škode ni bilo.
- yaw tresenje 10–15 na odsekih z lestvijo dol in padcem (A5 velja samo za ravno progo).
- prvo iskanje na svežem strežniku znova ~63 ms (JIT), nato ≤ 2,3 ms.

**Ni narejeno (namenoma):** M4.6 ogenj in magma nimata testa (lava in kaktus ga imata); M4.7 izogibanje mobom ni začeto (privzeto izklopljeno, ni v merilih) → M10.

**Naslednji korak:** merge `m4-interakcije` → `main` po ročnih preverbah v klientu (M2 A6/D-008, M3 A1/A2) ali brez njih po odločitvi uporabnika; razvoj gre na M5 (zmogljivost).

---

## 2026-09-25 (4) — M4: interakcije (veja `m4-interakcije` iz `m3-vidnost`)

**Namen seje:** M4 do točke, ko ostane tek T2 na Windowsu; voda in lestve najprej izmeriti, šele nato popravljati.

**Narejeno (oblak, 68/68 JUnit)**

| Commit | Kaj |
|---|---|
| mehansko | `Course` (Segment, Expect, Sink, Builder) iz `CourseT1` (D-026) |
| jedro | vrata na cilju in izhodišču premika (Automatone `3216de48`, `b1899f30`); **upstream 1.12.2 je vrata pred sabo klical z zamenjanima argumentoma in jih nikoli ni odprl** |
| M4.1 | `EntityInteractions`: `CLICK_RIGHT` → lesena/ograjna vrata ob premiku, zapiranje za NPC-jem |
| M4.8/M4.9 | `CourseT2` (10 zaprtih prog), splošen `CourseRunner`, stolpca `damage` in `openables_closed`, `blocks_changed`; `/npcb course <t1\|t2>`; `t2-run.ps1`; `t1-run.ps1` preveri še `blocks_changed` |

Headless A* na T2: vseh 10 izidov pravilnih (železna vrata = ni poti), poti gredo skozi vrata in ograjna vrata, po lestvi gor (`Pillar`) in dol (`Downward`), velik padec samo v vodo, lava in kaktus se ne dotakneta. Voda (T2/6, T2/7) gre po površini z `Descend/Ascend` — ali mob, ki v vodi tone, to zmore, bo pokazal tek.

**Prevedeno, čaka na zagon:** `t2-run.ps1` (M4 A1–A4) in `t1-run.ps1` (regresija + `blocks_changed`).

**Naslednji korak:** `git checkout m4-interakcije; .\dev.ps1 build --offline; .\t2-run.ps1; .\t1-run.ps1`. Padli odseki T2 določijo delo na vodi/lestvah (M4.3–M4.5).

---

## 2026-09-25 (3) — M3: prvi dedicated tek in popravka jedra iz sledi

**Preverjeno (Windows, veja `m3-vidnost`, `f103bc9..afa6636`):** `dev.ps1 build --offline` zelen, `t1-run.ps1` exit 0. T1 **10/10**, hoja **4,317 m/s**, sprint **5,612 m/s**, yaw tresenje 0, chunki med T1 0 — enako kot trije teki M2 (`docs/meritve/m2/*-110158*`, `speed-*-110229`). M3 A3: sled `docs/meritve/m3/t1-20260925-110158-trace.csv`, 17 stolpcev, 695 vrstic, vseh 10 odsekov. M3 A4: `debug paketov: 0` brez prejemnikov.

**Sled je takoj pokazala tri stvari** (analiza iz CSV):

| # | Ugotovitev | Dokaz v sledi | Ukrep |
|---|---|---|---|
| 1 | `CANCELED` vsak tick, ko NPC miruje (podedovano: `PathingControlManager.preTick` brez procesa vsak tick prekliče segment in upstream vedno odda dogodek) | 8–10 zaporednih `CANCELED` na začetku vsakega odseka | `PathingBehavior.segmentCancel`: dogodek samo, če je bilo kaj preklicano, ali ob izrecnem `cancelEverything` |
| 1a | posledica #1: `NavStatus` ob `CANCELED` pobriše ARRIVED/FAILED → sled ni nikoli pokazala FAILED, `fail_reason` vedno prazen | stanja v sledi samo IDLE/SEARCHING/MOVING | odpravljeno s #1 |
| 2 | nedosegljiv cilj: po `NEXT_CALC_FAILED` upstream poskusi načrtovanje naprej **vsak tick** z istega začetka proti istemu cilju | T1/10: 48 iskanj v 49 tickih, ~1,7 ms vsako | začetek+cilj neuspelega načrtovanja se zapomni; ponovi se šele ob spremembi (ob koncu segmenta tako ali tako teče polno iskanje) — pomembno za M5 |
| 3 | prvo iskanje v svežem strežniku 61 ms, nato 0,5–4 ms | `search_us` T1/1 | samo zapisano (JIT/nalaganje razredov); M5 meri po ogrevanju |

Poleg tega `PathTrace` posluša s prioriteto HIGH, da vzorči pred `CourseRunner` (sicer zadnji tick odseka, npr. FAILED, manjka). `t1-run.ps1` preveri še: stanje FAILED in razlog `no_path` v sledi, ≤ 5 iskanj na T1/10, ≤ 20 samostojnih `CANCELED`.

**Preverjeno (ponovni tek 11:11, `t1-20260925-111153*`):** T1 10/10, hoja 4,317, sprint 5,612 m/s; iskanj na T1/10 **3** (prej 48), samostojnih `CANCELED` **1** (prej ~90), sled vsebuje FAILED z `no_path` (tick 837), "No path found" v logu 2× (prej 47×), 0 debug paketov. Popravka #1 in #2 sta potrjena.

**Naslednji korak:** M3 ostane odprt samo za ročne preverbe v klientu (A1 izris, A2 vanilla klient) in M2 A6/D-008; razvoj gre naprej na M4 (veja `m4-interakcije` iz `m3-vidnost`).

---

## 2026-09-25 (2) — M3: vidnost (veja `m3-vidnost`, iz `m2-noge`)

**Namen seje:** M2 čaka samo ročni preverbi v klientu (A6, D-008), zato M3 do točke, ko ostanejo build na Windowsu in preverbe v igri.

**Narejeno (oblak, 61/61 JUnit; prej 43)**

| # | Kaj |
|---|---|
| jedro | `PathingBehavior`: `lastSearchMicros()`, `lastSearchResult()`, `searchesStarted()` (edina sprememba v `core`) |
| M3.1 | `/npcb` razširjen: `attach … [profil]`, `goto` na entiteto, `status [e]`, `profile`, `debug`, `trace`; poimenovani profili v configu (`profile.named`, D-016) |
| M3.2 | `DebugSync` + `net/PathSyncMessage`: kanal `npcbaritone`, prejemniki = OP 2 + mod na klientu + `debug on`/`syncPathsToOps`, 128 blokov, 4 Hz |
| M3.3 | `client/`: `ClientProxy`, `ClientPaths`, `PathRenderer` (`@SidedProxy`) |
| M3.4 | `NavStatus` (stanja in razlogi iz ARHITEKTURE §5), `PathTrace` (17 stolpcev), T1 zapiše `t1-<čas>-trace.csv` |
| testi | `M3VisibilityTest` (16), `ClientPathsTest` (1), lint "nihče izven `client/` ne uvaža `client`"; mutacijske preverbe: paket, keepalive, lint, profil — vse štiri ujete |
| skript | `t1-run.ps1`: filter izidov izključi `*-trace.csv` (sicer bi pobral sled), preverbi M3 A3 in A4, sled v `docs/meritve/m3/` |

Odločitve pri izvedbi (brez spremembe D-xxx):
- **Prejemnik ima mod** se ugotovi iz FML seznama modov (`NetworkDispatcher.getModList()`), ne s "hello" paketom — vanilla klient tako nikoli ne dobi paketa (D-024).
- **Sled je v pomnilniku** (največ 500 000 vrstic, presežek se šteje) in se zapiše ob `dump` ali koncu tečaja; `course t1 run` zamenja ročno začeto sled.
- **Stanje `NavStatus`** je predhodnik javnega `NavState` (M6); M6 ga premakne v `api`.
- **Profil** se ob menjavi uporabi s `softCancelIfSafe` — cilj ostane, pot se izračuna znova.

**Prevedeno, čaka na zagon (Windows):** `.\dev.ps1 build --offline`, `.\t1-run.ps1` (M2 A1–A5 + M3 A3, A4). Izris v klientu (A1) in vanilla klient (A2) ročno po `milestones/M3-vidnost/README.md`.

**Še odprto iz M2:** A6/M2.11 in D-008 v klientu (nespremenjeno).

**Naslednji korak:** uporabnik: `git checkout m3-vidnost; .\dev.ps1 build --offline; .\t1-run.ps1`, nato v klientu preverbe 2–4 iz M3 README (in obe M2 preverbi, ker je klient že odprt).

---

## 2026-09-25 — M2: Windows build in dedicated tečaj

**Namen seje:** na veji `m2-noge` pognati `dev.ps1 build --offline` in `t1-run.ps1`, pregledati merila A1–A5/A7 ter popraviti konkretne napake.

**Preverjeno:** `dev.ps1 build --offline` (prevod, JUnit, reobf) zelen. `t1-run.ps1` trikrat zapored konča z exit 0 na svežem dedicated svetu; CSV so v `docs/meritve/m2/` (trije `t1-*.csv` in trije `speed-*.csv`). Mediana in razpon vseh treh tekov: T1 **10/10 [10, 10]** pričakovanih izidov (odsek 10 pravilno `FAILED`), hoja **4,317 [4,317, 4,317] m/s**, sprint **5,612 [5,612, 5,612] m/s**, novi chunki med T1 **0 [0, 0]**, yaw tresenje na merilni progi **0 [0, 0]**. Dedicated zagon, pripenjanje in premikanje zato izpolnijo A1–A5 in A7.

**Popravki ob merjenju:** skript pred `/summon` postavi T1, ker strežnik brez igralca sprva nima naloženega chunka; med tečajem in speedtestom testna entiteta dobi `CanUpdate`; speedtest si pred začetkom pripravi ravno progo in jo med meritvijo zadrži s Forge chunk vozovnico (ob koncu sprosti). `BaritoneMoveHelper` uporabi odločitev `PathExecutor.isSprinting()`, ker izvajalec poti vhod `SPRINT` že porabi. CSV ime odseka z vejico je pravilno citirano.

**Še odprto:** A6/M2.11 v klientu s pravim Baritonom 1.2.19 ter D-008 preverba, da zombi brez `puppet` še napade. Uradni `baritone-standalone-forge-1.2.19.jar` (SHA-1 se ujema z `checksums.txt`) se v ForgeGradle `runClient` ustavi pri `MixinStateImplementation: Shadow field b was not located` — ta obfuskirani jar ni združljiv z razvojnimi MCP mappingi. Jar je zato shranjen v `tools/cache/`, ne v `mod/run/mods`. `runClient` brez tega jarja naloži integrirani strežnik in igralca, a se je zaprl pred ročno preverbo napada. A6 zahteva običajen Forge klient z izdanima jarjema; D-008 ostaja ročna preverba. M2 se zaključi po teh preverbah.

**Naslednji korak:** v klientu opravi M2.11/A6 in D-008 po navodilih v `milestones/M2-noge/README.md`.

---

## 2026-09-24 (6) — M2: noge (veja `m2-noge`)

**Narejeno (oblak, 43/43 JUnit)**

| # | Commit | Kaj |
|---|---|---|
| M2.4, M2.5 | `dcad71a` | obrat telesa ≤ `npcMaxTurnDegrees`/tick (yaw + renderYawOffset), POST prejšnjega ticka v `tick()` |
| M2.2, M2.3, M2.5–M2.8, M2.10 | `041be87` | `BaritoneMoveHelper` (vrstni red D-010, način "kot igralec"), `BaritoneJumpHelper`, minimalni `BaritonePathNavigate`, `Attach` (refleksija, puppet), `/npcb`, `Telemetry` (ChunkEvent.Load, speedtest) |
| M2.9 | `e8f7518` | tečaj T1 v Javi (`/npcb course t1 build|run`), CSV, headless preverba geometrije (`CourseT1PathTest`), `t1-run.ps1` |

Odločitve pri izvedbi (brez spremembe D-xxx):
- **M2.1 preskočen:** `InputOverrideHandler` je že samo stanje vhodov (M1); preimenovanje v `InputState` ne prinese ničesar, dokler ni druge implementacije.
- **T1 ni `.mcfunction`**, ampak Java (`CourseT1`): ista geometrija se tako preveri headless z A* pred zagonom v igri.
- **Odsek 7** ("reža pod ploščo 1,5"): odprtina z zgornjo polovično ploščo (1,5 bloka prostora) je za entiteto 1,95 neprehodna; uspeh = obhod skozi režo 1×2 brez vstopa v stolpec odprtine.
- **Brez igralca:** vanilla `WorldServer` po 300 tickih brez igralcev ustavi entitete; tečaj in speedtest med tekom kličeta `resetUpdateEntityTick()` (samo takrat).

**Čaka na zagon (Windows):** `.\dev.ps1 build --offline`, `.\t1-run.ps1` (A1–A5, A7 na dedicated), ročno v klientu M2.11 (soobstoj s pravim Baritonom, A6) in D-008 preverba (zombi brez `puppet` še vedno napade).

**Naslednji korak:** uporabnik: `git checkout m2-noge; .\dev.ps1 build --offline; .\t1-run.ps1`.

---

## 2026-09-24 (5) — M1 zaključen

Uporabnik je na Windowsu na veji `m1-jedro` pognal build, JUnit in server smoke:

| # | Merilo | Dokaz |
|---|---|---|
| A1 | prevod oblak + Windows | `npcbaritone-0.0.1-m0.jar` z 163 razredi `core` (reobf OK) |
| A2 | G1–G12 zeleni | Gradle JUnit: GoldenPathTest 13/13 (vsak < 0,1 s); skupaj 38/38 |
| A3 | lint | ArchitectureLintTest 5/5 |
| A4 | brez elytre in klienta | lint + grep = 0 |
| A5 | dedicated server smoke | `m0-server-smoke.log`: `side=SERVER`, `Done (0.629s)`, `ready (dedicated=true)` |
| A6 | ločeni mehanski commiti | `d99d792`, `b121b5c` |

Veja združena v `main` z `--no-ff` (`262c819`), commiti ostanejo vidni. Obe veji sta na
GitHubu (`jakakriletic/barritone_converted_for_entities`); `main` po tem commitu je treba pushati.

**Naslednji korak:** M2 — noge (`BaritoneMoveHelper`, `BaritoneJumpHelper`, `/npcb attach`, tečaj T1).

---

## 2026-09-24 (4) — M1: jedro na strežniku (veja `m1-jedro`)

**Namen seje:** M1 do točke, ko jedro dela headless in ostane samo Windows build.

**Narejeno (preverjeno v oblaku, 38/38 JUnit)**

| # | Commit | Kaj |
|---|---|---|
| M1.1 | `d99d792`, `1b674dc` | mehanska relokacija `baritone.*` → `si.ladja.npcbaritone.core.*` (+ popravek za korenski paket); preverjeno z obratno zamenjavo |
| M1.2 | `b121b5c` | izrez DROP 179, CLIENT 4, **LATER 7** (`tools/portmap_cut.py`) — LATER je izrezan zato, da ne rabi prenosa že zdaj; vrne se iz `3a4ea58` v M10 |
| M1.3 | `20b912c` | MCP preimenovanja + Forge `isSideSolid`; `tools/mark_modified.py` (D-004) |
| M1.4a | `fe3d2bd` | mehansko: `IPlayerContext` → `IEntityContext`, `player()` → `entity()` … |
| M1.4–M1.11 | `a811786` | jedro se prevede (304 → 0 napak) nad `EntityLiving`; BSI nad `id2ChunkMap`, bere `ExtendedBlockStorage`; vitka instanca; register po entiteti; log4j; `SearchExecutor` |
| M1.6, M1.12, M1.13 | `ba606ca` | golden testi G1–G12, lint D-012/D-024/meje/elytra |
| M1.8 | `5b1290f` | nastavitve na instanco; statični `Baritone.settings()` odstranjen |
| M1.5 | `cc630a4` | omejen posnetek ob začetku iskanja; brez konteksta na tick |

Odstopanja od načrta (zapisana, ne tiho):
- **M1.7:** `WorldData`/`WorldProvider` sta **izbrisana**, ne minimalizirana — po izrezu predpomnilnika ju nič ne rabi.
- **M1.8:** profil instance nosi `BlockStateInterface` (`bsi.settings`), ker ga skoraj vse statične funkcije `MovementHelper` že dobijo; Automatone je namesto tega dodal parameter v ~90 datotek. Globalno ostanejo samo hevristika ciljev (`costHeuristic`, `axisHeight`), `chatDebug` in `censorCoordinates`.
- **M1.10:** `InputOverrideHandler` je ohranil ime (samo stanje vhodov, D-010); preimenovanje v `InputState` je lahko mehanski commit v M2.

Ugotovitve → RAZISKAVA §4b (V1: 4,7 % predelave v `pathing/**`; kontekst vsak tick; rob posnetka).

**Prevedeno, čaka na zagon (Windows):** `.\dev.ps1 build --offline` na veji `m1-jedro` (A1 Windows, A2 golden v Gradlu), `.\smoke-server.ps1` (A5).

**Odprto v M1:** nič vsebinskega; M1 se zapre z Windows buildom in smoke testom, nato merge `m1-jedro` → `main` (`--no-ff`, commiti ostanejo ločeni, A6).

**Naslednji korak:** uporabnik: `git checkout m1-jedro; .\dev.ps1 build --offline; .\smoke-server.ps1`. Nato M2 (noge: `BaritoneMoveHelper`, `/npcb attach`).

---

## 2026-09-24 (3) — M0 zaključen

Uporabnik je na Windowsu pognal build, server smoke in klient. Vsa merila M0 zelena:

| # | Merilo | Dokaz |
|---|---|---|
| A1 | `.\dev.ps1 build` zelen | `mod/build/libs/npcbaritone-0.0.1-m0.jar` (LICENSE + NOTICE v `META-INF`) |
| A2 | `runClient` naloži mod | `latest.log`: `NPC Baritone 0.0.1-m0 loaded (side=CLIENT…)`, `successfully loaded 5 mods`, integrirani strežnik `ready (dedicated=false)`, igralec vstopi v svet |
| A3 | `runServer` (dedicated) naloži mod | `docs/build-logs/m0-server-smoke.log`: `side=SERVER`, `Done (0.856s)`, `ready (dedicated=true)`, čist `stop` |
| A4 | `HarnessTest` zelen v oblaku in na Windowsu | Gradle JUnit: NpcbConfigTest 5/5, HarnessTest 8/8, IBlockAccessProbeTest 3/3 |
| A5 | uvoz = upstream | `tools/check-upstream-import.sh` OK (311 datotek) |
| A6 | profil napak zapisan | RAZISKAVA §1a |

Opombe iz loga (nenevarne): `Missing English translation for npcbaritone` (mod nima lang
datotek), FML `maven library folder` / `signature data` napake so standardne v dev okolju.

**Naslednji korak:** M1.1 — mehanska relokacija `mod/src/upstream` → `core`/`api`.

---

## 2026-09-24 (2) — M0: okolje, uvoz, headless harness

**Namen seje:** M0 do točke, ko ostane samo Windows build in zagon v igri.

**Narejeno (preverjeno v oblaku)**

- **Git:** `3a4ea58` upstream import (M0.4, D-026) — 311 datotek, bitno enakih
  `src/{api,main}` v `d9cb2d9` (A5: `bash tools/check-upstream-import.sh`);
  `1a38f97` dokumentacija priprave + submoduli. Lokalni git `user.name=jaka`.
- **M0.6:** `tools/cloud-compile.sh` — prevod `mod/src/main` + `mod/src/test` z
  `javac --release 8` in JUnit brez Gradla. `tools/cache/` (gitignore) ima
  `forgeSrc-1.12.2-14.23.5.2847.jar` (snapshot_20171003) in 42 knjižnic iz Gradle
  predpomnilnika (seznam: `versionJsons/1.12.2.json` + junit 4.13.2, hamcrest 1.3,
  jsr305 3.0.1, launchwrapper, asm 5.2, …).
- **M0.7 (A6):** profil napak upstream proti snapshot_20171003 = 81 / 43 / 38, istih 38
  mest kot v `MCP-PREIMENOVANJA.md` → RAZISKAVA §1a.
- **M0.9:** sonda `IBlockAccessProbeTest` → RAZISKAVA §4a. Iskanje poti bere samo
  `getBlockState`; **past za M1.5:** `Chunk.getBlockState` rabi `World` (NPE brez njega).
- **JUnit v oblaku: 16/16 zelenih** (`HarnessTest` 8, `IBlockAccessProbeTest` 3,
  `NpcbConfigTest` 5). Mutacijski preverbi: pokvarjen `SyntheticWorld.set` → 6 padcev,
  odstranjen clamp v configu → 1 padec (testi res testirajo).

**Prevedeno, čaka na zagon (Windows)**

- **M0.1** `mod/` Gradle projekt (FG 2.3-SNAPSHOT, wrapper 4.9 iz CNPC, Forge
  14.23.5.2847, snapshot_20171003); `upstream` ni v `sourceSets`.
- **M0.2** `dev.ps1` (Java 8: `NPCB_JAVA8_HOME` → `..\customNPC_rework\.tools\jdk8` →
  `C:\Program Files\Java\jdk1.8.0_202`), `prepare-assets.ps1` (kopija iz CNPC).
- **M0.3** `NpcBaritoneMod` (`@Mod npcbaritone`, `@NetworkCheckHandler` vedno `true`),
  `NpcbConfig` (`config/npcbaritone.cfg`: sekcije search/profile/movement/debug, obrezovanje mej).
- **M0.8** `SyntheticWorld`, `BootstrapOnce`, `HarnessTest` — zeleni v oblaku, na Windowsu še ne (A4).
- **M0.11** `smoke-server.ps1` (A3); runClient (A2) ročno.

**Blokirano:** nič.

**Naslednji korak (uporabnik, PowerShell v korenu repozitorija):**

```powershell
.\dev.ps1 setupDecompWorkspace --offline   # če pade zaradi odvisnosti: brez --offline
.\dev.ps1 build --offline                  # A1 + A4 (JUnit v Gradlu)
.\smoke-server.ps1 -AcceptEula             # A3
.\dev.ps1 runClient --offline              # A2: v logu 'npcbaritone' in 'successfully loaded'
```

Ko je vse zeleno: M0 zaključen → M1.1 (mehanska relokacija).

---

## 2026-09-24 — Priprava projekta

**Narejeno**

- Raziskava: prevod Baritona v1.2.19 proti Forge 1.12.2 (81 napak, 43 v Elytri, 38 drugod),
  velikost in klientske odvisnosti, vanilla mehanika z `javap`, headless sonda
  (`Bootstrap` + sintetični chunk), CustomNPC rework okolje in uporaba navigatorja,
  Automatonova časovnica (167 commitov), `ladja_mod` shipyard. → `docs/06-RAZISKAVA.md`
- 26 odločitev z dokazi in preverbami → `docs/02-ODLOCITVE.md`
- Arhitektura, faze M0–M10, register tveganj, protokol seje
- `docs/porting/`: port map vseh 348 izvornih datotek, Automatone roadmap, MCP preimenovanja
  (generirano s `tools/*.py`)
- Git repozitorij inicializiran (`main`, brez commita); reference pripete kot submoduli:
  - `references/baritone-1.12.2` @ `d9cb2d9` (v1.2.19)
  - `references/automatone` @ `843b8397`

**Prevedeno, čaka na zagon:** nič (kode še ni).

**Blokirano:** nič.

**Odprto (namenoma, rešuje M0)**

- Ali ima CNPC mapping (snapshot_20171003) enak profil napak kot §1 (M0.7).
- Katere `IBlockAccess` metode potrebujejo pravi `World` (M0.9).

**Naslednji korak:** M0.1 — Gradle projekt v `mod/` po vzoru CNPC okolja
([`milestones/M0-okolje-in-uvoz`](../milestones/M0-okolje-in-uvoz/README.md)).
Pred prvim commitom preglej `git status` (submoduli in dokumenti so pripravljeni, niso commitani).
