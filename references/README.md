# Reference

Nespremenjene, pripete kopije. Iz njih se ne gradi in v njih se ne razvija. Oba sta git
submodula v detached HEAD stanju.

| Mapa | Repozitorij | Commit | Zakaj |
|---|---|---|---|
| `baritone-1.12.2/` | [cabaletta/baritone](https://github.com/cabaletta/baritone) | `d9cb2d9` = tag `v1.2.19` (2023-08-17) | **osnova** (D-001): M0.4 jo uvozi v `mod/src/upstream` |
| `automatone/` | [Ladysnake/Automatone](https://github.com/Ladysnake/Automatone) | `843b8397` (2021-05-26, `main`) | **vodič** (D-001): celotna zgodovina Baritona + 167 commitov predelave za entitete |

## Uporaba

```powershell
git submodule update --init

# Kaj je Automatone naredil v enem koraku (glej docs/porting/AUTOMATONE-ROADMAP.md)
git -C references/automatone show 8adf38cf

# Vsi Automatonovi commiti predelave
git -C references/automatone log --reverse --author=Pyrofab --oneline

# Ista datoteka v Baritonu 1.12.2 in v Automatonu
git -C references/baritone-1.12.2 show HEAD:src/main/java/baritone/utils/InputOverrideHandler.java
git -C references/automatone show HEAD:src/main/java/baritone/utils/InputOverrideHandler.java
```

## Drugi viri (niso submoduli)

| Kaj | Kje | Zakaj |
|---|---|---|
| CustomNPC rework | `github.com/jakakriletic/customNPC_rework` | porabnik (M7); okolje (D-007); merila M2.7 |
| ladja_mod | `C:\Users\jakak\Desktop\ladja_mod` | `ShipNavigator`, shipyard (M9); (od M0.6 oblačni prevod uporablja `tools/cache/forgeSrc-*.jar` iz Gradle predpomnilnika) |
| Novejše veje Automatona (`1.18`–`1.20`, izdaja `0.11.0`) | isti repozitorij, `git fetch` | popravki, ki jih 1.12 port morda rabi (R-12) |
