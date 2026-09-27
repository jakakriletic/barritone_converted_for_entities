# Automatone roadmap: 167 commitov predelave, razvrščenih po naših milestonih

Generirano s `tools/automatone_roadmap.py` iz `references/automatone` @ `843b8397` (avtor Pyrofab, 2021-03-05 → 2021-05-26). Commit je **vzorec**, ne vir za kopiranje (D-001): pred delom na isti datoteki ga preberi z `git -C references/automatone show <hash>`.

SKIP pomeni: Fabric/Yarn, Cardinal Components, Brigadier ukazi, fake player, izbira /sel, posebnosti 1.14+ (scaffolding, tagi, višina sveta), build in changelog.

| za nas | commitov |
|---|---:|
| M1 | 20 |
| M2 | 14 |
| M3 | 6 |
| M4 | 14 |
| M5 | 2 |
| M6 | 3 |
| M8 | 9 |
| M11 | 8 |
| M12 | 4 |
| M14 | 1 |
| SKIP | 86 |

| commit | datum | sporočilo | za nas | opomba |
|---|---|---|---|---|
| `d386d440` | 2021-03-05 | Gut everything that can be | **M1** | izhodišče: izrezano vse, kar ni jedro (−2030 vrstic) |
| `808f2b99` | 2021-03-05 | Remove references to ClientPlayerEntity | **M1** | odstranitev klientskega igralca iz jedra |
| `4dae7a91` | 2021-03-06 | Move path computing to the server | **M1** | računanje poti na strežniku |
| `e757b3e5` | 2021-03-08 | Let paths compute with regular entities | **M1** | poti za navadne entitete |
| `8a97376b` | 2021-03-08 | Rename IPlayerContext to IEntityContext | **M1** | IPlayerContext → IEntityContext |
| `00c370f8` | 2021-03-08 | Make pathfinding properly work with other entities | **M1** | iskanje poti z drugimi entitetami |
| `8adf38cf` | 2021-03-08 | Kind of make mobs follow the path | **M2** | mobi sledijo poti (MixinMobEntity; pri nas navigator, D-008/D-009) |
| `064c4857` | 2021-03-08 | Fix isPathing check | **M2** | isPathing |
| `574e0897` | 2021-03-08 | Re-add player compatibility | **SKIP** | ni relevantno za 1.12 NPC noge |
| `f1a2d467` | 2021-03-08 | Fix sprinting | **M2** | sprint |
| `c212980e` | 2021-03-08 | Execute commands on the server thread | **M3** | ukazi v strežniški niti |
| `fb073c81` | 2021-03-08 | Fix crash when falling into the void | **M1** | padec v praznino |
| `cebc28f8` | 2021-03-09 | It's Automatone now | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `891b9e10` | 2021-03-09 | CCA time | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `d3828043` | 2021-03-10 | Fix chained block breaking | **M11** | zaporedno rušenje |
| `2ba848ca` | 2021-03-10 | Un-harcode eye height | **M1** | višina oči iz entitete |
| `52935625` | 2021-03-10 | Add a todo | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `c3cafa8a` | 2021-03-10 | Actually it's baritone again | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `e627516c` | 2021-03-10 | Merge remote-tracking branch 'orig/master' into automatone | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `38f5ff6b` | 2021-03-10 | Fix merge issues | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `c32eb014` | 2021-03-10 | Un-hardcode nether check | **M1** | nether preverba iz sveta |
| `3786cd0b` | 2021-03-10 | Un-hardcode world height | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `5789a51e` | 2021-03-11 | Fix invoker mod incompatibility | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `324bd259` | 2021-03-12 | Un-hardcode player dimensions in MovementTraverse | **M8** | dimenzije v MovementTraverse |
| `943ae1be` | 2021-03-12 | Fix block breaking pos in MovementTraverse | **M8** | pozicija rušenja v Traverse |
| `3a08d43e` | 2021-03-12 | Make MovementPillar 50% smarter | **M8** | pametnejši MovementPillar |
| `a7cd7e71` | 2021-03-13 | Copy Requiem's WIP fake player | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `95cbf4f5` | 2021-03-14 | Fix buildscript failing on java 11+ | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `ab0fca0e` | 2021-03-14 | Update gradle wrapper | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `f585c6f7` | 2021-03-14 | Replace schematic registry with minecraft's impl | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `618e528f` | 2021-03-15 | Use brigadier to execute baritone commands | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `9fc118e3` | 2021-03-15 | Remove some now useless mixins | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `a893582a` | 2021-03-15 | Require OP to run automatone commands | **M3** | ukazi samo za OP |
| `530b3803` | 2021-03-15 | Remove client baritone instance | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `0f8a664b` | 2021-03-15 | Make IPlayerController a component | **M11** | IPlayerController kot komponenta; ne-igralci dobijo Dummy (pri nas roke, D-032) |
| `2b691f8c` | 2021-03-15 | Fix broken references to IEntityRenderManager | **SKIP** | ni relevantno za 1.12 NPC noge |
| `446120d8` | 2021-03-15 | Give default step height to fake players | **M2** | privzeta višina koraka |
| `31374839` | 2021-03-15 | Un-hardcode soulsand movement speed | **M2** | hitrost na soul sandu |
| `55152759` | 2021-03-15 | Make selection rendering slightly more sane | **SKIP** | izbira (gradnja) |
| `8a322d68` | 2021-03-15 | Fix buildscript plugins | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `d5b932df` | 2021-03-15 | Refactor the selection system | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `bcc7f0d3` | 2021-03-15 | Change the logger prefix to "Automatone" | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `930085b5` | 2021-03-15 | Fix entities still sprinting at the end of path | **M2** | sprint na koncu poti |
| `6ddd4b14` | 2021-03-15 | Use a BitSet in InputOverrideHandler | **M2** | BitSet v InputOverrideHandler |
| `35becc89` | 2021-03-15 | Copy requiem's chunkloading cancellation mixins | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `7854b05e` | 2021-03-15 | Finish moving stuff out of BaritoneProvider | **SKIP** | ni relevantno za 1.12 NPC noge |
| `c3874459` | 2021-03-15 | Make commands throw brigadier exceptions | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `d65e13c6` | 2021-03-15 | Improve side safety | **M1** | varnost strani (klient/strežnik) |
| `fe537239` | 2021-03-15 | Fix command exception spaghetti | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `d9ce76fa` | 2021-03-15 | Properly register world data provider component | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `d7be8404` | 2021-03-15 | Alea Jacta Est | **SKIP** | ni relevantno za 1.12 NPC noge |
| `8478669e` | 2021-03-16 | Fix /click crashing dedicated servers | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `e3d476d5` | 2021-03-16 | Properly shutdown the executor | **M5** | ugašanje executorja |
| `e1a0ca4f` | 2021-03-16 | Fix exception when clearing keys in constructor | **SKIP** | ni relevantno za 1.12 NPC noge |
| `cee22661` | 2021-03-16 | Reference Automatone in IO operations | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `548f9f25` | 2021-03-16 | Fix mob AI cancellation crash | **M2** | sesutje ob preklicu AI moba |
| `2ebfbf0f` | 2021-03-16 | Attempt to sync paths for render | **M3** | sinhronizacija poti za izris |
| `37c9fc2d` | 2021-03-16 | Register automatone's argument type | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `6c8ae019` | 2021-03-16 | Replace stack lookups with tags | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `635f8aaf` | 2021-03-16 | Fix path sync packet writing | **M3** | zapis paketa poti |
| `ebc87346` | 2021-03-16 | Remove derelict PlayerMovementInput class | **M2** | odstranjen PlayerMovementInput |
| `4bb57893` | 2021-03-16 | Remove settings from command suggestions | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `df9a13fe` | 2021-03-16 | Implement protection detection | **M11** | zaščita v CalculationContext (samo igralci; pri nas D-033) |
| `47d5461f` | 2021-03-16 | Make chat logging work on servers | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `b98c95d5` | 2021-03-17 | Yeet baritone references in strings | **SKIP** | ni relevantno za 1.12 NPC noge |
| `11a93648` | 2021-03-17 | Make /version refer to mod metadata | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `1ed02cd6` | 2021-03-17 | Let gradle replace the version string | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `744a7aff` | 2021-03-17 | Make /come work as intended | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `05c0e051` | 2021-03-17 | Re-implement /click to work on dedicated servers | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `678ecb02` | 2021-03-17 | Make block drop lookups actually sensible | **M12** | dropi blokov (BlockOptionalMeta) za MineProcess |
| `76ffef84` | 2021-03-17 | Use command pos as origin for /build | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `6684f0d9` | 2021-03-17 | Fix tab complete erasing previous arguments | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `61cf726a` | 2021-03-17 | Change prefix color | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `456aa6bf` | 2021-03-17 | Fix MovementTraverse looping in water under vine | **M4** | Traverse v vodi pod trto |
| `333cc295` | 2021-03-17 | Assert rubber banding cannot happen serverside | **M2** | brez rubber bandinga na strežniku |
| `a3081aac` | 2021-03-17 | Fix WorldScanner NPE | **M12** | NPE v WorldScanner |
| `1f43bbe7` | 2021-03-17 | Un-hardcode number of sections in chunk | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `b3da3410` | 2021-03-17 | Optimize chunk scanning | **M12** | hitrejše skeniranje chunkov (vzorec za D-035) |
| `38c477f1` | 2021-03-17 | Fix chunk lookups being excruciatingly slow | **M1** | hitro branje chunkov na strežniku |
| `8c12848f` | 2021-03-17 | Prevent MovementAscend from mining in water | **M4** | Ascend ne koplje v vodi |
| `076c44ff` | 2021-03-17 | Make feetPos state lookup faster | **M1** | hitrejši feetPos |
| `d0970ec6` | 2021-03-17 | Take some performance notes | **M12** | zmogljivost MineProcess |
| `de9d45ec` | 2021-03-20 | actually trolling is not fine | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `53b5a594` | 2021-03-20 | Update readme | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `770d700c` | 2021-03-20 | Stop recommending hack clients | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `a9129616` | 2021-03-20 | Merge remote-tracking branch 'orig/1.16.5' into dev | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `dc67fc17` | 2021-03-21 | Fix compilation errors | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `805eebf8` | 2021-03-21 | Make Proguard work with Yarn | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `7db8bb07` | 2021-03-21 | Make Proguard work on Java 9+ | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `afab2352` | 2021-03-21 | Get rid of most sysout calls | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `89b1174a` | 2021-03-21 | Pave the way for per-instance settings | **M1** | nastavitve na instanco (priprava) |
| `7bd582c2` | 2021-03-22 | Implement cascading settings | **M1** | kaskadne nastavitve |
| `b445b384` | 2021-03-22 | Allow /setting to modify both local and global settings | **SKIP** | ni relevantno za 1.12 NPC noge |
| `a1bb2422` | 2021-03-22 | Actually implement local settings | **M1** | lokalne nastavitve |
| `46b388ca` | 2021-03-22 | Stop sprinting when parkour reaches goal | **M2** | parkour: sprint na cilju |
| `496de22a` | 2021-03-23 | Move fake player classes to API | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `08d79439` | 2021-03-24 | Rename BaritoneAPI#getSettings to getGlobalSettings | **M1** | globalne nastavitve preimenovane |
| `19a15193` | 2021-03-24 | Update readme | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `175bb84c` | 2021-03-24 | Configure automatic publishing in buildscript | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `f5bb4962` | 2021-03-24 | Create changelog | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `f7a15f8d` | 2021-03-25 | Migrate custom build tasks to The Gradle Way | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `d726a0e8` | 2021-03-26 | Use CCA for baritone ticking | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `fab0df8d` | 2021-03-26 | Save entrypoint classes from proguard | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `4b6b6062` | 2021-03-27 | Save fake player profile to NBT | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `b1899f30` | 2021-03-28 | Improve the method to determine if a door can be opened | **M4** | ali se vrata dajo odpreti |
| `f3b5b24a` | 2021-03-28 | Somewhat handle different sizes in MovementDiagonal | **M8** | velikosti v MovementDiagonal |
| `7b3a8a43` | 2021-03-28 | Un-hardcode small block detection | **M8** | zaznava majhnih blokov |
| `5d9ddabe` | 2021-03-28 | Somewhat handle different sizes in MovementParkour | **M8** | velikosti v MovementParkour |
| `109a8e67` | 2021-03-28 | Document each Movement's behaviour | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `0a6399f9` | 2021-03-28 | Un-hardcode player dimensions in MovementDownward | **M8** | dimenzije v MovementDownward |
| `63e756af` | 2021-03-28 | Handle scaffolding in MovementDownward | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `06176954` | 2021-03-29 | Un-hardcode missed nether check for MLG | **M11** | MLG nether preverba |
| `a3656efe` | 2021-03-29 | Handle most interactions with scaffolding | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `43268b09` | 2021-03-29 | Fix MovementDownward breaking wrong block | **M11** | MovementDownward ruši napačen blok |
| `39da8286` | 2021-03-31 | Somewhat handle different sizes in MovementAscend | **M8** | velikosti v MovementAscend |
| `0412211c` | 2021-03-31 | Fix tags not being loaded in a dev env | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `885027f1` | 2021-03-31 | Make players jump to reach levitating water | **M4** | skok do lebdeče vode |
| `a9c929f0` | 2021-03-31 | Fix MovementTraverse going bonkers with large entities | **M8** | Traverse z velikimi entitetami |
| `2fec595b` | 2021-03-31 | Partially fix MovementAscend's extended block placement | **M11** | razširjeno postavljanje v Ascend |
| `d27076e2` | 2021-04-01 | Save world data to NBT instead of external files | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `a67d6901` | 2021-04-01 | Move executor to main mod class | **M5** | executor v glavnem razredu moda |
| `c5db7d1f` | 2021-04-01 | Turn SelectionManager into an Entity component | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `a8d793f9` | 2021-04-01 | Decouple basic movements from look direction | **M2** | gibanje neodvisno od smeri pogleda |
| `921af7dd` | 2021-04-01 | Update changelog | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `b3d431f8` | 2021-04-01 | Fix crash when building with ladders | **M14** | gradnja z lestvami |
| `71029ea9` | 2021-04-01 | Document dependencies in fabric.mod.json | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `18d66bed` | 2021-04-01 | Add mod icon | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `ddcbf339` | 2021-04-01 | Make fake players use their display profile's name | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `72695f8c` | 2021-04-01 | Automatically release used items on next tick | **M11** | sprostitev uporabljenih predmetov |
| `4a94645c` | 2021-04-01 | Fix air bridging | **M11** | air bridging |
| `5c99eaff` | 2021-04-01 | Update changelog | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `5cda8303` | 2021-04-03 | Fix mixin refmap reference | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `1f6e6131` | 2021-04-04 | Replace block lists with tags in settings | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `2bce8f85` | 2021-04-04 | Disable rendering for other entities by default | **M3** | izris za druge entitete privzeto izklopljen |
| `5671596f` | 2021-04-04 | Remove redundant type unsound interface | **SKIP** | ni relevantno za 1.12 NPC noge |
| `2cf35fe2` | 2021-04-04 | Fix tab complete for aliases | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `32bf8032` | 2021-04-04 | Generify renderDebug settings into syncWithOps | **M3** | syncWithOps |
| `9d6a5306` | 2021-04-04 | Update changelog | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `e4d493a9` | 2021-04-06 | Make some fields and methods accessible from API | **M6** | dostop iz API-ja |
| `7dd013f0` | 2021-04-06 | Let other mods supply the avoidance list | **M6** | drugi modi podajo seznam izogibanja |
| `49f20d5f` | 2021-04-06 | Yeet the life buoy | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `5d0a1d69` | 2021-04-06 | Let fake players sink faster on command | **SKIP** | fake player potapljanje |
| `d5ff55ce` | 2021-04-06 | Prevent MovementPillar from ascending flowing water | **M4** | Pillar ne leze po tekoči vodi |
| `77aadb8a` | 2021-04-08 | Tweak movement costs in water | **M4** | cene gibanja v vodi |
| `64c680a9` | 2021-04-08 | Fix MovementTraverse in water | **M4** | Traverse v vodi |
| `bd80cf1d` | 2021-04-10 | Make all movement cost estimations take a result arg | **M4** | ocene cen z rezultatnim argumentom |
| `46a3b4d1` | 2021-04-29 | Implement oxygen movement costs | **M4** | cena kisika |
| `9b018aaa` | 2021-04-29 | Add configs for underwater pathing | **M4** | nastavitve za pot pod vodo |
| `51694ee9` | 2021-04-29 | Fix some drowning scenarios | **M4** | utopitve |
| `7f6f8034` | 2021-04-30 | Allow sprint swimming in horizontal movements | **M4** | sprint plavanje |
| `1fb7ad15` | 2021-04-30 | Fix fall in water | **M4** | padec v vodo |
| `3216de48` | 2021-04-30 | Fix door interactions | **M4** | interakcije z vrati |
| `3f5c5014` | 2021-05-01 | Merge branch 'feature/swimming' into dev | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `82f2a852` | 2021-05-01 | Update changelog | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `fc065ee2` | 2021-05-02 | Fix crash when fake players ride something | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `8c1cf9f1` | 2021-05-03 | Fix crash when fake players get summoned clientside | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `fa380f68` | 2021-05-05 | Fix crash on dedicated servers | **M1** | sesutje na dedicated strežniku |
| `33b707cf` | 2021-05-05 | Fix sneaking being reset every tick serverside for all players | **M2** | sneak se ne ponastavi vsak tick |
| `ea4a817a` | 2021-05-05 | Fix fake player unloading | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `a6c0ba83` | 2021-05-05 | Update version and changelog | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `1afcc8fe` | 2021-05-10 | Prevent NPE in ServerWorld#unloadEntities | **M1** | NPE ob odstranjevanju entitet |
| `41371c6f` | 2021-05-17 | Guard against NPEs in MovementHelper#isBlockNormalCube | **M1** | NPE v isBlockNormalCube |
| `e8b74bbe` | 2021-05-17 | Repackage jars to ladysnake.automatone | **M1** | prepakiranje paketov (relokacija) |
| `cbc583d0` | 2021-05-17 | Move fake player to test mod | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `240cf8d0` | 2021-05-17 | Update changelog | **SKIP** | Fabric/CCA/brigadier/gradnja/fake player ali 1.14+ posebnost |
| `fadc7082` | 2021-05-19 | Fix head yaw being 0 on client fake player spawn | **M2** | yaw glave ob spawnu |
| `843b8397` | 2021-05-26 | Expose baritone component factory through API | **M6** | tovarna instanc v API |
