/*
 * This file is part of Baritone.
 * Modified for NPC Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package si.ladja.npcbaritone.core.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import si.ladja.npcbaritone.core.api.utils.IEntityContext;
import si.ladja.npcbaritone.core.utils.pathing.BetterWorldBorder;
import si.ladja.npcbaritone.core.world.ChunkSnapshot;

/**
 * Wraps get for chunk caching capability
 *
 * <p>NPC Baritone (D-012, D-013, RAZISKAVA §4a): bere samo že naložene strežniške chunke
 * (neposredno iz {@link ExtendedBlockStorage}, ker {@code Chunk.getBlockState} rabi svet),
 * nikoli ne naloži ali generira chunka. Nenaloženo = zrak in {@link #isLoaded} = false;
 * A* se tam ustavi (D-014). Predpomnilnika regij ni več.
 *
 * @author leijurv
 */
public class BlockStateInterface {

    private final Long2ObjectMap<Chunk> loadedChunks;
    public final BlockPos.MutableBlockPos isPassableBlockPos;
    public final IBlockAccess access;
    public final BetterWorldBorder worldBorder;

    private Chunk prev = null;

    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    public BlockStateInterface(IEntityContext ctx) {
        this(ctx, false);
    }

    /**
     * @param copyLoadedChunks true za iskalno nit: kopija mape naloženih chunkov (D-013).
     *                         Kopija je omejena na {@code snapshot} pravokotnik, če je podan
     *                         prek {@link #BlockStateInterface(IEntityContext, ChunkSnapshot.Bounds)}.
     */
    public BlockStateInterface(IEntityContext ctx, boolean copyLoadedChunks) {
        this(ctx, copyLoadedChunks ? ChunkSnapshot.Bounds.ALL : null);
    }

    /**
     * @param bounds null = brez kopije (samo glavna nit), sicer kopija v mejah (iskalna nit)
     */
    public BlockStateInterface(IEntityContext ctx, ChunkSnapshot.Bounds bounds) {
        World world = ctx.world();
        if (!(world instanceof WorldServer)) {
            throw new IllegalStateException("NPC Baritone runs on the server world only (D-024)");
        }
        WorldServer server = (WorldServer) world;
        if (!server.getMinecraftServer().isCallingFromMinecraftThread()) {
            throw new IllegalStateException("BlockStateInterface must be created on the server thread");
        }
        Long2ObjectMap<Chunk> live = server.getChunkProvider().id2ChunkMap;
        this.loadedChunks = bounds == null ? live : ChunkSnapshot.copy(live, bounds);
        this.worldBorder = new BetterWorldBorder(world.getWorldBorder());
        this.isPassableBlockPos = new BlockPos.MutableBlockPos();
        this.access = new BlockStateInterfaceAccessWrapper(this, world);
    }

    /**
     * Headless / posnetek: bere samo iz podane mape (ključ {@link ChunkPos#asLong}).
     *
     * @param worldType svet za {@code IBlockAccess.getWorldType()}; lahko null (privzeto DEFAULT)
     */
    public BlockStateInterface(Long2ObjectMap<Chunk> chunks, WorldBorder border, IBlockAccess worldType) {
        this.loadedChunks = chunks;
        this.worldBorder = new BetterWorldBorder(border == null ? new WorldBorder() : border);
        this.isPassableBlockPos = new BlockPos.MutableBlockPos();
        this.access = new BlockStateInterfaceAccessWrapper(this, worldType);
    }

    public boolean worldContainsLoadedChunk(int blockX, int blockZ) {
        return loadedChunks.containsKey(ChunkPos.asLong(blockX >> 4, blockZ >> 4));
    }

    public static Block getBlock(IEntityContext ctx, BlockPos pos) { // won't be called from the pathing thread because the pathing thread doesn't make a single blockpos pog
        return get(ctx, pos).getBlock();
    }

    public static IBlockState get(IEntityContext ctx, BlockPos pos) {
        return new BlockStateInterface(ctx).get0(pos.getX(), pos.getY(), pos.getZ()); // immense iq
        // can't just do world().get because that doesn't work for out of bounds
        // and toBreak and stuff fails when the movement is instantiated out of load range but it's not able to BlockStateInterface.get what it's going to walk on
    }

    public IBlockState get0(BlockPos pos) {
        return get0(pos.getX(), pos.getY(), pos.getZ());
    }

    public IBlockState get0(int x, int y, int z) { // Mickey resigned

        // Invalid vertical position
        if (y < 0 || y >= 256) {
            return AIR;
        }

        Chunk cached = prev;
        // there's great cache locality in block state lookups
        // generally it's within each movement
        // if it's the same chunk as last time
        // we can just skip the mc.world.getChunk lookup
        // which is a Long2ObjectOpenHashMap.get
        // see issue #113
        if (cached == null || cached.x != x >> 4 || cached.z != z >> 4) {
            cached = loadedChunks.get(ChunkPos.asLong(x >> 4, z >> 4));
            if (cached == null || !cached.isLoaded()) {
                return AIR;
            }
            prev = cached;
        }
        ExtendedBlockStorage section = cached.getBlockStorageArray()[y >> 4];
        return section == Chunk.NULL_BLOCK_STORAGE ? AIR : section.get(x & 15, y & 15, z & 15);
    }

    public boolean isLoaded(int x, int z) {
        Chunk prevChunk = prev;
        if (prevChunk != null && prevChunk.x == x >> 4 && prevChunk.z == z >> 4) {
            return true;
        }
        prevChunk = loadedChunks.get(ChunkPos.asLong(x >> 4, z >> 4));
        if (prevChunk != null && prevChunk.isLoaded()) {
            prev = prevChunk;
            return true;
        }
        return false;
    }
}
