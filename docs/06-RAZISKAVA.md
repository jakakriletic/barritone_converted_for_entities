# 06 — Raziskava: izmerjena dejstva

Vse številke v tem dokumentu so bile 24. 9. 2026 izmerjene na pripetih revizijah, ne
ocenjene. Odločitve v `02-ODLOCITVE.md` se sklicujejo na te razdelke (§).

Revizije:

| Kaj | Revizija | Opomba |
|---|---|---|
| cabaletta/baritone | tag `v1.2.19` = `d9cb2d9` (2023-08-17) | zadnja izdaja za 1.12.2; `master` = `f2679be` (2023-08-22) se razlikuje samo v `ElytraBehavior.java` |
| Ladysnake/Automatone | `origin/main` = `843b8397` (2021-05-26) | vsebuje celotno zgodovino Baritona + 167 commitov predelave (avtor Pyrofab); novejše veje `1.18`–`1.20`, izdaja `0.11.0` |
| Forge jar za prevod | `ladja_mod/.devsync/forgeBin.jar` (1.12.2, MCP snapshot_20170927) | M0.7 je prevod ponovil proti CNPC okolju (snapshot_20171003) — glej §1a |
| Forge jar od M0.6 | `tools/cache/forgeSrc-1.12.2-14.23.5.2847.jar` (snapshot_20171003, SHA-256 `e01c85cb…692a2d`) | iz uporabnikovega Gradle predpomnilnika; gitignore |
| CustomNPC rework | `github.com/jakakriletic/customNPC_rework` HEAD 24. 9. | uradni `CustomNPCs_1.12.2-(01Oct19).jar` + `src/patch` |

---

## §1 Prevod Baritona v1.2.19 proti našemu Forge 1.12.2

Prevedenih 321 datotek (`api` + `main` + `schematica_api`, brez `launch` mixinov) z
`javac --release 8` proti `forgeBin.jar` + knjižnicam Minecrafta + fastutil:

| | napak |
|---|---:|
| skupaj | **81** |
| v `process/elytra/**` (odpade, D-002) | 43 |
| **drugje** | **38** |

Teh 38 je izključno:

| vzrok | primerov | primer |
|---|---:|---|
| `EnumFacing.getXOffset/getYOffset/getZOffset` (stable_39) → `getFrontOffsetX/Y/Z` | 17 | `MovementParkour`, `MovementAscend`, `MovementTraverse`, `GoalStrictDirection`, `BuilderProcess` |
| `Minecraft.gameDir` → `mcDataDir` (klientsko, odpade s klientom) | 8 | ukazi, `SettingsUtil`, `WorldProvider`, `Baritone` |
| `Vec3d.add(double,double,double)` → `addVector` | 3 | `RotationUtils`, `RayTraceUtils`, `FarmProcess` |
| `World.getChunk(int,int)` / `getChunk(BlockPos)` → `getChunkFromChunkCoords` / `getChunkFromBlockCoords` | 3 | `GameEventHandler`, `BackfillProcess` |
| `ResourceLocation.getPath/getNamespace` → `getResourcePath/getResourceDomain` | 3 | `BlockUtils`, `FindCommand` |
| drobno: `EnumFacing.byHorizontalIndex`→`getHorizontal`, `EntityList.REGISTRY`, en generični `map(...)` v ukazu | 3 | |
| Forge doda `IBlockAccess.isSideSolid(BlockPos, EnumFacing, boolean)` | 1 | `BlockStateInterfaceAccessWrapper` |

Celoten seznam z datoteko in vrstico: `porting/MCP-PREIMENOVANJA.md`.

**Zaključek:** jedro Baritona je API-združljivo z Forge 1.12.2; delo ni prevajanje med
verzijami, ampak predelava igralec → entiteta.

## §1a M0.7: isti prevod proti CNPC mappingu (snapshot_20171003)

Izmerjeno 24. 9. 2026 s `tools/compile_probe.sh` proti
`forgeSrc-1.12.2-14.23.5.2847.jar` iz `~/.gradle/caches/minecraft/.../snapshot/20171003`
(isti jar, ki ga uporablja CNPC okolje) + 41 knjižnic iz `versionJsons/1.12.2.json`
+ jsr305 3.0.1:

| | snapshot_20170927 (§1) | **snapshot_20171003** |
|---|---:|---:|
| skupaj | 81 | **81** |
| elytra | 43 | **43** |
| drugje | 38 | **38** |

Vseh 38 napak izven Elytre je na **istih datotekah in vrsticah** kot v
`porting/MCP-PREIMENOVANJA.md` (primerjava množic `datoteka:vrstica`: enaki). Mappinga se
za Baritonovo uporabo API-ja ne razlikujeta; tabela preimenovanj velja brez sprememb.

