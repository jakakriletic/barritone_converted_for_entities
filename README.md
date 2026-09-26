# NPC Baritone — Baritone za NPC-je (Minecraft 1.12.2)

Strežniška knjižnica, ki **Baritonov iskalnik in izvajalec poti prenese z igralca na
NPC-je** (`EntityLiving`). Prvi porabnik je [CustomNPC rework](https://github.com/jakakriletic/customNPC_rework),
drugi (neobvezno) piratske posadke v `ladja_mod`.

Ta README je **vstopna točka**. Vsaka seja začne tukaj, nato prebere `docs/04-STANJE.md`.

---

## Hitri status

| | |
|---|---|
| Datum zadnje posodobitve | 2026-09-26 |
| Trenutna faza | **M8 zaključen**, **vse ročne preverbe v klientu zaprte** (`/npcb selftest` 6/6 v dev klientu, Forge profilu s pravim Baritonom in vanilla klientu). M7 čaka stopnjo A zmogljivosti (D-027) |
| Naslednji korak | stopnja A zmogljivosti (pogoj za M7): profil glavne niti pri 200 mobih (`.\t4-run.ps1 -Mobs 200`); glej [`docs/04-STANJE.md`](docs/04-STANJE.md) |
| Osnova | Baritone **v1.2.19** (`d9cb2d9`, zadnja izdaja za 1.12.2), pripet v `references/baritone-1.12.2` |
| Konceptualni vodič | Automatone (`843b8397`), pripet v `references/automatone`; 167 commitov predelave Baritona za entitete je razvrščenih po milestonih |
| Licenca | LGPL-3.0 (podedovano od Baritona), glej `docs/02-ODLOCITVE.md` D-004 |

---

## Kaj projekt je in česa ni

**Je:** Baritonov A\*, cene premikov in izvajalec poti, predelani tako, da tečejo na
strežniku in premikajo navadno entiteto namesto igralca. Integracija prek zamenjave
`PathNavigate` + `EntityMoveHelper`, zato obstoječi AI taski (sledenje, napad, tavanje)
delujejo nespremenjeni.

**Ni:** bot, ki rudari, gradi ali farma; fake player; klientski mod; zamenjava
za vanilla navigacijo pri vseh NPC-jih. Baritone je **izbirno ozadje** za NPC-je, ki
morajo priti daleč, čez zahteven teren ali zanesljivo (D-005).

---

## Kazalo dokumentacije

| Dokument | Kaj vsebuje | Kdaj ga bereš |
|---|---|---|
| **README.md** (ta datoteka) | pregled, status, kazalo | vedno prvo |
| [`docs/04-STANJE.md`](docs/04-STANJE.md) | **živ dnevnik** — kaj je narejeno, kaj teče, kaj je blokirano | vedno drugo |
| [`docs/01-ARHITEKTURA.md`](docs/01-ARHITEKTURA.md) | zgradba knjižnice, tok enega ticka, meje modulov | preden pišeš kodo |
| [`docs/02-ODLOCITVE.md`](docs/02-ODLOCITVE.md) | **28 težkih vprašanj, vsako z odločitvijo, dokazom in preverbo** | preden karkoli spremeniš |
| [`docs/03-FAZE.md`](docs/03-FAZE.md) | milestoni M0–M10, odvisnosti, izhodni kriteriji | ko načrtuješ sejo |
| [`docs/05-SEJA-PROTOKOL.md`](docs/05-SEJA-PROTOKOL.md) | kako seja začne, dela, preverja in zaključi | vedno, tudi na koncu seje |
| [`docs/06-RAZISKAVA.md`](docs/06-RAZISKAVA.md) | izmerjena dejstva: prevod, velikosti, Automatone časovnica, CNPC API | ko dvomiš v odločitev |
| [`docs/07-TVEGANJA.md`](docs/07-TVEGANJA.md) | register tveganj z blažitvami in sprožilci | na začetku vsakega milestona |
| [`docs/porting/PORT-MAP.md`](docs/porting/PORT-MAP.md) | **vseh 348 izvornih datotek Baritona**: KEEP / ADAPT / REWRITE / DROP z razlogom | M1 |
| [`docs/porting/AUTOMATONE-ROADMAP.md`](docs/porting/AUTOMATONE-ROADMAP.md) | 167 commitov Automatona, razvrščenih po naših milestonih | ko se lotiš milestona |
| [`docs/porting/MCP-PREIMENOVANJA.md`](docs/porting/MCP-PREIMENOVANJA.md) | 38 napak prevoda Baritona proti našemu Forge jarju | M1 |
| [`milestones/`](milestones/) | en README na milestone: cilj, naloge, merila sprejema | ko delaš na milestonu |
| [`references/README.md`](references/README.md) | pripete reference in kako jih uporabljati | M0, M1 |

---

## Milestoni na kratko

| ID | Ime | Velikost | Odvisen od |
|---|---|---|---|
| **M0** | Okolje, uvoz Baritona, headless test harness | S | — |
| **M1** | Jedro na strežniku: iskanje poti brez igralca, golden testi | L | M0 |
| **M2** | Noge: izvajanje poti na vanilla mobu | M | M1 |
| **M3** | Vidnost: ukazi `/npcb`, debug prikaz poti, telemetrija | S | M2 |
| **M4** | Interakcije in varnost: vrata, lestve, voda, padci, brez rušenja | M | M2 |
| **M5** | Strežniška zmogljivost: omejen executor, vrsta, deljenje poti, meritve | M | M2 |
| **M6** | `PathNavigate` adapter in javni API | M | M4, M5 |
| **M7** | Integracija v CustomNPC rework (A/B po meritvah M2.7) | M | M6 |
| **M8** | Velikosti entitet (port Automatonovih dimenzij) | M | M6 |
| **M9** | Ladje (MovingWorld): hoja po palubi v ladijskem prostoru | L | M6 (neobvezno) |
| **M10** | Kasneje: sledenje, pot do bloka, rušenje/gradnja po profilu | — | po potrebi |

Podrobno: [`docs/03-FAZE.md`](docs/03-FAZE.md).

---

## Struktura mape

```
barittone_for_npc_rework/
├── README.md, AGENTS.md, LICENSE, NOTICE.md
├── docs/                    načrt, odločitve, raziskava, stanje, protokol
│   └── porting/             port map, Automatone roadmap, MCP preimenovanja
├── milestones/M0 … M10/     en README na milestone
├── references/              pripete reference (git submoduli, detached HEAD)
│   ├── baritone-1.12.2/     cabaletta/baritone @ v1.2.19 (d9cb2d9)
│   └── automatone/          Ladysnake/Automatone @ 843b8397
├── mod/                     Gradle projekt knjižnice (FG 2.3, Forge 14.23.5.2847)
│   ├── src/main/java        si.ladja.npcbaritone.core (port Baritona) + forge
│   └── src/test/java        headless JUnit (SyntheticWorld, golden testi, lint)
├── dev.ps1, prepare-assets.ps1, smoke-server.ps1
└── tools/                   cloud-compile.sh, compile_probe.sh, check-upstream-import.sh, portmap_cut.py, mark_modified.py
```

Reference se ne spreminjajo in se iz njih ne gradi.

---

## Za novo seje: minimalni kontekst

1. Ta README → `docs/04-STANJE.md` → README trenutnega milestona.
2. Odločitve v `docs/02-ODLOCITVE.md` veljajo, dokler jih nova odločitev izrecno ne zamenja.
3. Protokol `docs/05-SEJA-PROTOKOL.md`: prevajanje v oblaku je dovoljeno in zaželeno,
   "narejeno" pa pomeni zagon v igri pri uporabniku.
