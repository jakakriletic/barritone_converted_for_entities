# MCP preimenovanja: Baritone v1.2.19 proti Forge 1.12.2

Izhod `tools/compile_probe.sh`: prevod `src/api` + `src/main` + `src/schematica_api` iz `references/baritone-1.12.2` z `javac --release 8` proti `forgeBin.jar` (MCP snapshot_20170927) + knjižnice Minecrafta + fastutil. Baritone sam uporablja `stable_39`. Skupaj 81 napak; 43 v `process/elytra/**` (odpade, D-002) ni v tabeli.

**M0.7 (24. 9. 2026): potrjeno** — prevod proti CNPC mappingu (snapshot_20171003, `forgeSrc`) da iste napake na istih datotekah in vrsticah (81 / 43 / 38); glej `06-RAZISKAVA.md` §1a.

Od 38 napak jih je **24 v datotekah, ki ostanejo** (KEEP/ADAPT/REWRITE); ostale izginejo z izrezom v M1.2.

| datoteka (`src/…`) | vrstica | simbol | popravek | port map | koda |
|---|---:|---|---|---|---|
| `api/java/baritone/api/process/IBuilderProcess.java` | 56 | `variable gameDir` | mcDataDir (klient; datoteka večinoma odpade) | DROP | `File file = new File(new File(Minecraft.getMinecraft().gameDir, "schematics"), s` |
| `api/java/baritone/api/command/datatypes/EntityClassById.java` | 36 | `variable REGISTRY` | EntityList.getClass(id) / ForgeRegistries.ENTITIES | DROP | `entity = EntityList.REGISTRY.getObject(id);` |
| `api/java/baritone/api/command/datatypes/RelativeFile.java` | 104 | `variable gameDir` | mcDataDir (klient; datoteka večinoma odpade) | DROP | `File gameDir = mc.gameDir.getAbsoluteFile();` |
| `api/java/baritone/api/pathing/goals/GoalStrictDirection.java` | 40 | `method getXOffset()` | getFrontOffsetX() | KEEP | `dx = direction.getXOffset();` |
| `api/java/baritone/api/pathing/goals/GoalStrictDirection.java` | 41 | `method getZOffset()` | getFrontOffsetZ() | KEEP | `dz = direction.getZOffset();` |
| `api/java/baritone/api/utils/SettingsUtil.java` | 107 | `variable gameDir` | mcDataDir (klient; datoteka večinoma odpade) | ADAPT | `return Minecraft.getMinecraft().gameDir.toPath().resolve("baritone").resolve(nam` |
| `api/java/baritone/api/utils/RayTraceUtils.java` | 54 | `argumenti: double,double,double` | addVector(x,y,z) | ADAPT | `Vec3d end = start.add(` |
| `api/java/baritone/api/utils/BlockUtils.java` | 32 | `method getPath()` | getResourcePath() | ADAPT | `String name = loc.getPath(); // normally, only write the part after the minecraf` |
| `api/java/baritone/api/utils/BlockUtils.java` | 33 | `method getNamespace()` | getResourceDomain() | ADAPT | `if (!loc.getNamespace().equals("minecraft")) {` |
| `api/java/baritone/api/utils/RotationUtils.java` | 205 | `argumenti: double,double,double` | addVector(x,y,z) | ADAPT | `possibleRotation = reachableOffset(ctx, pos, new Vec3d(pos).add(xDiff, yDiff, zD` |
| `main/java/baritone/Baritone.java` | 97 | `variable gameDir` | mcDataDir (klient; datoteka večinoma odpade) | REWRITE | `this.directory = mc.gameDir.toPath().resolve("baritone");` |
| `main/java/baritone/event/GameEventHandler.java` | 102 | `method getChunk(int,int)` | getChunkFromChunkCoords(x,z) — na strežniku NE (D-012) | ADAPT | `Chunk chunk = world.getChunk(event.getX(), event.getZ());` |
| `main/java/baritone/event/GameEventHandler.java` | 122 | `method getChunk(int,int)` | getChunkFromChunkCoords(x,z) — na strežniku NE (D-012) | ADAPT | `worldData.getCachedWorld().queueForPacking(world.getChunk(pos.x, pos.z));` |
| `main/java/baritone/process/BuilderProcess.java` | 407 | `method getXOffset()` | getFrontOffsetX() | DROP | `double x = side.getXOffset() == 0 ? 0.5 : (1 + side.getXOffset()) / 2D;` |
| `main/java/baritone/process/BuilderProcess.java` | 407 | `method getXOffset()` | getFrontOffsetX() | DROP | `double x = side.getXOffset() == 0 ? 0.5 : (1 + side.getXOffset()) / 2D;` |
| `main/java/baritone/process/BuilderProcess.java` | 408 | `method getZOffset()` | getFrontOffsetZ() | DROP | `double z = side.getZOffset() == 0 ? 0.5 : (1 + side.getZOffset()) / 2D;` |
| `main/java/baritone/process/BuilderProcess.java` | 408 | `method getZOffset()` | getFrontOffsetZ() | DROP | `double z = side.getZOffset() == 0 ? 0.5 : (1 + side.getZOffset()) / 2D;` |
| `main/java/baritone/process/FarmProcess.java` | 288 | `argumenti: double,double,double` | addVector(x,y,z) | DROP | `Vec3d faceCenter = new Vec3d(pos).add(0.5, 0.5, 0.5).add(new Vec3d(dir.getDirect` |
| `main/java/baritone/cache/WorldProvider.java` | 135 | `variable gameDir` | mcDataDir (klient; datoteka večinoma odpade) | REWRITE | `if (worldDir.relativize(ctx.minecraft().gameDir.toPath()).getNameCount() != 2) {` |
| `main/java/baritone/pathing/movement/movements/MovementTraverse.java` | 136 | `method getXOffset()` | getFrontOffsetX() | KEEP | `int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_U` |
| `main/java/baritone/pathing/movement/movements/MovementTraverse.java` | 137 | `method getYOffset()` | getFrontOffsetY() | KEEP | `int againstY = y - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_U` |
| `main/java/baritone/pathing/movement/movements/MovementTraverse.java` | 138 | `method getZOffset()` | getFrontOffsetZ() | KEEP | `int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_U` |
| `main/java/baritone/pathing/movement/movements/MovementAscend.java` | 81 | `method getXOffset()` | getFrontOffsetX() | KEEP | `int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_U` |
| `main/java/baritone/pathing/movement/movements/MovementAscend.java` | 82 | `method getYOffset()` | getFrontOffsetY() | KEEP | `int againstY = y + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i]` |
| `main/java/baritone/pathing/movement/movements/MovementAscend.java` | 83 | `method getZOffset()` | getFrontOffsetZ() | KEEP | `int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_U` |
| `main/java/baritone/pathing/movement/movements/MovementAscend.java` | 228 | `method byHorizontalIndex(int)` | getHorizontal(i) | KEEP | `BetterBlockPos check = startUp.offset(EnumFacing.byHorizontalIndex(i));` |
| `main/java/baritone/process/BackfillProcess.java` | 60 | `method getChunk(BlockPos)` | getChunkFromBlockCoords(pos) — na strežniku NE (D-012) | DROP | `if (ctx.world().getChunk(pos) instanceof EmptyChunk // ctx.world().getBlockState` |
| `main/java/baritone/command/defaults/SetCommand.java` | 228 | `variable gameDir` | mcDataDir (klient; datoteka večinoma odpade) | DROP | `return RelativeFile.tabComplete(args, Minecraft.getMinecraft().gameDir.toPath().` |
| `main/java/baritone/command/defaults/BuildCommand.java` | 42 | `variable gameDir` | mcDataDir (klient; datoteka večinoma odpade) | DROP | `this.schematicsDir = new File(baritone.getPlayerContext().minecraft().gameDir, "` |
| `main/java/baritone/command/defaults/ExploreFilterCommand.java` | 44 | `variable gameDir` | mcDataDir (klient; datoteka večinoma odpade) | DROP | `File file = args.getDatatypePost(RelativeFile.INSTANCE, ctx.minecraft().gameDir.` |
| `main/java/baritone/command/defaults/FindCommand.java` | 59 | `method getPath()` | getResourcePath() | DROP | `Block.REGISTRY.getNameForObject(block).getPath(),` |
| `main/java/baritone/command/defaults/FindCommand.java` | 66 | `map(BetterBlockPos::new)` | (ukaz odpade) | DROP | `.map(BetterBlockPos::new)` |
| `main/java/baritone/pathing/movement/movements/MovementParkour.java` | 71 | `method getXOffset()` | getFrontOffsetX() | KEEP | `int xDiff = dir.getXOffset();` |
| `main/java/baritone/pathing/movement/movements/MovementParkour.java` | 72 | `method getZOffset()` | getFrontOffsetZ() | KEEP | `int zDiff = dir.getZOffset();` |
| `main/java/baritone/pathing/movement/movements/MovementParkour.java` | 187 | `method getXOffset()` | getFrontOffsetX() | KEEP | `int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_U` |
| `main/java/baritone/pathing/movement/movements/MovementParkour.java` | 188 | `method getYOffset()` | getFrontOffsetY() | KEEP | `int againstY = y - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_U` |
| `main/java/baritone/pathing/movement/movements/MovementParkour.java` | 189 | `method getZOffset()` | getFrontOffsetZ() | KEEP | `int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_U` |
| `main/java/baritone/utils/BlockStateInterfaceAccessWrapper.java` | 37 | `isSideSolid(BlockPos,EnumFacing,boolean)` | implementiraj (Forge doda v IBlockAccess) | ADAPT | `public final class BlockStateInterfaceAccessWrapper implements IBlockAccess {` |
