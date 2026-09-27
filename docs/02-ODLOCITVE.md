# 02 — Težka vprašanja in odločitve

Vsako vprašanje, ki bi sredi dela lahko ustavilo projekt, je tu **odgovorjeno vnaprej**:
z odločitvijo, dokazom (izmerjeno ali prebrano iz kode, ne domneva), posledico in
**preverbo** — kdaj in kako se pokaže, da je odločitev držala. Če preverba pade, se
odločitev ne popravlja tiho: doda se nova odločitev, ki staro izrecno zamenja.

Dokazi se sklicujejo na `docs/06-RAZISKAVA.md` (§) in na pripete reference.

Oznaka `CNPC D-0xx` pomeni odločitev v projektu customNPC_rework, ne tu.

---

## A. Osnova in izvor kode

### D-001 — Osnova je Baritone v1.2.19; Automatone je vodič, ne osnova

**Vprašanje.** Migrirati Automatone (Fabric 1.16–1.18, Yarn) nazaj na 1.12.2, ali vzeti
Baritone za 1.12.2 in vanj prenesti Automatonove *ideje*?

**Odločitev.** Osnova je `cabaletta/baritone` tag **v1.2.19** (`d9cb2d9`, 2023-08-17).
Automatone (`843b8397`) je **konceptualni vodič**: njegovih 167 commitov predelave je
razvrščenih po naših milestonih (`docs/porting/AUTOMATONE-ROADMAP.md`), koda se iz njega
jemlje samo kot vzorec, ne kot copy-paste.

**Dokaz.**
- Baritone v1.2.19 se proti našemu Forge 1.12.2 jarju prevede z **81 napakami**, od tega
  **43 v Elytri** (ki jo odstranimo, D-002) in **38 drugod** — 37 je MCP preimenovanj ali
  en manjkajoč Forge `isSideSolid`, ena je generični izraz v chat ukazu, ki odpade
  (§1, `docs/porting/MCP-PREIMENOVANJA.md`).
- Automatone je na Fabricu z Yarn imeni in 1.16+ API-jem (flattening, `FluidState`,
  Brigadier, Cardinal Components). Selitev nazaj bi zadela vseh ~33.500 vrstic.
- `master` Baritona (`f2679be`) se od v1.2.19 razlikuje samo v `ElytraBehavior.java`.

**Posledica.** Vse, kar je na 1.12.2 že rešeno (bloki z metadata, voda brez FluidState,
višina sveta 0–255), dobimo zastonj. Automatonove rešitve za entitete prevajamo ročno.

**Preverba.** M1: jedro se prevede in golden testi tečejo. Pade, če bi 1.12 posebnosti
zahtevale več kot ~20 % prepisa jedra (`pathing/**`).

### D-002 — Elytra in nativni nether-pathfinder se odstranita

**Odločitev.** `process/elytra/**`, `ElytraProcess`, `IElytraProcess` in odvisnost
`dev.babbaj:nether-pathfinder` (nativna knjižnica) se ne prenesejo.

**Dokaz.** 43 od 81 napak prevoda; nativna `.so/.dll` na strežniku je nepotrebno
tveganje; babbaj Maven je iz oblačnega okolja vrnil 403.

**Preverba.** Po M1 `grep -r elytra` v `mod/` vrne nič.

### D-003 — Paket se relocira, mod ima svoj ID

**Vprašanje.** Kaj se zgodi, ko igralec v enoigralskem načinu uporablja pravi Baritone
(klient), naš mod pa teče v integriranem strežniku istega JVM?

**Odločitev.** Vsa koda se premakne iz `baritone.*` v **`si.ladja.npcbaritone.*`**
(`...api`, `...core`, `...forge`). Mod ID **`npcbaritone`**. Ime se lahko spremeni
samo do konca M1; po tem je v API-ju.

**Dokaz.** Dva razreda `baritone.Baritone` v istem classloaderju = `LinkageError` ali tiho
napačen razred. Automatone je iz istega razloga naredil `e8b74bbe` "Repackage jars to
ladysnake.automatone".

**Preverba.** M2: dev klient z nameščenim Baritone 1.2.19 + našim modom zažene
enoigralski svet in NPC hodi.

### D-004 — Licenca LGPL-3.0

**Odločitev.** Knjižnica je LGPL-3.0 (kot Baritone in Automatone). Ohranijo se vse
glave datotek; spremenjene datoteke dobijo vrstico `Modified for NPC Baritone`;
`NOTICE.md` navaja izvor in commit. Integracijska koda v CustomNPC rework knjižnico
**uporablja** prek API-ja (povezovanje), zato CNPC ne podeduje LGPL. Izvorna koda
knjižnice mora biti dostopna vsem, ki dobijo jar (javen repozitorij ali source jar ob
vsaki izdaji).

**Dokaz.** `references/baritone-1.12.2/LICENSE` (LGPL-3.0 + `LICENSE-Part-2.jpg`),
Automatone LICENSE (LGPL-3.0).

**Preverba.** Ob vsaki izdaji: `LICENSE`, `NOTICE.md` in source jar so v izdaji.

### D-026 — Izvor v gitu: prvi commit je nespremenjen Baritone

**Odločitev.** M0 uvozi datoteke iz v1.2.19 **nespremenjene** v `mod/` kot en commit
("upstream import d9cb2d9"). Vse spremembe so v naslednjih commitih. Tako je vsaka
sprememba dokazljiva z `git diff` proti uvozu (zahteva LGPL §2a o označenih spremembah).

---

## B. Umestitev in razmerje do CustomNPC rework

### D-005 — Ločena knjižnica, izbirno ozadje, mehka odvisnost

**Vprašanje.** Koda znotraj CustomNPC rework ali samostojen mod? In kako to gre skupaj s
**CNPC D-012**, ki "stopnjo D (async iskanje, lasten gibalni sklad)" zavrača?

**Odločitev.**
1. Samostojen Forge mod `npcbaritone` z ločenim API paketom.
2. CNPC ga uporablja **samo, če je naložen** in je za NPC vklopljen (stikalo, privzeto
   vanilla — skladno s CNPC D-007).
3. Integracija v CNPC (M7) se začne šele z **novo odločitvijo v CNPC** (predlog
   "D-0xx: stopnja D se odpre kot izbirno ozadje za posamezne NPC-je"), ki se sklicuje
   na meritve M2.7 tega projekta. Do takrat se knjižnica razvija in meri na vanilla mobih.

**Dokaz.** CNPC D-012 (`docs/01-ARHITEKTURA.md` v CNPC) postavlja pogoj za stopnjo D in
izrecno omenja tveganje race conditionov. Ta projekt ta tveganja naslovi z D-012/D-013
tu in jih izmeri v M5; CNPC-jeva šest-veličinska meritev M2.7 je v M7 merilo sprejema.