Opomba: brez `jsr305` na classpathu je napak 101 (20 × `Nullable`/`Nonnull`); jsr305 je
del Forge userdev odvisnosti, zato ga `tools/cache/libs` vsebuje.

## §2 Velikost in odvisnost od klienta

| izvorni sklop | datotek | vrstic |
|---|---:|---:|
| `src/api` | 157 | 13.978 |
| `src/main` | 153 | 24.780 |
| `src/launch` (mixini) | 27 | 1.938 |

Jedro po sklopih (v1.2.19 → Automatone 1.18 za primerjavo):

| sklop | Baritone 1.12.2 | Automatone |
|---|---|---|
| `pathing/calc` | 8 datotek, 1.153 vrstic | 8, 1.189 |
| `pathing/movement` | 14, 3.848 | 14, 4.267 |
| `pathing/path` | 3, 843 | 3, 845 |
| `process` | 17, 5.586 | 8, 2.742 |
| `cache` | 10, 2.026 | 7, 732 |
| `utils` | 46, 3.995 | 38, 3.386 |
| `command` | 45, 5.396 | 45, 5.035 |
| `behavior` | 6, 1.366 | 5, 1.025 |

Klici konteksta v `src/main` (kar mora predelati D-006):
`ctx.player()` 227, `ctx.playerFeet()` 117, `ctx.world()` 83, `ctx.playerRotations()` 30,
`ctx.playerController()` 23, `ctx.minecraft()` 22, `ctx.playerHead()` 15,
`ctx.getSelectedBlock()` 8, `ctx.worldData()` 7, `ctx.objectMouseOver()` 6.

29 datotek uvaža `net.minecraft.client`; v jedru iskanja poti samo trije
(`CalculationContext`, `MovementDescend`, `MovementDiagonal` — vsi le zaradi tipa
`EntityPlayerSP`).

Port map (`porting/PORT-MAP.md`) razvrsti vseh 348 izvornih datotek (`src/test` s 6 testi je
izvzet in se pregleda v M1):

| akcija | datotek | vrstic |
|---|---:|---:|
| KEEP | 68 | 8.865 |
| ADAPT | 39 | 6.707 |
| REWRITE | 13 | 1.454 |
| CLIENT (debug prikaz, M3) | 4 | 696 |
| LATER (M10) | 7 | 626 |
| DROP | 217 | 22.685 |
| **skupaj** | 348 | 41.033 |

Seam za vhode je že čist: premiki postavljajo samo `Input` stanja
(`MOVE_FORWARD/BACK/LEFT/RIGHT`, `JUMP`, `SNEAK`, `SPRINT`, `CLICK_LEFT/RIGHT`); na
igralca jih preslika samo `InputOverrideHandler` (prek `PlayerMovementInput`).

## §3 Vanilla 1.12.2 mehanika (javap na `forgeBin.jar`)

`EntityLiving`:

```
private final EntityLookHelper lookHelper;      ← ne da se zamenjati (D-011)
protected EntityMoveHelper moveHelper;          ← zamenljiv (D-008)
protected EntityJumpHelper jumpHelper;          ← zamenljiv
protected PathNavigate navigator;               ← zamenljiv
protected PathNavigate createNavigator(World);
protected final void updateEntityActionState(); ← final (tudi CNPC D-018)
public void setAIMoveSpeed(float):  super.setAIMoveSpeed(f); setMoveForward(f);
```

Vrstni red v `updateEntityActionState` (profiler sekcije):
`checkDespawn` → `sensing` → `targetSelector` → `goalSelector` → **`navigation`
(`PathNavigate.onUpdateNavigation`)** → `mob tick` (`updateAITasks`) → [jahanje:
nosilcu `setPath(getPath(), 1.5)` + `moveHelper.read`] → `controls`: **`move`**
(`onUpdateMoveHelper`) → `look` (`onUpdateLook`) → **`jump`** (`doJump` →
`setJumping`).

`EntityMoveHelper.onUpdateMoveHelper`: v stanju `WAIT` pokliče `setMoveForward(0)`; v
`MOVE_TO` sam obrne `rotationYaw`, nastavi `setAIMoveSpeed` in po potrebi
`getJumpHelper().setJumping()`.

`EntityLivingBase.onLivingUpdate`: `jumpTicks`, `isJumping` → `handleJumpWater` /
`jump`; `moveStrafing *= 0.98`, `moveForward *= 0.98`; nato `travel(strafe, vertical,
forward)`. `setSprinting` doda/odstrani `SPRINTING_SPEED_BOOST` na vseh živih
entitetah.

`ChunkProviderServer`: `public final Long2ObjectMap<Chunk> id2ChunkMap`,
`public Chunk getLoadedChunk(int,int)` (ne nalaga), `provideChunk/loadChunk` (nalagata
oziroma generirata).

## §4 Headless sonda (oblak, JUnit-ekvivalent)

