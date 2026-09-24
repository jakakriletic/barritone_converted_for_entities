# M4 — Interakcije in varnost

**Velikost:** M · **Odvisen od:** M2 (priporočeno M3) · **Odločitve:** D-015, D-016

## Cilj

NPC gre skozi vrata, po lestvah, čez vodo in se varno spusti — ne da bi kadarkoli porušil
ali postavil blok.

## Naloge

| # | Naloga | Automatone vzorec |
|---|---|---|
| M4.1 | `EntityInteractions` (namesto `IPlayerController`): odpri/zapri lesena vrata, ograjna vrata, lopute neposredno (`BlockDoor.toggleDoor` …); železna vrata so neprehodna | `3216de48`, `b1899f30` |
| M4.2 | `MovementTraverse`: vrata brez desnega klika igralca (vhod `CLICK_RIGHT` → `EntityInteractions`) | `3216de48` |
| M4.3 | Lestve gor/dol (`MovementPillar`/`Downward`), trte izklopljene v profilu | `d5ff55ce` |
| M4.4 | Voda: cene, plavanje, sprint plavanje, padec v vodo | `77aadb8a`, `64c680a9`, `7f6f8034`, `1fb7ad15`, `885027f1`, `456aa6bf`, `8c12848f` |
| M4.5 | Kisik: cena poti pod vodo iz `getAir()`; brez utopitve | `46a3b4d1`, `9b018aaa`, `51694ee9` |
| M4.6 | Nevarnosti: lava, ogenj, kaktus, magma — Baritone jih že izogiba; test, da ostane tako | — |
| M4.7 | Izogibanje mobom (`Avoidance`) iz strežniških entitet, izklopljeno privzeto | `7dd013f0` |
| M4.8 | Tečaj **T2** + `t2-run.ps1` | — |
| M4.9 | Varnostni test: med T1+T2 števec sprememb blokov, ki jih povzroči entiteta, razen vrat = 0 | D-015 |

## Merila sprejema

| # | Merilo | Meja |
|---|---|---|
| A1 | T2 odsekov doseženih | ≥ 9/10; železna vrata = obhod ali FAILED |
| A2 | porušenih/postavljenih blokov | 0 |
| A3 | škode od padca ali utopitve v T1+T2 | 0 |
| A4 | vrata za NPC-jem | zaprta (kot jih je našel) |
