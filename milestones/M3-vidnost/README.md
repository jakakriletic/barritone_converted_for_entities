# M3 — Vidnost: ukazi, debug prikaz poti, telemetrija

**Velikost:** S · **Odvisen od:** M2 · **Odločitve:** D-024

## Cilj

Vse, kar NPC z Baritonom misli, se da **videti** (pot v svetu) in **prebrati** (CSV), brez
tega pa vanilla klient še vedno normalno igra. Zaradi tega je M3 pred M4/M5: vsak
naslednji problem se razišče hitreje.

## Naloge

| # | Naloga | Automatone vzorec |
|---|---|---|
| M3.1 | `/npcb`: `attach [puppet] [profil]`, `detach`, `goto <x y z / @e>`, `stop`, `status [@e]`, `profile <ime>`, `debug on/off`, `speedtest`, `trace on/off/dump` | `c212980e`, `a893582a` |
| M3.2 | Paket poti (strežnik → klient z modom, samo OP-jem z vklopljenim debugom, največ 4 Hz na entiteto) | `2ebfbf0f`, `635f8aaf`, `32bf8032` |
| M3.3 | Klientski izris: pot kot črta + točke, trenutni premik poudarjen, cilj; prirejeno iz `PathRenderer` (CLIENT v port mapu) | `2bce8f85` |
| M3.4 | Telemetrija CSV (vzorec `FleetTrace` iz `ladja_mod`): tick, entiteta, stanje, pozicija, cilj, dolžina poti, premik, µs iskanja, ponovna iskanja, razlog neuspeha | — |
| M3.5 | Vanilla klient (brez moda) se poveže na strežnik z modom; NPC-ji hodijo | — |

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | OP klient z modom vidi pot zombija v T1 |
| A2 | vanilla klient se poveže in vidi zombija hoditi (D-024) |
| A3 | `trace dump` zapiše CSV z vsemi stolpci; en tek T1 = ena datoteka |
| A4 | debug paketi se ne pošiljajo, ko je debug izklopljen (števec 0) |