```
Bootstrap.register()                      → 2.240 ms
new Chunk(null, 3, -2)                    → OK
ExtendedBlockStorage.set(1,4,1, STONE)    → branje vrne minecraft:stone[variant=stone]
sosednji blok                             → minecraft:air
Blocks.OAK_DOOR.getDefaultState()         → wooden_door[facing=north,half=lower,…,open=false]
```

Iskanje poti je torej mogoče testirati brez zagona igre (D-022).

### §4a M0.9: katere metode sveta rabi iskanje poti

Sonda `IBlockAccessProbeTest` (poročilo `mod/build/reports/npcb/iblockaccess-probe.txt`):
za vsako veljavno stanje vsakega vanilla bloka (254 blokov, 5.485 stanj) postavi blok v
`SyntheticWorld` in pokliče metodo skozi posredniški `IBlockAccess`, ki beleži klice.

Baritonovo iskanje poti vpraša svet prek `IBlockAccess` samo na dveh mestih
(`MovementHelper.canWalkThroughPosition` in `fullyPassablePosition`:
`block.isPassable(bsi.access, pos)`); drugod kliče `isPassable(null, null)`.

| klic | kje teče | metode `IBlockAccess` | bloki |
|---|---|---|---|
| `isPassable(access, pos)` | iskalna nit | **samo `getBlockState`** (1.576 klicev) | 16: vsa vrata, vrata ograje, obe loputi, `snow_layer` |
| `getBoundingBox(access, pos)` | glavna nit (`VecUtils`, `RotationUtils`), pravi svet | `getBlockState` (29 blokov: ograje, zidovi, stekla, vrata, skrinja, redstone) + `getTileEntity` (samo 16 shulker boxov in `piston_extension`) | — |
| `isPassable(null, null)` | iskalna nit (catch-all) | — | vrže NPE za **natanko istih 16 blokov**; vse Baritone obravnava pred catch-all vejo |

Svetloba, biom, redstone moč in `getWorldType` se pri iskanju poti **ne kličejo**. Stubi v
`BlockStateInterfaceAccessWrapper` (tile entity `null`, svetloba 0, biom gozd) so za vanilla
bloke varni; posnetek chunkov (D-013) zadošča. Modded bloki so izven tega dokaza
(ponovi sondo z modpackom, če se pokaže napaka).

**Past, ki jo M1.5 mora upoštevati:** `Chunk.getBlockState(int,int,int)` najprej prebere
`this.world.getWorldType()` (preverba `DEBUG_ALL_BLOCK_STATES`), zato na chunku brez sveta
(`new Chunk(null, x, z)`) vrže NPE. Baritonov `BlockStateInterface.get0` kliče prav to
metodo. Posnetek naj bere neposredno iz `getBlockStorageArray()[y >> 4]` (tudi hitreje)
ali pa kopije chunkov obdržijo referenco na svet. Poleg tega BSI preskoči chunke z
`isLoaded() == false` — sintetični chunki morajo imeti `markLoaded(true)`. Obe dejstvi sta
pripeti kot testa v `HarnessTest`.

## §5 CustomNPC rework

Okolje (`OKOLJE.md`): Minecraft 1.12.2, Forge 14.23.5.2847, Temurin 8u492-b09
(`.tools/jdk8`), Gradle 4.9, ForgeGradle 2.3-SNAPSHOT (2.3.4-gfc67182), MCP
snapshot_20171003. `dev.ps1` nastavi Javo 8 in preveri exit code.

Navigacija danes (`reference-src/noppes/npcs/entity/EntityNPCInterface.java`):
- `extends EntityCreature`
- v `updateTasks()` izbere `moveHelper`/`navigator` glede na `ais.movementType`:
  1 → `FlyingMoveHelper` + `PathNavigateFlying`, 2 → `FlyingMoveHelper` +
  `PathNavigateSwimmer`, sicer `EntityMoveHelper` + `PathNavigateGround`
  (**to je točka integracije v M7**; razred je že v `src/patch`, torej prevedljiv)
- `FOLLOW_RANGE = CustomNpcs.NpcNavRange` (32), `MOVEMENT_SPEED = ais.getWalkingSpeed()/20`
- velikost: `width/height = … / 5 * display.getSize()` (vr. 1086–1087)

Uporaba navigatorja v `reference-src/noppes/npcs/**` (21 AI datotek):

| klic | št. |
|---|---:|
| `clearPath()` | 28 |
| `noPath()` | 21 |
| `tryMoveToXYZ(...)` | 17 |
| `tryMoveToEntityLiving(...)` | 3 |
| `setSpeed(...)` | 2 |
| `setPath(...)` | 2 |
| `getPathToEntityLiving(...)` | 2 |
| `getPath()` | 2 |
| `getPathToPos(...)` | 1 |

