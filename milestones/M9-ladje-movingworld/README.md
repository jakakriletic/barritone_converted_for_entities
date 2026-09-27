# M9 — Ladje (MovingWorld): hoja po palubi v ladijskem prostoru

**Velikost:** L · **Odvisen od:** M6 · **Neobvezno** · **Odločitve:** D-021, D-012

## Cilj

NPC (piratska posadka ali CNPC NPC na ladji) hodi po palubi **pluteče in zavijajoče**
ladje do cilja na ladji; prehod med kopnim in ladjo.

## Zakaj je izvedljivo

`ladja_mod`: bloki ladje so pravi bloki v pravih naloženih chunkih shipyarda; vsak zaseden
chunk ima dva chunka praznega roba; `ShipNavigator` že dela vanilla iskanje v ladijskem
prostoru in hrani pot v ladijskih koordinatah (§7 raziskave). Baritone lahko torej išče
nad shipyard chunki brez sprememb — manjka samo okvir (D-021).

## Naloge

| # | Naloga |
|---|---|
| M9.1 | `ShipFrame implements IMovementFrame`: `worldToFrame(pos)`, `frameToWorld(pos)`, `yawToWorld(yaw)` iz živega `ShipTransform` (kvaternion) |
| M9.2 | `IEntityContext.feetPos()/rotation()` skozi okvir; posnetek chunkov = chunki `ShipChunkClaim` (brez roba izven claima — branje izven bi generiralo, D-012) |
| M9.3 | Izvajanje: vhodi so v prostoru entitete, yaw se pretvori; nošenje po palubi ostane `ShipEntityCollider`/passenger sweep |
| M9.4 | Nagib ladje: Baritone predpostavlja gravitacijo po −Y; izmeri, pri katerem nagibu hoja odpove; nad mejo navigator čaka |
| M9.5 | Prehod kopno ↔ ladja: dvostopenjski cilj (svet → točka ob trupu → okvir ladje), menjava okvirja ob dotiku palube (`movingworld$getTouchedShipId`) |
| M9.6 | Stikalo v `ladja_mod`: `ShipNavigator` (privzeto) ali Baritone za piratsko posadko |
| M9.7 | Test: posadka na testnih ladjah (`testships/galeon`, `fregata`) med plovbo in zavijanjem hodi do topov |
| M9.8 | Če so workerji na ladji (po M11): `IWorkPermission` ladja_moda prepove rušenje ladijskih blokov; delovno območje v ladijskem okvirju (D-021, D-033) |

## Merila sprejema

| # | Merilo |
|---|---|
| A1 | na zasidrani ladji ≥ 9/10 ciljev na palubi |
| A2 | med plovbo in zavijanjem (≥ 10°/s) ≥ 8/10 ciljev, brez padca čez ograjo |
| A3 | 0 generiranih chunkov v shipyardu |
| A4 | prehod kopno → ladja → kopno |
