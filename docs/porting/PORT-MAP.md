# Port map: Baritone v1.2.19 → NPC Baritone

Generirano s `tools/portmap.py` iz `references/baritone-1.12.2/src` (brez `src/test`). Ne urejaj ročno — spremeni pravila v skripti in jo poženi znova.

Akcije: **KEEP** nespremenjeno (razen relokacije paketa); **ADAPT** delne spremembe; **REWRITE** napisano znova po vzoru; **CLIENT** samo klientski debug prikaz (M3); **LATER** odloženo v naveden milestone (M11–M15); **DROP** se ne prenese. Stolpec *klient* = datoteka uvaža klientske razrede.

| akcija | datotek | vrstic |
|---|---:|---:|
| KEEP | 68 | 8865 |
| ADAPT | 39 | 6707 |
| REWRITE | 13 | 1454 |
| CLIENT | 4 | 696 |
| LATER | 46 | 5912 |
| DROP | 178 | 17399 |
| **skupaj** | 348 | 41033 |

| datoteka (`src/…`) | vrstic | klient | akcija | razlog |
|---|---:|:-:|---|---|
| `api/java/baritone/api/BaritoneAPI.java` | 52 |  | REWRITE | globalni vhod v knjižnico |
| `api/java/baritone/api/IBaritone.java` | 146 |  | ADAPT |  |
| `api/java/baritone/api/IBaritoneProvider.java` | 140 | da | ADAPT |  |
| `api/java/baritone/api/Settings.java` | 1578 | da | ADAPT | profili nastavitev na instanco; NPC privzete vrednosti (D-016) |
| `api/java/baritone/api/behavior/IBehavior.java` | 30 |  | ADAPT | izvajanje poti; bounded executor (D-017) |
| `api/java/baritone/api/behavior/ILookBehavior.java` | 50 |  | ADAPT | obrat entitete namesto kamere (D-011) |
| `api/java/baritone/api/behavior/IPathingBehavior.java` | 132 |  | ADAPT | izvajanje poti; bounded executor (D-017) |
| `api/java/baritone/api/behavior/look/IAimProcessor.java` | 45 |  | ADAPT | obrat entitete namesto kamere (D-011) |
| `api/java/baritone/api/behavior/look/ITickableAimProcessor.java` | 47 |  | ADAPT | obrat entitete namesto kamere (D-011) |
| `api/java/baritone/api/cache/IBlockTypeAccess.java` | 34 |  | KEEP |  |
| `api/java/baritone/api/cache/ICachedRegion.java` | 48 |  | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `api/java/baritone/api/cache/ICachedWorld.java` | 84 |  | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `api/java/baritone/api/cache/IWaypoint.java` | 136 |  | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `api/java/baritone/api/cache/IWaypointCollection.java` | 66 |  | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `api/java/baritone/api/cache/IWorldData.java` | 40 |  | REWRITE | minimalen per-dimension WorldData brez datotek na disku |
| `api/java/baritone/api/cache/IWorldProvider.java` | 41 |  | REWRITE | minimalen per-dimension WorldData brez datotek na disku |
| `api/java/baritone/api/cache/IWorldScanner.java` | 96 |  | LATER | M12: vzorec za strežniški ServerBlockScanner (D-035) |
| `api/java/baritone/api/cache/Waypoint.java` | 102 |  | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `api/java/baritone/api/command/Command.java` | 67 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/IBaritoneChatControl.java` | 42 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/ICommand.java` | 67 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/ICommandSystem.java` | 29 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/argparser/IArgParser.java` | 60 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/argparser/IArgParserManager.java` | 69 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/argument/IArgConsumer.java` | 594 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/argument/ICommandArgument.java` | 101 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/BlockById.java` | 65 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/EntityClassById.java` | 61 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/ForAxis.java` | 43 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/ForBlockOptionalMeta.java` | 153 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/ForEnumFacing.java` | 43 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/ForWaypoints.java` | 81 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/IDatatype.java` | 56 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/IDatatypeContext.java` | 46 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/IDatatypeFor.java` | 43 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/IDatatypePost.java` | 41 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/IDatatypePostFunction.java` | 29 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/NearbyPlayer.java` | 55 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/RelativeBlockPos.java` | 57 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/RelativeCoordinate.java` | 69 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/RelativeFile.java` | 110 | da | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/RelativeGoal.java` | 64 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/RelativeGoalBlock.java` | 53 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/RelativeGoalXZ.java` | 52 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/datatypes/RelativeGoalYLevel.java` | 50 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandErrorMessageException.java` | 29 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandException.java` | 29 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandInvalidArgumentException.java` | 43 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandInvalidStateException.java` | 25 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandInvalidTypeException.java` | 39 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandNoParserForTypeException.java` | 25 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandNotEnoughArgumentsException.java` | 25 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandNotFoundException.java` | 40 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandTooManyArgumentsException.java` | 25 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/CommandUnhandledException.java` | 41 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/exception/ICommandException.java` | 55 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/helpers/Paginator.java` | 184 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/helpers/TabCompleteHelper.java` | 287 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/manager/ICommandManager.java` | 52 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/command/registry/Registry.java` | 135 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `api/java/baritone/api/event/events/BlockChangeEvent.java` | 47 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/events/BlockInteractEvent.java` | 71 |  | DROP | klientski dogodek |
| `api/java/baritone/api/event/events/ChatEvent.java` | 43 |  | DROP | klientski dogodek |
| `api/java/baritone/api/event/events/ChunkEvent.java` | 122 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/events/PacketEvent.java` | 58 |  | DROP | klientski dogodek |
| `api/java/baritone/api/event/events/PathEvent.java` | 33 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/events/PlayerUpdateEvent.java` | 43 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/events/RenderEvent.java` | 41 |  | DROP | klientski dogodek |
| `api/java/baritone/api/event/events/RotationMoveEvent.java` | 113 |  | DROP | klientski dogodek |
| `api/java/baritone/api/event/events/SprintStateEvent.java` | 35 |  | DROP | klientski dogodek |
| `api/java/baritone/api/event/events/TabCompleteEvent.java` | 34 |  | DROP | klientski dogodek |
| `api/java/baritone/api/event/events/TickEvent.java` | 76 | da | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/events/WorldEvent.java` | 57 | da | DROP | klientski dogodek |
| `api/java/baritone/api/event/events/type/Cancellable.java` | 40 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/events/type/EventState.java` | 35 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/events/type/ICancellable.java` | 35 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/events/type/Overrideable.java` | 53 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/listener/AbstractGameEventListener.java` | 80 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/listener/IEventBus.java` | 36 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/event/listener/IGameEventListener.java` | 162 | da | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `api/java/baritone/api/pathing/calc/IPath.java` | 179 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/calc/IPathFinder.java` | 66 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/calc/IPathingControlManager.java` | 47 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/Goal.java` | 71 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalAxis.java` | 59 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalBlock.java` | 119 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalComposite.java` | 96 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalGetToBlock.java` | 93 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalInverted.java` | 77 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalNear.java` | 121 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalRunAway.java` | 163 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalStrictDirection.java` | 109 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalTwoBlocks.java` | 105 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalXZ.java` | 132 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/goals/GoalYLevel.java` | 86 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/movement/ActionCosts.java` | 98 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/movement/IMovement.java` | 55 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/movement/MovementStatus.java` | 74 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/pathing/path/IPathExecutor.java` | 31 |  | KEEP | jedro iskanja poti |
| `api/java/baritone/api/process/IBaritoneProcess.java` | 114 |  | KEEP | pogodba procesov |
| `api/java/baritone/api/process/IBuilderProcess.java` | 78 | da | LATER | M14 |
| `api/java/baritone/api/process/ICustomGoalProcess.java` | 55 |  | KEEP | pogodba procesov |
| `api/java/baritone/api/process/IElytraProcess.java` | 50 |  | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `api/java/baritone/api/process/IExploreProcess.java` | 27 |  | DROP | proces je izpuščen |
| `api/java/baritone/api/process/IFarmProcess.java` | 45 |  | LATER | M13 |
| `api/java/baritone/api/process/IFollowProcess.java` | 51 |  | LATER | M15 |
| `api/java/baritone/api/process/IGetToBlockProcess.java` | 35 |  | LATER | M12 |
| `api/java/baritone/api/process/IMineProcess.java` | 117 |  | LATER | M12 |
| `api/java/baritone/api/process/PathingCommand.java` | 61 |  | KEEP | pogodba procesov |
| `api/java/baritone/api/process/PathingCommandType.java` | 65 |  | KEEP | pogodba procesov |
| `api/java/baritone/api/schematic/AbstractSchematic.java` | 50 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/CompositeSchematic.java` | 81 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/CompositeSchematicEntry.java` | 33 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/FillSchematic.java` | 54 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/ISchematic.java` | 95 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/ISchematicSystem.java` | 44 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/IStaticSchematic.java` | 59 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/MaskSchematic.java` | 55 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/ReplaceSchematic.java` | 53 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/ShellSchematic.java` | 32 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/SubstituteSchematic.java` | 91 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/WallsSchematic.java` | 32 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/format/ISchematicFormat.java` | 45 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/mask/AbstractMask.java` | 49 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/mask/Mask.java` | 60 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/mask/PreComputedMask.java` | 44 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/mask/StaticMask.java` | 82 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/mask/operator/BinaryOperatorMask.java` | 79 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/mask/operator/NotMask.java` | 56 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/mask/shape/CylinderMask.java` | 69 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/schematic/mask/shape/SphereMask.java` | 64 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `api/java/baritone/api/selection/ISelection.java` | 91 |  | DROP | izbira /sel; območja poda porabnik (D-037) |
| `api/java/baritone/api/selection/ISelectionManager.java` | 116 |  | DROP | izbira /sel; območja poda porabnik (D-037) |
| `api/java/baritone/api/utils/BetterBlockPos.java` | 249 |  | KEEP |  |
| `api/java/baritone/api/utils/BlockOptionalMeta.java` | 361 |  | KEEP |  |
| `api/java/baritone/api/utils/BlockOptionalMetaLookup.java` | 97 |  | KEEP |  |
| `api/java/baritone/api/utils/BlockUtils.java` | 67 |  | ADAPT | MCP preimenovanja |
| `api/java/baritone/api/utils/BooleanBinaryOperator.java` | 27 |  | KEEP |  |
| `api/java/baritone/api/utils/BooleanBinaryOperators.java` | 38 |  | KEEP |  |
| `api/java/baritone/api/utils/Helper.java` | 240 | da | REWRITE | log4j namesto klepeta |
| `api/java/baritone/api/utils/IInputOverrideHandler.java` | 34 |  | KEEP |  |
| `api/java/baritone/api/utils/IPlayerContext.java` | 108 | da | REWRITE | IEntityContext nad EntityLiving + referenčni okvir (D-006, D-021) |
| `api/java/baritone/api/utils/IPlayerController.java` | 62 | da | REWRITE | interakcije entitete: vrata (D-015); M11 roke workerja (D-032) |
| `api/java/baritone/api/utils/MyChunkPos.java` | 37 |  | KEEP |  |
| `api/java/baritone/api/utils/NotificationHelper.java` | 89 |  | DROP | namizna obvestila |
| `api/java/baritone/api/utils/Pair.java` | 59 |  | KEEP |  |
| `api/java/baritone/api/utils/PathCalculationResult.java` | 55 |  | KEEP |  |
| `api/java/baritone/api/utils/RayTraceUtils.java` | 65 |  | ADAPT |  |
| `api/java/baritone/api/utils/Rotation.java` | 163 |  | KEEP |  |
| `api/java/baritone/api/utils/RotationUtils.java` | 287 | da | ADAPT |  |
| `api/java/baritone/api/utils/SettingsUtil.java` | 354 | da | ADAPT | branje configa strežnika namesto .minecraft/baritone |
| `api/java/baritone/api/utils/TypeUtils.java` | 44 |  | KEEP |  |
| `api/java/baritone/api/utils/VecUtils.java` | 116 |  | KEEP |  |
| `api/java/baritone/api/utils/accessor/IItemStack.java` | 23 |  | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `api/java/baritone/api/utils/gui/BaritoneToast.java` | 79 | da | CLIENT | ponovno napisano kot neobvezen debug prikaz (M3) |
| `api/java/baritone/api/utils/input/Input.java` | 71 |  | KEEP |  |
| `api/java/baritone/api/utils/interfaces/IGoalRenderPos.java` | 25 |  | KEEP |  |
| `launch/java/baritone/launch/BaritoneTweaker.java` | 55 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinAnvilChunkLoader.java` | 43 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinBitArray.java` | 77 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinBlockStateContainer.java` | 57 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinChatTabCompleter.java` | 39 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinChunkProviderClient.java` | 39 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinChunkProviderServer.java` | 42 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinChunkRenderContainer.java` | 52 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinChunkRenderWorker.java` | 57 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinEntityFireworkRocket.java` | 57 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinEntityLivingBase.java` | 152 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinEntityPlayerSP.java` | 143 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinEntityRenderer.java` | 45 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinGuiScreen.java` | 33 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinItemStack.java` | 68 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinItemTool.java` | 36 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinMinecraft.java` | 210 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinNBTTagLongArray.java` | 34 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinNetHandlerPlayClient.java` | 141 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinNetworkManager.java` | 116 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinPlayerControllerMP.java` | 41 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinRenderChunk.java` | 89 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinRenderList.java` | 47 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinStateImplementation.java` | 60 |  | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinTabCompleter.java` | 83 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinVboRenderList.java` | 47 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `launch/java/baritone/launch/mixins/MixinWorldClient.java` | 75 | da | DROP | klientski mixini in launcher; brez coremoda (D-008) |
| `main/java/baritone/Baritone.java` | 264 | da | REWRITE | vitka instanca na entiteto (NavigatorCore) |
| `main/java/baritone/BaritoneProvider.java` | 91 | da | REWRITE | register instanc po entiteti, šibke reference |
| `main/java/baritone/KeepName.java` | 21 |  | DROP | proguard |
| `main/java/baritone/behavior/Behavior.java` | 39 |  | ADAPT | izvajanje poti; bounded executor (D-017) |
| `main/java/baritone/behavior/InventoryBehavior.java` | 230 | da | LATER | M11: inventar workerja nad IItemHandler (D-034) |
| `main/java/baritone/behavior/LookBehavior.java` | 347 |  | ADAPT | obrat entitete namesto kamere (D-011) |
| `main/java/baritone/behavior/PathingBehavior.java` | 573 |  | ADAPT | izvajanje poti; bounded executor (D-017) |
| `main/java/baritone/behavior/WaypointBehavior.java` | 92 |  | DROP | waypointi igralca |
| `main/java/baritone/behavior/look/ForkableRandom.java` | 85 |  | ADAPT | obrat entitete namesto kamere (D-011) |
| `main/java/baritone/cache/CachedChunk.java` | 271 | da | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `main/java/baritone/cache/CachedRegion.java` | 358 |  | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `main/java/baritone/cache/CachedWorld.java` | 326 |  | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `main/java/baritone/cache/ChunkPacker.java` | 171 |  | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `main/java/baritone/cache/FasterWorldScanner.java` | 276 |  | LATER | M12: vzorec za strežniški ServerBlockScanner (D-035) |
| `main/java/baritone/cache/WaypointCollection.java` | 145 |  | DROP | klientski predpomnilnik regij/waypointi; NPC ne hodi po nenaloženem svetu (D-014) |
| `main/java/baritone/cache/WorldData.java` | 63 |  | REWRITE | minimalen per-dimension WorldData brez datotek na disku |
| `main/java/baritone/cache/WorldProvider.java` | 184 | da | REWRITE | minimalen per-dimension WorldData brez datotek na disku |
| `main/java/baritone/cache/WorldScanner.java` | 185 | da | LATER | M12: vzorec za strežniški ServerBlockScanner (D-035) |
| `main/java/baritone/command/CommandSystem.java` | 35 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/ExampleBaritoneControl.java` | 200 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/argparser/ArgParserManager.java` | 90 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/argparser/DefaultArgParsers.java` | 124 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/argument/ArgConsumer.java` | 444 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/argument/CommandArgument.java` | 96 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/argument/CommandArguments.java` | 79 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/AxisCommand.java` | 64 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/BlacklistCommand.java` | 70 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/BuildCommand.java` | 93 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ClickCommand.java` | 61 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ComeCommand.java` | 64 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/CommandAlias.java` | 64 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/DefaultCommands.java` | 79 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ETACommand.java` | 84 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ElytraCommand.java` | 224 | da | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `main/java/baritone/command/defaults/ExecutionControlCommands.java` | 207 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ExploreCommand.java` | 74 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ExploreFilterCommand.java` | 91 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/FarmCommand.java` | 89 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/FindCommand.java` | 117 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/FollowCommand.java` | 169 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ForceCancelCommand.java` | 64 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/GcCommand.java` | 61 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/GoalCommand.java` | 102 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/GotoCommand.java` | 86 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/HelpCommand.java` | 126 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/InvertCommand.java` | 75 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/LitematicaCommand.java` | 70 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/MineCommand.java` | 82 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/PathCommand.java` | 66 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ProcCommand.java` | 85 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ReloadAllCommand.java` | 61 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/RenderCommand.java` | 71 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/RepackCommand.java` | 62 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/SaveAllCommand.java` | 61 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/SchematicaCommand.java` | 60 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/SelCommand.java` | 452 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/SetCommand.java` | 280 | da | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/SurfaceCommand.java` | 89 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/ThisWayCommand.java` | 67 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/TunnelCommand.java` | 113 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/VersionCommand.java` | 66 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/defaults/WaypointsCommand.java` | 416 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/command/manager/CommandManager.java` | 163 |  | DROP | klientski chat ukazi; nadomesti /npcb (M3) |
| `main/java/baritone/event/GameEventHandler.java` | 188 |  | ADAPT | dogodki se prožijo iz navigatorja (D-009) |
| `main/java/baritone/pathing/calc/AStarPathFinder.java` | 177 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/calc/AbstractNodeCostSearch.java` | 241 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/calc/Path.java` | 180 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/calc/PathNode.java` | 107 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/calc/openset/BinaryHeapOpenSet.java` | 133 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/calc/openset/IOpenSet.java` | 54 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/calc/openset/LinkedListOpenSet.java` | 91 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/CalculationContext.java` | 185 | da | ADAPT | jedro; preimenovanja MCP + kontekst entitete |
| `main/java/baritone/pathing/movement/Movement.java` | 298 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/MovementHelper.java` | 734 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/MovementState.java` | 92 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/Moves.java` | 361 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/movements/MovementAscend.java` | 242 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/movements/MovementDescend.java` | 293 | da | ADAPT | jedro; preimenovanja MCP + kontekst entitete |
| `main/java/baritone/pathing/movement/movements/MovementDiagonal.java` | 323 | da | ADAPT | jedro; preimenovanja MCP + kontekst entitete |
| `main/java/baritone/pathing/movement/movements/MovementDownward.java` | 97 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/movements/MovementFall.java` | 202 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/movements/MovementParkour.java` | 310 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/movements/MovementPillar.java` | 287 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/movement/movements/MovementTraverse.java` | 372 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/path/CutoffPath.java` | 70 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/path/PathExecutor.java` | 667 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/path/SplicedPath.java` | 106 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/precompute/PrecomputedData.java` | 117 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/pathing/precompute/Ternary.java` | 22 |  | KEEP | jedro iskanja poti |
| `main/java/baritone/process/BackfillProcess.java` | 143 |  | LATER | M12: zasipanje lukenj |
| `main/java/baritone/process/BuilderProcess.java` | 1137 | da | LATER | M14: gradnja (D-037) |
| `main/java/baritone/process/CustomGoalProcess.java` | 138 |  | ADAPT | edini proces v jedru: "pojdi do cilja" |
| `main/java/baritone/process/ElytraProcess.java` | 565 |  | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `main/java/baritone/process/ExploreProcess.java` | 300 |  | DROP | raziskovanje nenaloženega sveta (D-014, D-037) |
| `main/java/baritone/process/FarmProcess.java` | 371 |  | LATER | M13: farmanje |
| `main/java/baritone/process/FollowProcess.java` | 126 |  | LATER | M15: sledenje kot proces |
| `main/java/baritone/process/GetToBlockProcess.java` | 255 |  | LATER | M12: pot do bloka |
| `main/java/baritone/process/InventoryPauserProcess.java` | 90 |  | DROP | premik v inventarju je takojšen, ni pavze (D-034) |
| `main/java/baritone/process/MineProcess.java` | 537 |  | LATER | M12: rudarjenje (D-035) |
| `main/java/baritone/process/elytra/BlockStateOctreeInterface.java` | 54 |  | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `main/java/baritone/process/elytra/ElytraBehavior.java` | 1359 |  | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `main/java/baritone/process/elytra/NetherPath.java` | 65 |  | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `main/java/baritone/process/elytra/NetherPathfinderContext.java` | 243 |  | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `main/java/baritone/process/elytra/NullElytraProcess.java` | 92 |  | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `main/java/baritone/process/elytra/PathCalculationException.java` | 28 |  | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `main/java/baritone/process/elytra/UnpackedSegment.java` | 83 |  | DROP | Elytra + nativni nether-pathfinder (28+ napak prevoda, D-002) |
| `main/java/baritone/selection/Selection.java` | 130 |  | DROP | izbira /sel; območja poda porabnik (D-037) |
| `main/java/baritone/selection/SelectionManager.java` | 118 |  | DROP | izbira /sel; območja poda porabnik (D-037) |
| `main/java/baritone/selection/SelectionRenderer.java` | 57 |  | DROP | izbira /sel; območja poda porabnik (D-037) |
| `main/java/baritone/utils/BaritoneMath.java` | 37 |  | KEEP |  |
| `main/java/baritone/utils/BaritoneProcessHelper.java` | 39 |  | ADAPT |  |
| `main/java/baritone/utils/BlockBreakHelper.java` | 73 |  | LATER | M11: roke workerja (D-031, D-032) |
| `main/java/baritone/utils/BlockPlaceHelper.java` | 55 |  | LATER | M11: roke workerja (D-031, D-032) |
| `main/java/baritone/utils/BlockStateInterface.java` | 167 |  | ADAPT | strežniški id2ChunkMap, omejena kopija (D-013) |
| `main/java/baritone/utils/BlockStateInterfaceAccessWrapper.java` | 83 |  | ADAPT | Forge isSideSolid manjka (napaka prevoda) |
| `main/java/baritone/utils/GuiClick.java` | 136 | da | CLIENT | ponovno napisano kot neobvezen debug prikaz (M3) |
| `main/java/baritone/utils/IRenderer.java` | 138 | da | CLIENT | ponovno napisano kot neobvezen debug prikaz (M3) |
| `main/java/baritone/utils/InputOverrideHandler.java` | 122 |  | REWRITE | Input → moveForward/moveStrafing/setJumping/sprint (D-010) |
| `main/java/baritone/utils/PathRenderer.java` | 343 | da | CLIENT | ponovno napisano kot neobvezen debug prikaz (M3) |
| `main/java/baritone/utils/PathingCommandContext.java` | 33 |  | KEEP |  |
| `main/java/baritone/utils/PathingControlManager.java` | 217 |  | ADAPT |  |
| `main/java/baritone/utils/PlayerMovementInput.java` | 58 |  | DROP | klientski MovementInput |
| `main/java/baritone/utils/ToolSet.java` | 230 | da | ADAPT | orodje v roki entitete ali stub (cena rušenja); M11 iz inventarja workerja (D-034) |
| `main/java/baritone/utils/accessor/IAnvilChunkLoader.java` | 32 |  | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `main/java/baritone/utils/accessor/IBitArray.java` | 10 |  | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `main/java/baritone/utils/accessor/IBlockStateContainer.java` | 16 |  | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `main/java/baritone/utils/accessor/IChunkProviderClient.java` | 26 |  | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `main/java/baritone/utils/accessor/IChunkProviderServer.java` | 31 |  | LATER | ni potreben: id2ChunkMap je v 1.12 javen |
| `main/java/baritone/utils/accessor/IEntityFireworkRocket.java` | 25 |  | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `main/java/baritone/utils/accessor/IGuiScreen.java` | 25 |  | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `main/java/baritone/utils/accessor/IItemTool.java` | 24 |  | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `main/java/baritone/utils/accessor/INBTTagLongArray.java` | 26 |  | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `main/java/baritone/utils/accessor/IPlayerControllerMP.java` | 29 | da | DROP | mixin accessorji; v 1.12 strežniku javna polja / AT |
| `main/java/baritone/utils/pathing/Avoidance.java` | 97 |  | ADAPT | izogibanje mobom na strežniku |
| `main/java/baritone/utils/pathing/BetterWorldBorder.java` | 50 |  | KEEP |  |
| `main/java/baritone/utils/pathing/Favoring.java` | 55 |  | KEEP |  |
| `main/java/baritone/utils/pathing/MutableMoveResult.java` | 44 |  | KEEP |  |
| `main/java/baritone/utils/pathing/PathBase.java` | 58 |  | KEEP |  |
| `main/java/baritone/utils/pathing/PathingBlockType.java` | 47 |  | KEEP |  |
| `main/java/baritone/utils/player/BaritonePlayerContext.java` | 88 | da | REWRITE | EntityContext + nadzornik interakcij |
| `main/java/baritone/utils/player/BaritonePlayerController.java` | 99 | da | REWRITE | EntityContext + nadzornik interakcij |
| `main/java/baritone/utils/schematic/MapArtSchematic.java` | 70 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `main/java/baritone/utils/schematic/SchematicSystem.java` | 51 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `main/java/baritone/utils/schematic/SelectionSchematic.java` | 53 |  | DROP | izbira /sel; območja poda porabnik (D-037) |
| `main/java/baritone/utils/schematic/StaticSchematic.java` | 50 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `main/java/baritone/utils/schematic/format/DefaultSchematicFormats.java` | 101 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `main/java/baritone/utils/schematic/format/defaults/LitematicaSchematic.java` | 346 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `main/java/baritone/utils/schematic/format/defaults/MCEditSchematic.java` | 69 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `main/java/baritone/utils/schematic/format/defaults/SpongeSchematic.java` | 157 |  | LATER | M14: sheme in bralniki datotek (D-037) |
| `main/java/baritone/utils/schematic/litematica/LitematicaHelper.java` | 214 |  | DROP | integracija s klientskim modom Schematica/Litematica (D-037) |
| `main/java/baritone/utils/schematic/schematica/SchematicAdapter.java` | 59 |  | DROP | integracija s klientskim modom Schematica/Litematica (D-037) |
| `main/java/baritone/utils/schematic/schematica/SchematicaHelper.java` | 45 |  | DROP | integracija s klientskim modom Schematica/Litematica (D-037) |
| `main/java/baritone/utils/type/VarInt.java` | 95 |  | KEEP |  |
| `schematica_api/java/com/github/lunatrius/core/util/math/MBlockPos.java` | 42 |  | DROP | integracija s Schematico |
| `schematica_api/java/com/github/lunatrius/schematica/Schematica.java` | 25 |  | DROP | integracija s Schematico |
| `schematica_api/java/com/github/lunatrius/schematica/api/ISchematic.java` | 32 |  | DROP | integracija s Schematico |
| `schematica_api/java/com/github/lunatrius/schematica/client/world/SchematicWorld.java` | 30 |  | DROP | integracija s Schematico |
| `schematica_api/java/com/github/lunatrius/schematica/proxy/ClientProxy.java` | 25 |  | DROP | integracija s Schematico |
| `schematica_api/java/com/github/lunatrius/schematica/proxy/CommonProxy.java` | 20 |  | DROP | integracija s Schematico |
| `schematica_api/java/fi/dy/masa/litematica/Litematica.java` | 21 |  | DROP | integracija s Schematico |
| `schematica_api/java/fi/dy/masa/litematica/data/DataManager.java` | 33 |  | DROP | integracija s Schematico |
| `schematica_api/java/fi/dy/masa/litematica/schematic/placement/SchematicPlacement.java` | 35 |  | DROP | integracija s Schematico |
| `schematica_api/java/fi/dy/masa/litematica/schematic/placement/SchematicPlacementManager.java` | 31 |  | DROP | integracija s Schematico |
| `schematica_api/java/fi/dy/masa/litematica/schematic/placement/SchematicPlacementUnloaded.java` | 43 |  | DROP | integracija s Schematico |
