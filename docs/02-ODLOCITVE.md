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

### D-043 — Vrata V4: veličina 1 »ni slabše«, najslabši zagon, 5 ponovitev

*28. 9. 2026. Številka je izbrana nad D-042; D-029–D-042 so zapisane samo na domačem
računalniku (glej `REKONSTRUKCIJA-2026-09-28.md`).*

**Kontekst.** M7.8 (CNPC `docs/meritve/2026-09-28-M7.8-zakljucek.md`): vanilla ima na merilu 1
že 8/8 celih poti, zato zahteva A3 »boljše čez razpon« za veličino 1 ni izpolnljiva ne glede na
ozadje. Vanilla je deterministična (46 od 59 veličin ima razpon 0), Baritone ni: grlo B je dal
8, 8 in 4 od 8. Pri treh ponovitvah en slab zagon določi ves razpon, redkega slabega zagona pa
tri ponovitve ne ujamejo zanesljivo.

**Odločitev.** A3 se bere tako:
1. veličina 1 (delež celih poti): Baritone **ni slabši** od vanille v nobenem zagonu;
2. veličini 2 in 3: Baritonova **mediana je boljša** od vanille **in** njegov **najslabši** zagon
   ni slabši od najslabšega vanilla zagona;
3. Baritone najmanj **5 ponovitev** (vanilla ostane pri 3, ker je njen razpon 0).

A4 ostane nespremenjen, šteje pa šele, ko obe ozadji merita isti dogodek (vanilla sonda meri
celoten sinhroni `findPath`, knjižnica samo posnetek na glavni niti). Ko bo obstajal
determinističen način (M7.9 korak 2), se doda **A6**: v tem načinu imajo vedenjske veličine
(prispelo, prvi/mediana/zadnji, razpon) razpon 0.

**Posledica.** M7.8 tudi po tej definiciji ne prestane (grlo B najslabši 4/8 < 6/8). Odločitev
ne spremeni izida, ampak odstrani merilo, ki ga nobeno ozadje ne bi moglo izpolniti, in naredi
redke slabe zagone vidne. Potrdil uporabnik 28. 9. (predlog »popravek vrat V4«).

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
| D-015 | 2026-09-24 | Brez rušenja/postavljanja; vrata neposredno | velja |
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
| D-043 | 2026-09-28 | Vrata V4: veličina 1 »ni slabše«, najslabši zagon, Baritone ≥ 5 ponovitev (D-029–D-042 na domačem računalniku) | velja; V4 po njej sprejet 28. 9. (M7.10c) |
