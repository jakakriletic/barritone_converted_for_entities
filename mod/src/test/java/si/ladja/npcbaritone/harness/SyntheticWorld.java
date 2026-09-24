/*
 * This file is part of NPC Baritone.
 *
 * NPC Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NPC Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NPC Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package si.ladja.npcbaritone.harness;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * Sintetičen svet za headless teste (D-022, M0.8): chunki brez {@code World}, bloki
 * neposredno v {@link ExtendedBlockStorage}.
 *
 * <p>Oblika je namenoma enaka tisti, ki jo bere Baritonov {@code BlockStateInterface}:
 * {@code Long2ObjectMap<Chunk>} s ključem {@link ChunkPos#asLong(int, int)} in chunki z
 * {@code isLoaded() == true}. M1 posnetek chunkov (D-013) zgradi isto obliko iz strežnika.
 *
 * <p>Pozor (M0.9): {@link Chunk#getBlockState(int, int, int)} na chunku brez sveta vrže
 * {@code NullPointerException} (najprej prebere {@code world.getWorldType()}). Zato
 * {@link #get(int, int, int)} bere iz {@link ExtendedBlockStorage}, ne iz chunka.
 *
 * <p>Graditelj ustvari chunk ob prvem pisanju; {@link #ensureChunks} doda prazne (zrak)
 * chunke, ki štejejo kot naloženi. Vse, česar ni v mapi, je nenaloženo (D-014).
 */
public final class SyntheticWorld {

    static {
        BootstrapOnce.ensure(); // Blocks.* pred Bootstrapom vrže izjemo
    }

    private static final IBlockState AIR = Blocks.AIR.getDefaultState();

    private final Long2ObjectMap<Chunk> chunks = new Long2ObjectOpenHashMap<>();
    private final IBlockAccess access = new Access();

    public static SyntheticWorld create() {
        return new SyntheticWorld();
    }

    private SyntheticWorld() {
    }

    // ---------------------------------------------------------------- graditelj

    public SyntheticWorld set(int x, int y, int z, IBlockState state) {
        if (y < 0 || y >= 256) {
            throw new IllegalArgumentException("y out of range: " + y);
        }
        Chunk chunk = chunk(x >> 4, z >> 4);
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        ExtendedBlockStorage section = sections[y >> 4];
        if (section == Chunk.NULL_BLOCK_STORAGE) {
            section = new ExtendedBlockStorage(y >> 4 << 4, true);
            sections[y >> 4] = section;
        }
        section.set(x & 15, y & 15, z & 15, state);
        return this;
    }

    public SyntheticWorld set(BlockPos pos, IBlockState state) {
        return set(pos.getX(), pos.getY(), pos.getZ(), state);
    }

    /** Zapolni kvader (vključno z mejami, v poljubnem vrstnem redu). */
    public SyntheticWorld fill(int x1, int y1, int z1, int x2, int y2, int z2, IBlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
        return this;
    }

    /** Ravna plošča na višini {@code y}. */
    public SyntheticWorld floor(int x1, int z1, int x2, int z2, int y, IBlockState state) {
        return fill(x1, y, z1, x2, y, z2, state);
    }

    /** Navpičen zid od {@code yBase} navzgor, {@code height} blokov, med dvema točkama (os-poravnano). */
    public SyntheticWorld wall(int x1, int z1, int x2, int z2, int yBase, int height, IBlockState state) {
        if (x1 != x2 && z1 != z2) {
            throw new IllegalArgumentException("wall must be axis-aligned");
        }
        return fill(x1, yBase, z1, x2, yBase + height - 1, z2, state);
    }

