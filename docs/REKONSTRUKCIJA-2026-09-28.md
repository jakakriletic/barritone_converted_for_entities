# Veja `m7-cnpc-oblak` — rekonstrukcija kode iz oblaka (28. 9. 2026)

**Zakaj ta veja obstaja.** Seje 2026-09-27 (3)–(6) so delo commitale lokalno na veji `m7-cnpc`
na domačem računalniku (`desktop-sn494c0`), nič ni bilo pushano. Uporabnik do tega računalnika
trenutno nima dostopa. Ta veja vsebuje **kodo** v stanju zadnjega lokalnega commita `79da023`,
rekonstruirano iz oblačne kopije seje (vse `mod/src` datoteke so bile pred tem preverjene z
md5 proti računalniku), na vrhu `origin/m7-cnpc` (`c098a64`).

**Ni tukaj** (samo na domačem računalniku): lokalna zgodovina 15 commitov, dokumentacija
(`02-ODLOCITVE.md` D-029–D-042, `03-FAZE.md`, `04-STANJE.md` zapisi 2026-09-27 (3)–(6),
`07-TVEGANJA.md` R-16–R-23, milestoni M11–M15, `PORT-MAP.md`, `AUTOMATONE-ROADMAP.md`) in meritve
(`docs/meritve/m4/t2-20260927-*.csv`, `docs/meritve/klient/selftest-20260927-*.csv`).

**Ko je domači računalnik spet dosegljiv:** tam `git push origin m7-cnpc` (prava zgodovina z
dokumentacijo); ta veja je nato odveč — koda je enaka — in se lahko izbriše
(`git push origin --delete m7-cnpc-oblak`). Na tej veji ne nadaljuj dela, ki ga ne želiš ročno
prenesti.

## Kaj je v kodi (glede na `c098a64`)

| Odločitev | Vsebina |
|---|---|
| D-039 | navigacijski **API 2**: `INpcNavigator.reinstall()`, `speedMode/setSpeedMode`, `doorMode/setDoorMode`, enuma `SpeedMode`, `DoorMode`; `API_VERSION = 2`, manifest 2; vrata na instanco (`npcOpenDoors`, `npcOpenIronDoors`); `Attach.reinstall` po CNPC `updateTasks()`; `InstanceOverrides` (config → profil → instanca) |
| D-040 | `npcWaterPenalty`; profil `avoid_water` = 8 × hoja (vanilla `PathNodeType.WATER` = 8) |
| D-041 | vanilla zahteve prek `PathNavigate` vidijo entiteta ± (`FOLLOW_RANGE` + 8), dlje segmenti; stikalo `npcRespectFollowRange` |
| D-042 | način `own`: atribut × hitrost zahteve, brez sprinta |
| — | selftest korak "debug off" z rokom 110 tickov |

**Preverjeno:** JUnit 133/133 v oblaku na tem drevesu. Na Windowsu (27. 9., pred D-042): build,
T2 10/10, `/npcb selftest` 6/6. D-042 na Windowsu zgrajen (jar 19:26), v igri še ne preverjen.
