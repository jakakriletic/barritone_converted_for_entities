# 03 — Faze (milestoni)

Pregled vseh milestonov, odvisnosti in vrat (go/no-go). Podrobnosti vsakega so v
`milestones/<ID>/README.md`. Velikost: **S** ≈ 1–2 seji, **M** ≈ 3–5 sej, **L** ≈ 6+ sej.

Umeritev: Automatone je do prvega moba, ki sledi poti, prišel v 4 dneh in 7 commitih,
celotna predelava pa je trajala ~12 tednov in 167 commitov (§6 raziskave). Mi imamo
manjši obseg (brez gradnje, rudarjenja, fake playerjev, Fabric prehoda) in večjo
previdnost (meritve, stikala).

---

## Kritična pot

```
M0 ──► M1 ──► M2 ──┬──► M3 (vidnost; močno priporočeno pred M4/M5)
                   ├──► M4 ──┐
                   └──► M5 ──┴──► M6 ──┬──► M7 (CustomNPC)
                                       ├──► M8 (velikosti)
                                       └──► M9 (ladje, neobvezno)
M10: po potrebi, po M6
```

Najprej **navpična rezina**: M0–M2 pripeljejo do vanilla zombija, ki z Baritonom prehodi
tečaj. Šele ko to dokazano deluje, se širi (interakcije, zmogljivost, API).

---

## Pregled

| ID | Ime | Vel. | Izhod (bistvo) | Vrata |
|---|---|---|---|---|
| **M0** | Okolje, uvoz, harness | S | prazen mod se naloži na klientu in dedicated strežniku; nespremenjen Baritone uvožen kot en commit; headless JUnit z `Bootstrap` in sintetičnim chunkom teče | — |
| **M1** | Jedro na strežniku | L | A\* v `core` brez `net.minecraft.client`; 12 golden testov; lint testi za D-012 in meje paketov | **V1:** golden testi zeleni ⇒ nadaljuj; če >20 % jedra zahteva prepis ⇒ ponovna presoja D-001 |
| **M2** | Noge | M | vanilla zombi z `/npcb attach … puppet` prehodi tečaj T1 (10 odsekov) v načinu "kot igralec"; hitrost 4,32 ± 0,2 m/s; 0 naloženih chunkov | **V2:** tečaj ≥ 9/10 in hitrost v meji ⇒ nadaljuj; sicer se D-010 revidira pred M4 |
| **M3** | Vidnost | S | `/npcb` ukazi, debug prikaz poti za OP-je, CSV telemetrija; vanilla klient se poveže | — |
| **M4** | Interakcije in varnost | M | vrata/ograje/lopute, lestve, voda, varni padci; tečaj T2; nič porušenega/postavljenega | — |
| **M5** | Zmogljivost | M | omejen executor, vrsta, deljenje; stresni test 50/200 mobov; tabela MSPT in µs/iskanje | **V3:** prispevek glavne niti pri 200 mobih ≤ dogovorjena meja (predlog 2 ms/tick p95) ⇒ nadaljuj |
| **M6** | Adapter in API | M | `BaritonePathNavigate` po D-018; vanilla AI taski (wander, attack, follow owner) delujejo nespremenjeni; API jar | — |
| **M7** | CustomNPC rework | M | nova CNPC odločitev; stikalo na NPC; A/B po M2.7 (3 ponovitve) | **V4:** izboljšava veličin 1–3 brez poslabšanja 5–6 čez razpon ⇒ Baritone ostane izbiren v CNPC |
| **M8** | Velikosti | M | NPC size 1–10 na tečaju T3; port 9 Automatonovih commitov | — |
| **M9** | Ladje | L | NPC hodi po palubi pluteče in zavijajoče ladje do cilja; prehod kopno ↔ ladja | neobvezno |
| **M10** | Kasneje | — | sledenje entiteti, pot do bloka, profil z rušenjem/postavljanjem | po potrebi |

---

## Tečaji (skupni za M2, M4, M8)

Vsi na superflat svetu, prisilno naloženi chunki (CNPC D-011), zgrajeni z
`.mcfunction` (vzorec `ladja_mod/testships`), da so ponovljivi.

| Tečaj | Odseki | Za |
|---|---|---|
| **T1 — osnovno gibanje** | ravnina 30 b; zid z režo; stopnice gor 1/2/3; padec 2/3/4 (4 mora zaviti); diagonala ob stebrih; ozek hodnik 1×2; reža pod ploščo (1,5); obrat 180°; cilj za vogalom; nedosegljiv cilj (mora FAILED, ne tavati) | M2 |
| **T2 — interakcije** | lesena vrata, železna vrata (neprehodna), ograjna vrata, loputa, lestev gor/dol, trta (izklop), voda 1/3/5 globoko, lava (izogni), kaktus (izogni), padec v vodo 10 b | M4 |
| **T3 — velikosti** | T1 + T2 za NPC size 1, 3, 5, 7, 10 | M8 |
| **T4 — stres** | 50 in 200 mobov, naključni cilji v polmeru 64, 10 min; rušenje blokov ob poteh vsakih 5 s | M5 |

Merila so v README posameznega milestona in se zapisujejo strojno berljivo
(vzorec CNPC `meritve-lib.ps1`, shema 1).

---

## Kaj velja za vse milestone

1. Vsak milestone se konča z **gradle buildom na Windowsu in zagonom v igri** pri
   uporabniku; oblačni prevod + JUnit sta pogoj, ne dokaz (D-007).
2. Vsak milestone ima **dedicated server smoke** (strežnik se zažene, mod naloži, entiteta
   hodi).
3. Vsaka sprememba obnašanja pri porabniku (CNPC, ladja) je pod stikalom, privzeto
   staro obnašanje.
4. `docs/04-STANJE.md` dobi zapis ob začetku in koncu milestona.
5. Nova odločitev → `docs/02-ODLOCITVE.md`, nikoli tiho.