    /**
     * Stopnice iz polnih blokov: stopnica {@code i} (0..count-1) stoji na
     * {@code (x, y, z) + dir * i} in je visoka {@code (i + 1) * rise} blokov nad {@code y - 1}.
     * S {@code rise} 1–3 dobimo terene "stopnice 1–3" iz D-022.
     */
    public SyntheticWorld stairs(int x, int y, int z, EnumFacing dir, int count, int rise, IBlockState state) {
        if (dir.getAxis() == EnumFacing.Axis.Y) {
            throw new IllegalArgumentException("stairs must be horizontal");
        }
        for (int i = 0; i < count; i++) {
            int sx = x + dir.getFrontOffsetX() * i;
            int sz = z + dir.getFrontOffsetZ() * i;
            fill(sx, y, sz, sx, y + (i + 1) * rise - 1, sz, state);
        }
        return this;
    }

    /** Prazni (zrak) naloženi chunki v pravokotniku chunk koordinat. */
    public SyntheticWorld ensureChunks(int cx1, int cz1, int cx2, int cz2) {
        for (int cx = Math.min(cx1, cx2); cx <= Math.max(cx1, cx2); cx++) {
            for (int cz = Math.min(cz1, cz2); cz <= Math.max(cz1, cz2); cz++) {
                chunk(cx, cz);
            }
        }
        return this;
    }

    // ---------------------------------------------------------------- branje

    /** Stanje bloka ali {@code null}, če chunk ni naložen. Izven višine sveta: zrak (kot Baritone). */
    public IBlockState get(int x, int y, int z) {
        Chunk chunk = chunks.get(ChunkPos.asLong(x >> 4, z >> 4));
        if (chunk == null) {
            return null;
        }
        if (y < 0 || y >= 256) {
            return AIR;
        }
        ExtendedBlockStorage section = chunk.getBlockStorageArray()[y >> 4];
        return section == Chunk.NULL_BLOCK_STORAGE ? AIR : section.get(x & 15, y & 15, z & 15);
    }

    public IBlockState get(BlockPos pos) {
        return get(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean isLoaded(int blockX, int blockZ) {
        return chunks.containsKey(ChunkPos.asLong(blockX >> 4, blockZ >> 4));
    }

    /** Chunki v obliki, ki jo pričakuje Baritonov {@code BlockStateInterface}. */
    public Long2ObjectMap<Chunk> chunks() {
        return chunks;
    }

    /**
     * {@link IBlockAccess} nad tem svetom z enakimi privzetki kot Baritonov
     * {@code BlockStateInterfaceAccessWrapper}: brez tile entitet, svetloba 0, biom gozd,
     * brez redstone moči, {@link WorldType#DEFAULT}. Nenaloženo se bere kot zrak.
     */
    public IBlockAccess access() {
        return access;
    }

    private Chunk chunk(int cx, int cz) {
        long key = ChunkPos.asLong(cx, cz);
        Chunk chunk = chunks.get(key);
        if (chunk == null) {
            chunk = new Chunk(null, cx, cz);
            chunk.markLoaded(true);
            chunks.put(key, chunk);
        }
        return chunk;
    }

    private final class Access implements IBlockAccess {

        @Override
        public TileEntity getTileEntity(BlockPos pos) {
            return null;
        }

        @Override
        public int getCombinedLight(BlockPos pos, int lightValue) {
            return 0;
        }

        @Override
        public IBlockState getBlockState(BlockPos pos) {
            IBlockState state = get(pos);
            return state == null ? AIR : state;
        }

        @Override
        public boolean isAirBlock(BlockPos pos) {
            return getBlockState(pos).getMaterial() == Material.AIR;
        }

        @Override
        public Biome getBiome(BlockPos pos) {
            return Biomes.FOREST;
        }

        @Override
        public int getStrongPower(BlockPos pos, EnumFacing direction) {
            return 0;
        }

        @Override
        public WorldType getWorldType() {
            return WorldType.DEFAULT;
        }

        @Override
        public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
            IBlockState state = get(pos); // kot Forge World: nenaloženo -> _default
            return state == null ? _default : state.isSideSolid(this, pos, side);
        }
    }
}