Sinhroni `getPathTo*` samo v `ai/EntityAIAttackTarget`, `ai/EntityAIZigZagTarget`,
`entity/data/DataScenes`.

Governance, ki zadeva ta projekt:
- **CNPC D-012**: vanilla pathfinding se ne prepisuje; stopnja D (async iskanje, lasten
  gibalni sklad) je zavrnjena, dokler po M4.10, M4.11 in M5.6 merila M2.7 še padajo;
  glavno tveganje so race conditioni.
- **CNPC M2.7**: šest veličin kakovosti navigacije (delež celih poti, razmerje dolžine,
  čas skupine 8 NPC-jev, razpon na ozkem grlu, µs/iskanje, iskanj/tick) — merilo za M7.
- **CNPC D-016**: ena ponovitev = cel zagon v svežem svetu, tri ponovitve.
- **CNPC D-007**: vsaka sprememba obnašanja pod stikalom, privzeto original.

## §6 Automatone: časovnica predelave

- Razcep od Baritona: `8d2e65c1` "[Port] Port to Fabric 1.16.4" (2021-01-21).
- Predelava: **167 commitov**, Pyrofab, 2021-03-05 → 2021-05-26 (~12 tednov).
- **Prvi mob, ki sledi poti, po 4 dneh in 7 commitih** (`d386d440` → `8adf38cf`):

| commit | kaj | obseg |
|---|---|---|
| `d386d440` | Gut everything that can be | 46 dat., +79 / −2030 |
| `808f2b99` | Remove references to ClientPlayerEntity | 15, +52 / −58 |
| `4dae7a91` | Move path computing to the server | 45, +517 / −825 |
| `e757b3e5` | Let paths compute with regular entities | 36, +248 / −185 |
| `8a97376b` | Rename IPlayerContext to IEntityContext | 50, +208 / −222 |
| `00c370f8` | Make pathfinding properly work with other entities | 7, +17 / −21 |
| `8adf38cf` | Kind of make mobs follow the path | 6, +126 / −36 |

Razvrstitev vseh 167 po naših milestonih: `porting/AUTOMATONE-ROADMAP.md`
(M1 20, M2 14, M3 6, M4 15, M5 2, M6 3, M8 9, LATER 5, SKIP 93).

Automatonov pristop k tiku: `MixinMobEntity` injicira PRE/POST v `tick` in **prekliče
`tickNewAi`**, dokler mob sledi poti. Mi tega ne naredimo (D-008, D-009).

## §7 ladja_mod: ladje kot pravi bloki

`mod/movingworld/.../ship/entity/ShipNavigator.java` (javadoc): bloki ladje so pravi
bloki v pravih, naloženih chunkih shipyarda (`ShipChunkClaim`, polmer 7 chunkov); vsak
zaseden chunk ima dva chunka praznega roba; vanilla pathfinder dela na palubi
nespremenjen, če entiteto za čas iskanja postaviš v ladijske koordinate; pot se hrani v
ladijskem prostoru, v svet se preslika samo trenutna točka prek živega `ShipTransform`
(kvaternion; `shipToWorld`, `worldToShip`, `shipToWorldDirection`). Branje nenaloženega
chunka v shipyardu bi ga **generiralo** ("otok sredi ladje").

## §8 Privzete nastavitve Baritona, ki jih NPC profil spremeni

| nastavitev (vr. v `Settings.java`) | Baritone | NPC profil (D-015/D-016/D-017) |
|---|---|---|
| `allowBreak` (55) | true | **false** |
| `allowSprint` (65) | true | true (profil) |
| `allowPlace` (70) | true | **false** |
| `allowInventory` (75) | false | false |
| `allowParkour` (340) | false | false; true samo v načinu "kot igralec" |
| `allowWaterBucketFall` (136) | true | **false** (ni inventarja) |
| `maxFallHeightNoWater` (524) | 3 | 3 |
| `primaryTimeoutMS` (565) | 500 | **150** (začetno, M5) |
| `failureTimeoutMS` (570) | 2000 | **500** (začetno, M5) |
| `planAheadPrimaryTimeoutMS` (577) | 4000 | **300** (začetno, M5) |
| `planAheadFailureTimeoutMS` (582) | 5000 | **800** (začetno, M5) |
| `chunkCaching` (613) | true | **false** (ni predpomnilnika regij, D-014) |
| `renderPath` (669) | true | false (strežnik) |

## §9 Viri

- [cabaletta/baritone](https://github.com/cabaletta/baritone) — `v1.2.19`
- [Ladysnake/Automatone](https://github.com/Ladysnake/Automatone) — `main`, veje `1.17`–`1.20`
- [jakakriletic/customNPC_rework](https://github.com/jakakriletic/customNPC_rework)
- `ladja_mod` (lokalno): `mod/movingworld`, `.devsync/forgeBin.jar`