**Preverba.** M7 ima A/B po M2.7 s tremi ponovitvami (CNPC D-016). Če Baritone ne
izboljša veličin 1–3 brez poslabšanja 5–6 čez izmerjeni razpon, ostane izklopljen.

### D-020 — Kaj od Baritona ohranimo

**Odločitev.** Ohranimo: A\* (`pathing/calc`), vse premike (`pathing/movement`),
izvajalec poti (`pathing/path`), cilje (`api/pathing/goals`), `PathingBehavior`,
`LookBehavior`, `CustomGoalProcess`, `PathingControlManager`, `Settings`.
Kasneje (M10): `FollowProcess`, `GetToBlockProcess`, rušenje/postavljanje.
Izpustimo: Mine/Build/Farm/Explore/Backfill/Elytra/InventoryPauser, vse chat ukaze, GUI,
izris, sheme, izbire, predpomnilnik regij, waypointe.

**Dokaz.** `docs/porting/PORT-MAP.md`: 120 datotek / ~17.000 vrstic ostane
(KEEP+ADAPT+REWRITE), 217 datotek / ~22.700 vrstic odpade (brez 6 Baritonovih testov v
`src/test`, ki se pregledajo v M1).

---

## C. Okolje

### D-007 — Build okolje je enako CustomNPC rework

**Odločitev.** Forge **14.23.5.2847**, ForgeGradle **2.3-SNAPSHOT**, Gradle wrapper
**4.9**, MCP **snapshot_20171003**, Java **8**. Skripta `dev.ps1` po vzoru CNPC.
Seja sme sama prevajati in poganjati JUnit v oblaku proti mapiranemu Forge jarju (kot
CNPC D-014); "narejeno" pomeni gradle build + zagon v igri pri uporabniku.

**Dokaz.** CNPC `OKOLJE.md`: to okolje na uporabnikovem računalniku dokazano deluje in
ima vse v Gradle predpomnilniku. Isti mappingi pomenijo, da je API jar direktno
uporaben v CNPC prevajanju. Baritone sam uporablja `stable_39`; razlike so v 38
preimenovanjih (§1).

**Preverba.** M0: prazen mod se zgradi in naloži v `runClient` in `runServer`.

### D-008 — Brez mixinov in coremoda

**Vprašanje.** Automatone med potjo z mixinom prekliče celoten AI ticka moba
(`MixinMobEntity#cancelAiTick`). Rabimo to?

**Odločitev.** Ne. Uporabimo šive, ki jih vanilla 1.12.2 že ima:
`PathNavigate` (zamenljiv, `protected navigator`), `EntityMoveHelper` in
`EntityJumpHelper` (oba `protected`, zamenljiva v podrazredu). `EntityLookHelper` je
`private final` in ga **ne** menjamo (D-011). Za tuje entitete (vanilla mobi, pirati)
se polja nastavijo z refleksijo (`ObfuscationReflectionHelper`, SRG imena se preverijo
v M2).

**Dokaz.** `javap` `EntityLiving` (§3): `protected EntityMoveHelper moveHelper`,
`protected EntityJumpHelper jumpHelper`, `protected PathNavigate navigator`,
`private final EntityLookHelper lookHelper`, `protected final void updateEntityActionState()`.
CNPC D-018 je iz istega razloga ("`updateEntityActionState` je `final`") zavrnil coremod.

**Posledica.** AI taski tečejo naprej (so "možgani"), Baritone je samo "noge". To je
za CNPC pravilno: napad, sledenje in tavanje morajo odločati še naprej.

**Preverba.** M2: vanilla zombi z našim navigatorjem hodi po poti in še vedno napade
igralca (EntityAIAttackMelee nespremenjen).

### D-009 — Baritone tiktaka iz `PathNavigate.onUpdateNavigation()`

**Odločitev.** Instanca Baritona na entiteti se posodobi v našem navigatorju, ne prek
Forge dogodka.

**Dokaz.** Vrstni red v `updateEntityActionState` (`javap`, §3):
`goalSelector` → **`navigator.onUpdateNavigation()`** → `updateAITasks` →
`moveHelper.onUpdateMoveHelper()` → `lookHelper.onUpdateLook()` → `jumpHelper.doJump()`,
nato `travel()` v `onLivingUpdate`. Taski torej postavijo cilj, navigator izračuna vhode,
naš move/jump helper jih uporabi, `travel` premakne entiteto — v istem ticku, brez
zamika in brez dogodka, ki bi ga lahko drug mod preklical.

**Preverba.** M2 test: cilj, postavljen v ticku *t*, ima prve vhode v ticku *t*
(ali *t+1*, ko se čaka na pot).

---

## D. Gibanje

### D-006 — Kontekst je `EntityLiving`, ne igralec

**Odločitev.** `IPlayerContext` postane `IEntityContext` z `EntityLiving entity()`,
`WorldServer world()`, `IWorldData worldData()`, `feetPos()`, `headPos()`, `rotation()`
in `frame()` (D-021). Igralcev ne podpiramo.

**Dokaz.** 227 klicev `ctx.player()` + 117 `ctx.playerFeet()` + 83 `ctx.world()` (§2);
Automatone `8a97376b` (50 datotek, +208/−222) je isti korak.

### D-010 — Model vhodov in hitrosti

**Vprašanje.** Baritonove cene (npr. `WALK_ONE_BLOCK_COST = 20/4.317`) in izvedba
skokov predpostavljajo igralčevo hitrost. Mob pa se premika drugače.

**Odločitev.**
- Naš `EntityMoveHelper`, dokler vodi Baritone, vsak tick naredi **v tem vrstnem redu**:
  `setAIMoveSpeed(atribut MOVEMENT_SPEED)` → `setMoveForward(±1 ali 0)` →
  `setMoveStrafing(±1 ali 0)`; sneak pomnoži z 0,3. Jump helper postavi
  `setJumping(JUMP)`. Sprint gre prek `setSprinting` (vanilla modifier +30 %).
- **Privzeti način "kot igralec":** med vodenjem je osnovni `MOVEMENT_SPEED` 0,1 (igralec).
  Cene veljajo, parkour je dovoljen samo v tem načinu.
- **Način "lastna hitrost":** osnovna hitrost ostane NPC-jeva; cene hoje se pomnožijo z
  razmerjem 0,1 / hitrost, parkour je izklopljen.
- Ko Baritone ne vodi, helperja delegirata na vanilla (`super`).

