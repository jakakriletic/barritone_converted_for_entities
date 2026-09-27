# 03 — Faze (milestoni)

Pregled vseh milestonov, odvisnosti in vrat (go/no-go). Podrobnosti vsakega so v
`milestones/<ID>/README.md`. Velikost: **S** ≈ 1–2 seji, **M** ≈ 3–5 sej, **L** ≈ 6+ sej.

Umeritev: Automatone je do prvega moba, ki sledi poti, prišel v 4 dneh in 7 commitih,
celotna predelava pa je trajala ~12 tednov in 167 commitov (§6 raziskave). M0–M8 so
imeli manjši obseg (brez gradnje, rudarjenja, fake playerjev, Fabric prehoda). Od
2026-09-26 so worker plasti (M11–M15, D-030) načrtovane; tu Automatone ni vzorec za mobe
(rušenje in gradnjo ima samo za igralce, glej razdelek H v `02-ODLOCITVE.md`), zato je to
novo delo in je ocenjeno večje.

---

## Kritična pot

```
M0 ──► M1 ──► M2 ──┬──► M3 (vidnost; močno priporočeno pred M4/M5)
                   ├──► M4 ──┐
                   └──► M5 ──┴──► M6 ──┬──► M7 (CustomNPC)          ← po stopnji A
                                       ├──► M8 (velikosti) ✔
                                       └──► M9 (ladje, neobvezno)   ← po M11, če ima ladja workerje

Faza 0 (odprto iz M5–M8): stopnja A (D-027) → M5 A4 (1 h stres) → merge vej v main
                              │
                              ▼
                             M11 worker osnova ──┬──► M12 rudarjenje ──┬──► M13 farmanje
                                                 │                     └──► M14 gradnja (+ M12.4)
                                                 └──► M15 sledenje
M10: zaloga idej
```

**Vrstni red dela (priporočen):** faza 0 → M11 → M12 → M14 → M13 → M15. M14 je pred M13,
ker je za porabnika (generator vasi, ladja_mod) pomembnejša in večja, M13 pa skoraj v celoti
sloni na M12. M7 lahko teče vzporedno kadarkoli po stopnji A; M9 po M11, če naj workerji
delajo na ladji (ladijske bloke varuje porabnikov `IWorkPermission`).

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
| **M10** | Kasneje | — | zaloga idej (vogali širokih entitet, lestev pri ozkih, veliki workerji, flow field) | po potrebi |
| **M11** | Worker osnova | L | registriran worker hodi z rušenjem/postavljanjem v območju, orodja in bloki iz porabnikovega inventarja; T5; invariante (območje, ohranitev predmetov) | **V5:** A1–A5 ⇒ M12/M14/M15; A2 ali A4 pade ⇒ nič naprej, dokler ni popravljeno |
| **M12** | Rudarjenje | M | `mine`, `getToBlock`, odlaganje, backfill; strežniški skener; T6 | številka stopnje W potrjena |
| **M13** | Farmanje | S–M | žetev, setev, kostna moka, oranje; T7 | — |
| **M14** | Gradnja | L–XL | programske in datotečne sheme, material, en ali več workerjev; T8 hiše, T9 stres | **V6:** A5 (stopnja W) ⇒ primerno za porabnika z 20 workerji; sicer meja števila workerjev v configu |
| **M15** | Sledenje | S | `FollowProcess` kot proces s prednostjo | — |

---

## Tečaji

Vsi na superflat svetu, prisilno naloženi chunki (CNPC D-011), zgrajeni z
`.mcfunction` (vzorec `ladja_mod/testships`), da so ponovljivi.

| Tečaj | Odseki | Za |
|---|---|---|
| **T1 — osnovno gibanje** | ravnina 30 b; zid z režo; stopnice gor 1/2/3; padec 2/3/4 (4 mora zaviti); diagonala ob stebrih; ozek hodnik 1×2; reža pod ploščo (1,5); obrat 180°; cilj za vogalom; nedosegljiv cilj (mora FAILED, ne tavati) | M2 |
| **T2 — interakcije** | lesena vrata, železna vrata (neprehodna), ograjna vrata, loputa, lestev gor/dol, trta (izklop), voda 1/3/5 globoko, lava (izogni), kaktus (izogni), padec v vodo 10 b | M4 |
| **T3 — velikosti** | T1 + T2 za NPC size 1, 3, 5, 7, 10 | M8 |
| **T4 — stres** | 50 in 200 mobov, naključni cilji v polmeru 64, 10 min; rušenje blokov ob poteh vsakih 5 s | M5 |
| **T5 — worker osnove** | tunel z 3 orodji, steber, most, spust, gramoz, zaščiten steber, `TileEntity` v poti, cilj izven območja, prazen inventar, AI wander | M11 |
| **T6 — kamnolom** | `mine` kamen/premog, lava žep, obraba orodja, backfill, 10 workerjev | M12 |
| **T7 — kmetija** | vsi pridelki, setev, oranje, 2 cikla rasti | M13 |
| **T8 — hiše** | H1–H5: kocka, lesena hiša (orientacija, oporni bloki), neraven teren, manjkajoč material, 3 workerji | M14 |
| **T9 — stres workerjev** | 20 workerjev (10 mine, 5 farm, 5 build), 10 min | M14 (stopnja W) |

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
6. **Testna politika za M11–M15** (brez nepotrebnih testov, brez bližnjic):
   - **JUnit samo za logiko, ki lahko tiho odpove in se da preveriti brez sveta:** cena
     rušenja z orodji, obvoz zaščitenega bloka (golden), ohranitev predmetov z lažnim
     `IItemHandler`, skener na `SyntheticWorld`, sheme in bralniki formatov, rezervacije
     `IBuildJob`. Kar je vidno samo v igri (orientacija, razpoke, AI), se v JUnit ne ponareja.
   - **En tečaj na milestone**, en tek na dedicated strežniku za sprejem. **Tri ponovitve
     samo, kjer je merilo meritev z mejo** (M12 A4, M14 A5).
   - **Invariante preveri runner pri vsakem teku** (spremembe blokov samo v območju,
     ohranitev predmetov, 0 izjem) — niso ločeni testi in ne morejo biti pozabljeni.
   - **Regresija T1 + T2 samo, ko se spremeni deljena koda premikov** (M11 A5), ne pri vsakem
     milestonu. Selftest v klientu samo za stvari, ki jih vidi klient (M11 A7, M15 A1).
   - Vsak nov JUnit najprej pade (protokol §2).