**Dokaz (javap, §3).** `EntityLiving.setAIMoveSpeed(f)` pokliče tudi `setMoveForward(f)`
— zato mora `setMoveForward` priti **za** njim, sicer je vhod povozjen. Vanilla
`EntityMoveHelper` v stanju WAIT pokliče `setMoveForward(0)`, zato ga moramo zamenjati.
`EntityLivingBase.onLivingUpdate` vhoda pomnoži z 0,98 za vse entitete, kot pri igralcu.
`setSprinting` na `EntityLivingBase` doda `SPRINTING_SPEED_BOOST` vsem. Automatone ima
enako logiko (`InputOverrideHandler#onTickServer`) in sneak ×0,3.

**Preverba.** M2: izmerjena hitrost na ravnini 4,32 ± 0,2 m/s (hoja) in 5,6 ± 0,25 m/s
(sprint) v načinu "kot igralec"; tečaj s skoki 1–3 bloke brez padca.

### D-011 — Obrat telesa

**Odločitev.** `LookBehavior` nastavlja `rotationYaw` (smer gibanja, jo uporablja
`travel`) in `renderYawOffset`, z omejitvijo obrata na tick (privzeto 30°). Glavo
(`rotationYawHead`, pitch) pusti vanilla `EntityLookHelper`, tako da NPC med hojo še
vedno gleda igralca. Automatonov `a8d793f9` ("Decouple basic movements from look
direction") je vzorec: osnovni premiki ne potrebujejo natančnega pogleda.

**Dokaz.** `lookHelper` je `private final` (§3), torej ga niti podrazred ne zamenja;
`travel` bere `rotationYaw`, ne glave.

**Preverba.** M2: tečaj z ostrimi zavoji; entiteta ne "drsi bočno".

### D-015 — Brez rušenja, postavljanja in inventarja; vrata neposredno

**Status:** delno zamenjana z D-031 (2026-09-26) — velja za vse entitete, ki niso registrirane kot worker.

**Odločitev.** NPC profil ima `allowBreak = allowPlace = allowInventory = false`
(Baritone privzeto `true/true/false`). Vrata, ograjna vrata in lopute se odpirajo z
neposrednim klicem bloka (kot CNPC `EntityAIOpenAnyDoor`), ne z desnim klikom igralca.
Rušenje/postavljanje ostane za M10 kot izrecen profil.

**Dokaz.** `Settings.java` v1.2.19: `allowBreak=true` (vr. 55), `allowPlace=true` (70),
`allowInventory=false` (75), `allowParkour=false` (340). Baritone vrata odpira s
`CLICK_RIGHT` prek `IPlayerController.processRightClickBlock(EntityPlayerSP…)`, ki ga
entiteta nima. Automatone `3216de48`, `b1899f30` sta popravljala prav to.

**Preverba.** M4: tečaj z vrati (lesena, železna = neprehodna), ograjnimi vrati, loputo.

### D-019 — Velikost entitete do M8: en stolpec 1×2

**Status: zamenjana z D-028 (2026-09-25).** Velja še kot privzeto obnašanje navigatorja
(stikalo `movement.largeEntities = false`).

**Odločitev.** Do M8 Baritone vodi samo entitete s `width ≤ 1,0` in `height ≤ 2,0`.
Za večje navigator zavrne in pusti vanilla. M8 prenese Automatonove "size-aware"
premike (9 commitov, `324bd259` … `a9c929f0`).

**Dokaz.** Baritone 1.12.2 ima velikost igralca vgrajeno v premike; Automatone je
dimenzije vpeljal šele v `CalculationContext` (`width`, `height`, `requiredSideSpace`).
CNPC NPC velikosti 1–10 skalirajo `width`/`height` z `size/5` (`EntityNPCInterface`
vr. 1086–1087): privzeti NPC (size 5) je 0,6 × 1,8 in pade v okvir.

### D-028 — Velikosti entitet: okvir blokov na sredini, meje 3 stolpci × 4 bloki (M8)

**Vprašanje.** Kako Baritone vodi entitete, ki niso 1×2 (CNPC size 1–10, pajek, golem),
in do katere velikosti mu zaupamo?

**Odločitev.**
- **Model:** entiteta je okvir blokov na sredini bloka nog: `heightBlocks = ⌈višina⌉`,
  `sideSpace = ⌈(širina − 1) / 2⌉` dodatnih stolpcev na vsako stran (0 za širino ≤ 1,
  1 za širino ≤ 3). Premik preveri samo bloke, v katere okvir vstopi (Automatone
  `324bd259` … `a9c929f0`), in ciljni stolpec.
- **Standardna velikost** (`sideSpace 0`, `heightBlocks 2`: igralec, zombi, CNPC size 3–5)
  ostane na upstream veji premikov; splošna veja zanjo računa enako (test).
- **Spust/padec široke entitete:** kolizija jo porine naprej, zato se preverijo stolpci
  pred ciljem (`forwardSpan`), ne simetrični okvir. Dovoljeno samo za širino < 2 (sredina
  ostane v ciljnem stolpcu); širina ≥ 2 se ne spušča.
- **Omejitve:** široke entitete ne hodijo diagonalno, ne plezajo po lestvah in trtah, ne
  skačejo (parkour samo 1×2); nič se ne ruši ali postavlja (D-015). Model je konservativen:
  entiteta širine 1,2 ne gre skozi režo širine 2 (potrebuje 3 stolpce).
- **Meje navigatorja:** `sideSpace ≤ 1` in `heightBlocks ≤ 4` (do 3,0 × 4,0). Večje dobijo
  vanilla navigacijo. Stikalo `movement.largeEntities` (privzeto false = D-019) — obnašanje
  porabnika se brez njega ne spremeni; ukazi `/npcb` in tečaji velikosti ne preverjajo.

**Dokaz.** Automatone je dimenzije vpeljal v 9 commitih (M8 v `AUTOMATONE-ROADMAP.md`) in
za spust ni naredil ničesar; simetrični okvir pri spustu za širino 1,2 zadene blok, s
katerega entiteta stopi (G6 bi padel). `SizeAwareEquivalenceTest`: 1,2 M primerjav premikov
na naključnih terenih, splošna veja = upstream za 0,6 × 1,8. `GoldenSizeTest`: G1–G14 za
16 velikosti (0,3–2,0 × 0,9–3,6). `CourseT3PathTest`: T1 + T2 za CNPC size 1/3/5/7/10,
9 pričakovanih neuspehov z razlogom (hodnik 1×2 za višino 2,52, vrata za širino 1,2, lestve …).

**Preverba.** M8: `t3-run.ps1` — za vsako velikost T1 ≥ 9/10 in T2 ≥ 9/10 odsekov
pričakovanega izida, noben pričakovan neuspeh ne konča s TIMEOUT (A2).

---

## E. Svet, niti in zmogljivost

### D-012 — Svet se bere samo prek BlockStateInterface; nikoli se ne naloži chunk

**Odločitev.** Prenesena koda nikjer ne kliče `World.getBlockState` ali `getChunk*`, ki
nalaga. Vse gre prek `BlockStateInterface` nad **že naloženimi** chunki
(`ChunkProviderServer.getLoadedChunk` / kopija `id2ChunkMap`). JUnit "lint" test
preišče izvorno kodo in pade, če se prepovedani klic pojavi izven dovoljenih razredov.

**Dokaz.** Na strežniku `World.getBlockState` → `provideChunk` → naloži ali **generira**
chunk. `ladja_mod` `ShipNavigator` dokumentira, da je branje v shipyardu na ta način
"zraslo otok sredi ladje". Automatone je iz istega razloga dodal "chunkloading
cancellation" mixine (`35becc89`). CNPC M5.6 opozarja, da vanilla iskanje zna naložiti
chunk.

**Preverba.** M2/M5: števec `ChunkEvent.Load` med 1000 iskanji, sproženimi ob robu
naloženega območja, je 0.

### D-013 — Posnetek chunkov: omejena kopija na strežniški niti

**Odločitev.** Ob začetku iskanja se na strežniški niti skopira **samo** del
`id2ChunkMap` znotraj pravokotnika (začetek ∪ cilj) + rob (config, privzeto 8 chunkov,
strop 32). Iskalna nit bere samo to kopijo. Spremembe blokov med iskanjem so dovoljene:
izvajalec poti vsak premik pred izvedbo ponovno preveri (to Baritone že dela) in ob
neskladju sproži novo iskanje.

**Dokaz.** Baritone 1.12.2 že dela kopijo (`new Long2ObjectOpenHashMap<>(worldLoaded)`,
`BlockStateInterface` konstruktor, `copyLoadedChunks`) — a celotne mape, na klientu.
`id2ChunkMap` je na strežniku `public final` (§3). Branje `ExtendedBlockStorage` med
pisanjem glavne niti je v najslabšem primeru zastarelo, ne sesuje (Baritone to počne na
klientu že leta; Automatone na strežniku enako).

**Preverba.** M5: µs kopije na iskanje (cilj: p95 < 100 µs pri robu 8), 0 izjem v
1-urnem stresnem testu z rušenjem blokov ob poteh.

### D-014 — Nenaložen svet je meja; dolge poti po segmentih

**Odločitev.** Nenaložen chunk je neprehoden (`isLoaded == false`). Če je cilj zunaj,
iskanje vrne najboljšo delno pot, izvajalec jo prehodi in iskanje se nadaljuje
(Baritonov "plan ahead" / segmenti). Brez Baritonovega diskovnega predpomnilnika regij.

**Dokaz.** NPC v nenaloženem chunku se ne posodablja; iskati poti tja nima smisla.
Automatone je predpomnilnik regij odstranil (cache: 10 → 7 datotek, 2026 → 732 vrstic).
To je hkrati CNPC-jev M4.10 ("nadaljevanje delne poti"), rešen z obstoječo Baritonovo
logiko.

### D-017 — Omejen executor, vrsta, deljenje poti, meritve

**Odločitev.** Baritonov `ThreadPoolExecutor(4, Integer.MAX_VALUE, …, SynchronousQueue)`
(= nova nit za vsako iskanje nad 4) se zamenja z:
- fiksnim bazenom **2 niti** (config 1–8),
- prednostno vrsto (bližina igralca, starost zahteve), stropom dolžine vrste,
- **deljenjem**: zahteva z istim ciljem in bližnjim začetkom (isti chunk) dobi rezultat
  že tekočega iskanja,
- NPC časovnimi omejitvami (začetno `primaryTimeoutMS=150`, `failureTimeoutMS=500`
  namesto 500/2000; končne vrednosti iz meritev M5),
- števci: iskanj/s, µs/iskanje (p50/p95), čakanje v vrsti, delež deljenih.

**Dokaz.** `Baritone.java` vr. 61 (v1.2.19). CNPC D-012: pri 27 NPC-jih je p99 ticka že
115 ms; CNPC M5.6: 8 NPC-jev → 8 neodvisnih iskanj je največji pričakovani dobitek.

**Preverba.** M5: 200 mobov z Baritonom, MSPT prispevek (samo glavna nit) izmerjen in
zapisan; nobena nit ni ustvarjena izven bazena.

### D-016 — Nastavitve na instanco (profili)

**Odločitev.** `Settings` ni več globalen singleton. Obstaja privzet NPC profil in
poimenovani profili (config); instanca ima referenco na profil in lahko prepiše
posamezne vrednosti. Brez Automatonovih polnih kaskadnih nastavitev in ukaza `/setting`.

**Dokaz.** 1.12.2 kliče `Baritone.settings()` statično; Automatone je to rešil v treh
commitih (`89b1174a`, `7bd582c2`, `a1bb2422`; skupaj ~90 datotek dotaknjenih) — mi
potrebujemo samo profil + prepis.

### D-027 — Dve stopnji meje zmogljivosti (M5)

**Odločitev.** Merilo M5 A1 (prispevek glavne niti pri 200 mobih) ima dve stopnji:
- **Stopnja B — zdaj:** p95 ≤ **5 ms/tick**. Vrata V3 za M6 in za prvega porabnika, drug mod,
  ki ne rabi toliko entitet.
- **Stopnja A — odprta:** p95 ≤ **2 ms/tick**. Pogoj za integracijo v CustomNPC rework (M7),
  kjer bo NPC-jev veliko. Do M7 ostane zapisana kot cilj, ne blokira M5/M6.

Obe se merita z istim tečajem T4 in se poročata ločeno; stopnja A, ki ni dosežena, je
zapisana kot odprta naloga pred M7 (manj niti, krajše časovne omejitve, več deljenja ali
omejitev števila NPC-jev z Baritonom pri CNPC).

**Dokaz.** Uporabnik 2026-09-25: "opcija 1 (2 ms) odprta za kasnejšo integracijo s
CustomNPC, opcija 2 (5 ms) za drug mod".

**Preverba.** M5 tabela 50/200 mobov z obema mejama; M7 se ne začne brez stopnje A ali
izrecne nove odločitve.

---

## F. Integracija

### D-018 — Pogodba `PathNavigate` pri asinhronem iskanju

**Vprašanje.** `tryMoveToXYZ` mora takoj vrniti `true/false`, Baritone pa pot išče v
ozadju. Kako obstoječi AI taski dobijo pravilen odgovor?

**Odločitev.** Adapter `BaritonePathNavigate extends PathNavigateGround`:
- `tryMoveToXYZ / tryMoveToEntityLiving` → sprejme cilj, vrne `true`, če je cilj v
  naloženem svetu in entiteta ustreza D-019; iskanje je "pending".
- `noPath()` je `false`, dokler je iskanje pending ali pot teče; `true` po neuspehu ali
  na cilju.
- Isti cilj (±1 blok) ob ponovnem klicu **ne** začne novega iskanja (tasks kličejo
  `tryMoveTo*` pogosto).
- `clearPath()` ustavi; ponovni isti `tryMoveTo*` v 10 tickih nadaljuje brez novega
  iskanja (debounce).
- **Sinhrona vprašanja** (`getPathToPos`, `getPathToEntityLiving`, `setPath`) gredo na
  vanilla `PathFinder` (hibrid), ker jih kličejo samo 3 datoteke CNPC in pričakujejo
  takojšen `Path`.
- `getPath()` vrne vanilla `Path`, sestavljen iz Baritonove poti (za taske, ki berejo
  točke).

**Dokaz.** Uporaba navigatorja v CNPC `reference-src` (§5): `clearPath` 28×,
`noPath` 21×, `tryMoveToXYZ` 17×, `tryMoveToEntityLiving` 3×, `setSpeed` 2×,
`setPath` 2×, `getPathToEntityLiving` 2×, `getPath` 2×, `getPathToPos` 1×;
sinhroni `getPathTo*` samo v `EntityAIAttackTarget`, `EntityAIZigZagTarget`, `DataScenes`.

**Preverba.** M6: vanilla zombi in vaščan z adapterjem opravita tečaj z nespremenjenimi
AI taski; M7: vseh 21 CNPC AI datotek z navigatorjem je pregledanih in zapisanih.

### D-021 — Referenčni okvir v kontekstu od M1 naprej

**Odločitev.** `IEntityContext` ima `frame()`: pretvorbo med svetom, v katerem se išče
pot, in svetom, v katerem entiteta stoji. Do M9 je vedno identiteta. M9 doda ladijski
okvir: iskanje v shipyard koordinatah, izvedba prek `ShipTransform`.

**Dokaz.** `ladja_mod` ladijski bloki so **pravi bloki v pravih naloženih chunkih
shipyarda** (`ShipNavigator` javadoc) in `ShipNavigator` že dela vanilla iskanje v
ladijskem prostoru. Baritone lahko torej išče tam brez sprememb; manjka samo
pretvorba položaja in yaw. Če okvirja ni v API-ju od začetka, ga je kasneje treba
vriniti v 400+ klicev položaja.

### D-023 — Stanje poti se ne shranjuje

**Odločitev.** Pot in iskanje sta prehodna. Po nalaganju sveta lastnik (AI task, skripta)
ponovno pokliče `tryMoveTo*`. Knjižnica ne piše NBT.

**Dokaz.** Vanilla navigator se enako ne shranjuje; CNPC ima svoje cilje v svojih podatkih.

### D-024 — Samo strežnik; klient je izbiren

**Odločitev.** Vse delovanje je na strežniku. Klient z modom dobi samo debug prikaz poti
(M3). `@NetworkCheckHandler` sprejme povezavo ne glede na to, ali ima druga stran mod
(vzorec iz `xaerofleetcommand`). Nobena registracija entitet, blokov ali predmetov (D-025).

**Preverba.** M3: vanilla klient (brez moda) se poveže na strežnik z modom in vidi NPC-je
hoditi.

### D-025 — Testi na vanilla mobih, brez lastne entitete

**Odločitev.** Za razvoj in teste se Baritone pripne na obstoječe vanilla mobe
(`/npcb attach` na zombija, huska, vaščana; vsi 0,6 × 1,95). Način `puppet` jim odstrani
AI taske, da jih vodi samo ukaz.

**Dokaz.** Lastna entiteta bi bila vpisana v Forge register, ki se sinhronizira s
klientom — vanilla klient bi bil zavrnjen, kar krši D-024.

---

## G. Testiranje

### D-022 — Testna strategija

**Odločitev.** Tri ravni:
1. **Headless JUnit**: `Bootstrap.register()` + sintetični chunki (`new Chunk(null,x,z)`
   z ročno postavljenimi `ExtendedBlockStorage`) → A\* na znanih terenih ("golden"
   poti: ravnina, zid z režo, stopnice 1–3, padec 3 in 4, voda, vrata, nedosegljiv cilj).
2. **Scenariji v igri**, skriptirani kot v CNPC (`*-run.ps1`, prisilno naloženi chunki —
   CNPC D-011), z merili v strojno berljivem zapisu.
3. **Dedicated server smoke** na koncu vsakega milestona.

**Dokaz.** Sonda v oblaku (§4): `Bootstrap.register()` v 2,2 s, branje/pisanje
`ExtendedBlockStorage` na chunku brez sveta deluje, `Blocks.OAK_DOOR` stanja so na voljo.

---

## H. Worker: delo in procesi (M11–M15)

Ozadje: uporabnik 2026-09-26 želi polno funkcionalnost Baritona (rušenje, postavljanje,
rudarjenje, farmanje, gradnja), vendar modularno. CustomNPC uporablja samo navigacijo,
ladja_mod pa na isti knjižnici gradi "workerje". Gameplay (kdaj, zakaj, kam odnesti) ostane
v porabniku, knjižnica nudi zmožnosti.

**Kaj dela Automatone (preverjeno v `references/automatone` @ `843b8397`):** vse procese
(`MineProcess`, `BuilderProcess`, `FarmProcess` …) je obdržal, rušenje in postavljanje pa
**dela samo za igralske entitete**. `AutomatoneComponents` vsem `LivingEntity` registrira
`DummyEntityController` (vse metode vrnejo `false`/`FAIL`, doseg 0), `ServerPlayerController`
pa samo `ServerPlayerEntity` (tudi njegovim fake playerjem). `EntityContext.inventory()` je
za ne-igralca `null`, `InventoryBehavior` in `BuilderProcess` ne-igralca preskočita
(`instanceof PlayerEntity`). Zaščita (`df9a13fe`) je `world.canPlayerModifyAt(player, pos)`,
za ne-igralca vedno "ni zaščiteno". Mob, ki koplje ali gradi, v Automatonu torej **ne
obstaja**; to je novo delo, Automatone je vzorec samo za igralsko pot (`ServerPlayerController`,
ki gre skozi vanilla `interactionManager` z mixinom za stanje kopanja).

### D-029 — Distribucija: knjižnični mod, mehka odvisnost porabnikov

**Vprašanje.** Naj se jedro vgradi v CustomNPC in ladja_mod ali je ločen mod?

**Odločitev.** Ločen mod `npcbaritone` (knjižnica, ne addon). Porabniki se prevedejo proti
`api` jarju (`compileOnly`), v `@Mod` imajo `dependencies = "after:npcbaritone"` in
dostopajo samo prek mostu, ki najprej preveri `Loader.isModLoaded("npcbaritone")` in
različico API-ja iz manifesta. Brez knjižnice porabnik deluje kot prej (D-005). Vgrajevanje
kopije v vsak mod je prepovedano. Jar-in-jar (`ContainedDeps` v manifestu porabnika) je
dovoljen samo, če preverba pokaže, da Forge ob dveh vgrajenih kopijah naloži eno.

**Dokaz.** Dve kopiji istih razredov v enem classloaderju = `LinkageError` (D-003); dve
relocirani kopiji = dva bazena niti, dve vrsti in brez deljenja iskanj (D-017, meritve M5
predpostavljajo en primerek); LGPL je pri ločenem jarju trivialen (D-004).

**Preverba.** M7.2 in M9.6 (most, brez knjižnice bitno enako); jar-in-jar: `launcher-test.ps1`
s CNPC in ladja_mod, ki oba nosita knjižnico, na klientu in dedicated strežniku (M11.10).

### D-030 — Tri plasti; worker je izrecna registracija; API 2 je dodaten

**Odločitev.**
1. **Plast 1 — navigacija** (obstoječe, API 1: `NpcBaritone`, `INpcNavigator`, `NavListener`).
   Ostane nespremenjena; CustomNPC rabi samo to.
2. **Plast 2 — delo:** rušenje, postavljanje, orodja, inventar, dovoljenja.
3. **Plast 3 — procesi:** `mine`, `getToBlock`, `farm`, `build`, `follow`.

Plasti 2 in 3 obstajata za entiteto samo, če jo porabnik registrira:
`NpcBaritone.worker(entity, WorkerSpec)` → `INpcWorker`. `WorkerSpec` vsebuje inventar
(`IItemHandler`), lastnika (`GameProfile`), dovoljenja (`IWorkPermission`), delovno območje
(D-033) in profil. Entiteta brez registracije fizično nima rok (D-032), zato navigacijski NPC
ne more porušiti bloka ne glede na nastavitve. API 2 je nov paket
`si.ladja.npcbaritone.api.work`; manifest `NpcBaritone-Api-Version: 2`; noben podpis API 1
se ne spremeni. Strežniški config `worker.enabled` (privzeto `true`) plast 2 in 3 izklopi v
celoti (`worker()` vrne `null`).

**Dokaz.** En mod z enim executorjem (D-017) je pogoj za deljenje iskanj; dva jarja (core +
addon) bi pomenila dva mod ID-ja in usklajevanje verzij brez koristi, ker je ločitev že na
ravni API-ja in registracije.

**Preverba.** M11 A5: T1+T2 z navigacijskim profilom po M11 nespremenjena (0 spremenjenih
blokov); `ApiJarTest` prevede porabnika API 1 proti novemu jarju.

### D-031 — Rušenje, postavljanje in inventar samo za workerja (delno zamenja D-015)

**Odločitev.** D-015 ostane v veljavi za vse ne-registrirane entitete (navigacija, CNPC). Za
workerja se odprejo `allowBreak`, `allowPlace` in `allowInventory` prek profila `worker`
(D-016), vendar samo znotraj delovnega območja in dovoljenj (D-033). Omejitve:
- velikost workerja do M10: standardna veja (širina ≤ 1, višina ≤ 2, D-028); pri večjih je
  rušenje/postavljanje v ceni izklopljeno (hodi kot navigacijski NPC);
- `blocksToDisallowBreaking` dobi NPC privzete vrednosti: vsi bloki s `TileEntity`
  (skrinje, peči, …), postelje, vrata, spawnerji, bedrock, portal; porabnik jih sme razširiti,
  ne zmanjšati pod `TileEntity` pravilo;
- lava/voda vedra (MLG) samo, če jih inventar ima in profil dovoli (privzeto ne).

**Dokaz.** Cene rušenja in postavljanja so v portu že prisotne, samo izklopljene
(`NpcProfile`: `allowBreak = allowPlace = false`; `CalculationContext` in premiki še
postavljajo `CLICK_LEFT/RIGHT`). Manjka izvajalec (roke) in inventar.

**Preverba.** M11 T5 in invariante (M11 A2–A4).

### D-032 — Roke: Forge `FakePlayer` kot posrednik, brez mixinov

**Vprašanje.** Kako mob poruši ali postavi blok tako, da se obnaša kot igralec (čas kopanja,
orodje, dropi, orientacija stopnic, zaščitni modi), brez mixinov (D-008)?

**Odločitev.** Vsak worker dobi "roke": `EntityHands implements IPlayerController`, ki za
vsako dejanje uporabi `FakePlayer` (`FakePlayerFactory.get(world, lastnikov profil)`) kot
posrednika. Pred dejanjem se mu nastavijo pozicija, rotacija (iz `LookBehavior` entitete) in
predmet v roki (iz inventarja, D-034); po dejanju (v `finally`) se predmet vrne.
- **Rušenje:** napredek se računa kot vanilla (`IBlockState.getPlayerRelativeBlockHardness`
  na tick, vsota ≥ 1), prikaz razpok `world.sendBlockBreakProgress(id entitete, …)`, zamah z
  roko entitete; zaključek `fakePlayer.interactionManager.tryHarvestBlock(pos)` (proži
  `BreakEvent`, orodje, fortune/silk touch, obrabo orodja).
- **Postavljanje:** `interactionManager.processRightClickBlock(...)` z blokom v roki
  (proži `RightClickBlock` in `PlaceEvent`; orientacija stopnic, vrat, hlodov je vanilla).
- **Lastnik:** `GameProfile` iz `WorkerSpec` (npr. lastnik ladje), sicer
  `[NpcBaritone]`. Zaščitni modi (claimi) tako odločajo po pravicah lastnika.
- Ne-registrirana entiteta ima `DummyEntityController` (kot Automatone): vse `false`.

**Dokaz.** Automatone za igralca uporablja isti vanilla `interactionManager`
(`ServerPlayerController`), za stanje kopanja pa rabi mixin; napredek izračunamo sami po isti
formuli, zato mixin ni potreben. `tryHarvestBlock` in `processRightClickBlock` sta v 1.12
javna. **Odprto:** `FakePlayer.connection` je `null`, `ForgeHooks.onBlockBreakEvent` ob
preklicu pošlje paket prek `connection` — sonda M11.1 (javap + test) določi, ali je potreben
prazen `NetHandlerPlayServer`.

**Preverba.** M11 T5 (čas kopanja z različnimi orodji ± 1 tick od vanilla, orientacija
postavljenih stopnic), M11 A3 (preklican `BreakEvent` = blok ostane).

### D-033 — Varovala: obvezno območje, dovoljenja v ceni in ob izvedbi

**Odločitev.** Worker ne ruši in ne postavlja **izven delovnega območja** (seznam AABB v
`WorkerSpec`; brez območja plast 2 ni aktivna). Preverba je na dveh mestih:
1. **pri iskanju poti** (`CalculationContext.isProtected` → `COST_INF`, vzorec Automatone
   `df9a13fe`): izven območja, `IWorkPermission.canBreak/canPlace` porabnika,
   spawn protection in meja sveta, D-031 seznam — iskanje poti tako obide, kar ne sme rušiti, namesto da bi se zataknilo;
2. **ob izvedbi**: iste preverbe še enkrat (svet se je lahko spremenil),
   `WorldServer.isBlockModifiable(fakePlayer, pos)` + `BreakEvent` /
   `PlaceEvent` prek rok (D-032). Preklic = premik ne uspe, ponovno načrtovanje.

Preverba pri iskanju teče na iskalni niti: območje, seznam D-031, polmer spawn protection in
meja sveta se preberejo na glavni niti ob gradnji `CalculationContext` (nespremenljivi
podatki); `IWorkPermission.canBreak/canPlace` ima pogodbo **samo branje, varno za niti**
(porabnik preverja lastne nespremenljive podatke, npr. meje ladje). Eventi in zaščitni modi se
kličejo samo ob izvedbi na glavni niti.

**Dokaz.** Baritone 1.2.19 `isPossiblyProtected` vrne vedno `false` (TODO #220); Automatone
je dodal samo igralsko preverbo. Brez dvojne preverbe worker ali zatakne (pot skozi zaščiten
blok) ali ruši v zaščitenem.

**Preverba.** M11 A2 (0 porušenih/postavljenih blokov izven območja in v zaščitenem stebru T5).

### D-034 — Inventar: porabnikov `IItemHandler` je edini vir resnice

**Odločitev.** Knjižnica nima svojega inventarja. `WorkerSpec` poda Forge `IItemHandler`
(ladja_mod: lastna skrinja workerja; CNPC kasneje: NPC inventar). Pravila:
- orodje in bloki za postavljanje se iz njega **izvlečejo za eno dejanje** in v istem ticku
  vrnejo (z obrabo) v `finally`; roke ne hranijo predmetov med ticki;
- **dropi gredo neposredno v inventar**: `BlockEvent.HarvestDropsEvent` z `harvester` = naš
  `FakePlayer` → `insertItem`, ostanek se spusti v svet kot `EntityItem`;
  pobiranje `EntityItem` iz sveta (Baritone `mineScanDroppedItems`) ostane za ostanke;
- poln inventar → dogodek `onInventoryFull` porabniku; proces se ustavi (ne ruši naprej
  v prazno);
- `ToolSet` izbira orodje iz tega inventarja (ne iz hotbara igralca); cena rušenja se
  izračuna ob gradnji `CalculationContext` (predpomnjena po stanju bloka), iskalna nit
  inventarja ne bere.

**Dokaz.** Automatone za ne-igralca inventarja nima (`inventory() == null`). Dva vira
resnice (kopija v rokah + porabnik) sta klasičen izvor dupe hroščev.

**Preverba.** M11 A4: ohranitev predmetov (inventar + dropi v svetu = pričakovano) po T5 in
po prekinitvi sredi kopanja (smrt, odstranitev entitete, unload chunka).

### D-035 — Iskanje blokov na strežniku (nadomestek `WorldScanner`)

**Odločitev.** Baritonov `WorldScanner`/`FasterWorldScanner` (klientski `ChunkProviderClient`)
in `CachedWorld` (predpomnilnik regij, D-014) se ne preneseta. Nov `ServerBlockScanner`:
- bere samo **naložene** chunke znotraj območja procesa (D-012, D-014), prek posnetka D-013;
- teče v bazenu iskanja (D-017), ne na glavni niti, z omejitvijo chunkov na zahtevo;
- rezultat (seznam pozicij) se deli med workerji z istim filtrom in istim območjem (vzorec
  deljenja iskanj), velja do spremembe bloka v teh chunkih ali največ N tickov;
- privzeto samo **izpostavljeni** bloki (vsaj ena ploskev ob zraku/tekočini), stikalo
  `legitMine=false` dovoli "rentgen".

**Dokaz.** `MineProcess` v 1.2.19 kliče `getCachedWorld().getLocationsOf` in
`WorldScanner.scanChunkRadius` (vr. 370, 385), oboje klientsko; Automatone je skeniranje
optimiziral (`b3da3410`, `d0970ec6`) in popravil NPE (`a3081aac`) — vzorec za M12.

**Preverba.** M12 A3 (µs skeniranja p95, 0 naloženih chunkov) in D-038.

### D-036 — Aktiven proces ima prednost pred AI taski; stanje procesa se ne shranjuje

**Odločitev.**
- Ko ima worker aktiven proces (mine/farm/build/follow), `BaritonePathNavigate.tryMoveTo*`
  iz AI taskov vrne `false` in ne prekliče procesa; `NavListener` dobi `BUSY` (nova
  vrednost `NavState`, ki jo dobijo samo registrirani workerji — porabnik API 1 je nikoli ne vidi). Porabnik, ki
  želi, da AI prevzame (npr. boj), pokliče `INpcWorker.pause()` / `resume()` (vzorec
  Baritonov `PathingControlManager` s prioriteto procesov).
- Proces se ne shranjuje v NBT (razširitev D-023). Porabnik hrani svoj "job" (npr. "100
  kamna, 40 narejeno") in ga po nalaganju ponovno zažene; knjižnica mu da napredek prek
  `IWorkerListener.onProgress`.

**Dokaz.** Vanilla taski (`EntityAIWander`) kličejo `tryMoveTo` vsakih nekaj sekund; brez
prednosti bi worker vsakič opustil delo. D-023: knjižnica ne piše NBT.

**Preverba.** M11 A6 (worker z `EntityAIWander` konča T5; `pause/resume` med kopanjem).

### D-037 — Gradnja: programske sheme, datoteke, več workerjev

**Odločitev.**
- `ISchematic` in sheme iz Baritona (`Fill`, `Walls`, `Shell`, `Composite`, `Mask`,
  `Replace`, `Substitute`, maske) se prenesejo v API 2 — porabnik (npr. generator vasi) jih
  sestavi **programsko**, brez datoteke.
- Datoteke: formati MCEdit `.schematic`, Sponge `.schem`, Litematica `.litematic` (Baritonovi
  bralniki so brez klientskih razredov) iz mape strežnika `config/npcbaritone/schematics`;
  plus vanilla strukture `.nbt` (`Template`), ki jih Baritone nima.
- **Ne prenese se:** integracija s klientskima modoma Schematica/Litematica
  (`schematica_api/`, `SchematicaHelper`, `LitematicaHelper`), izbira `/sel`
  (`selection/`) — območja poda porabnik prek API-ja; `ExploreProcess` (hodi v nenaložen
  svet, v nasprotju z D-014); Elytra (D-002).
- **Več workerjev na eni shemi** (Baritone tega nima): `IBuildJob` si delijo workerji;
  vsak dobi `MaskSchematic` svojega pasu, bloki se **rezervirajo** (en blok = en worker),
  plasti gredo po vrsti (`buildInLayers`), naslednja plast se začne, ko so rezervacije
  prejšnje v pasu zaprte ali proste. Worker, ki se zatakne, sprosti rezervacije.
- Material: iz inventarja (D-034); manjkajoči predmeti → `onMissingMaterials(seznam)`;
  profil `creativeBuild` (brez materiala) za test in za gameplay porabnika.
- Napredek: `onBlockPlaced`, `onProgress(%)`, `onDone`, `onStuck(pozicija, razlog)`.

**Dokaz.** `BuilderProcess` (1137 vrstic) v 1.2.19 je brez klientskih uvozov, vezan pa je na
igralca (rotacija, hotbar, `processRightClickBlock`) — vse to pokrijeta D-032 in D-034.
Automatone gradnjo dovoli samo igralcem (`BuilderProcess` vr. 310: ne-igralec → prazno).

**Preverba.** M14 T8 (hiše z 1 in 3 workerji).

### D-038 — Meja zmogljivosti za workerje (stopnja W)

**Odločitev.** Poleg D-027 velja za plast 2 in 3: **20 workerjev** (mešano: 10 mine, 5 farm,
5 build) na tečaju T9 doda na glavni niti **p95 ≤ 1 ms/tick** nad isto sceno brez procesov;
skeniranje (D-035) ne teče na glavni niti. Številka je predlog; potrdi ali popravi jo
uporabnik ob začetku M12.

**Dokaz.** Glavna nit pri workerju dela več kot pri hoji: roke (FakePlayer, eventi), inventar,
proces (`onTick` procesa). Meja za navigacijo (D-027) teh stroškov ne zajema.

**Preverba.** M12 A4 (samo mine), M14 A5 (celotna mešanica, 3 ponovitve, mediana in razpon).

---

## Dnevnik odločitev

| ID | Datum | Odločitev | Status |
|---|---|---|---|
| D-001 | 2026-09-24 | Osnova Baritone v1.2.19, Automatone kot vodič | velja |
| D-002 | 2026-09-24 | Elytra in nether-pathfinder ven | velja |
| D-003 | 2026-09-24 | Relokacija v `si.ladja.npcbaritone`, mod ID `npcbaritone` | velja, ime odprto do konca M1 |
| D-004 | 2026-09-24 | LGPL-3.0, NOTICE, source ob izdaji | velja |
| D-005 | 2026-09-24 | Ločena knjižnica, izbirno ozadje, CNPC integracija po novi CNPC odločitvi | velja |
| D-006 | 2026-09-24 | Kontekst nad `EntityLiving` | velja |
| D-007 | 2026-09-24 | Okolje enako CNPC; prevajanje v oblaku dovoljeno | velja |
| D-008 | 2026-09-24 | Brez mixinov/coremoda; šivi navigator/move/jump | velja |
| D-009 | 2026-09-24 | Tick iz `onUpdateNavigation` | velja |
| D-010 | 2026-09-24 | Vhodi in hitrost: vrstni red, način "kot igralec" | velja, preverba M2 |
| D-011 | 2026-09-24 | Obrat telesa, glava ostane vanilla | velja |
| D-012 | 2026-09-24 | Branje samo prek BSI, brez nalaganja chunkov, lint test | velja |
| D-013 | 2026-09-24 | Omejena kopija `id2ChunkMap` | velja, preverba M5 |
| D-014 | 2026-09-24 | Nenaloženo = meja, segmenti | velja |
| D-015 | 2026-09-24 | Brez rušenja/postavljanja; vrata neposredno | delno zamenjana z D-031 (velja za ne-workerje) |
| D-016 | 2026-09-24 | Profili nastavitev na instanco | velja |
| D-017 | 2026-09-24 | Omejen executor, vrsta, deljenje, meritve | velja, številke iz M5 |
| D-018 | 2026-09-24 | `PathNavigate` pogodba pri asinhronosti | velja, preverba M6 |
| D-019 | 2026-09-24 | Velikost 1×2 do M8 | zamenjana z D-028 (privzeto stikalo) |
| D-020 | 2026-09-24 | Obseg ohranjenih funkcij | velja |
| D-021 | 2026-09-24 | Referenčni okvir v kontekstu | velja |
| D-022 | 2026-09-24 | Tri ravni testov | velja |
| D-023 | 2026-09-24 | Stanje poti se ne shranjuje | velja |
| D-024 | 2026-09-24 | Samo strežnik, klient izbiren | velja |
| D-025 | 2026-09-24 | Testi na vanilla mobih | velja |
| D-026 | 2026-09-24 | Prvi commit = nespremenjen upstream | velja |
| D-027 | 2026-09-25 | Dve stopnji meje zmogljivosti (5 ms zdaj, 2 ms pred M7) | velja |
| D-028 | 2026-09-25 | Velikosti: okvir blokov, meje 3 stolpci × 4 bloki, stikalo `largeEntities` | velja, preverba M8 T3 |
| D-029 | 2026-09-26 | Distribucija: knjižnični mod, mehka odvisnost, brez vgrajenih kopij | velja, preverba M7.2/M9.6/M11.10 |
| D-030 | 2026-09-26 | Tri plasti; worker je izrecna registracija; API 2 dodaten | velja |
| D-031 | 2026-09-26 | Rušenje/postavljanje/inventar samo za workerja (delno zamenja D-015) | velja, preverba M11 |
| D-032 | 2026-09-26 | Roke prek `FakePlayer` z lastnikovim profilom, brez mixinov | velja, sonda M11.1 |
| D-033 | 2026-09-26 | Obvezno delovno območje; dovoljenja v ceni in ob izvedbi | velja, preverba M11 |
| D-034 | 2026-09-26 | Porabnikov `IItemHandler` edini vir resnice; dropi v inventar | velja, preverba M11 |
| D-035 | 2026-09-26 | Strežniški skener blokov v bazenu, samo naloženi chunki | velja, preverba M12 |
| D-036 | 2026-09-26 | Proces ima prednost pred AI taski; stanje procesa se ne shranjuje | velja, preverba M11 |
| D-037 | 2026-09-26 | Gradnja: programske sheme, datoteke, več workerjev; kaj se ne prenese | velja, preverba M14 |
| D-038 | 2026-09-26 | Stopnja W: 20 workerjev ≤ 1 ms p95 dodatno (predlog) | potrdi uporabnik ob M12 |
