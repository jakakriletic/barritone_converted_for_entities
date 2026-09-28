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

package si.ladja.npcbaritone.forge.work;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import si.ladja.npcbaritone.api.work.WorkerSpec;
import si.ladja.npcbaritone.core.api.work.IWorkContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * M11.5 (D-033): posnetek workerja za eno iskanje. Nastane na glavni niti (ob gradnji
 * {@code CalculationContext}); iskalna nit bere samo polja, nespremenljivo območje in porabnikovo
 * dovoljenje (pogodba "varno za niti").
 *
 * <p>Spawn protection kot {@code DedicatedServer.isBlockProtected}: samo dedicated strežnik,
 * dimenzija 0, seznam OP-jev ni prazen, {@code max(|dx|, |dz|) <= spawn-protection}; roke niso OP.
 */
public final class WorkContextSnapshot implements IWorkContext {

    private final WorkerSpec spec;
    private final List<ItemStack> tools;
    private final boolean throwaway;
    private final int spawnX, spawnZ, spawnRadius;

    WorkContextSnapshot(WorkerSpec spec, List<ItemStack> tools, boolean throwaway, int spawnX, int spawnZ, int spawnRadius) {
        this.spec = spec;
        this.tools = Collections.unmodifiableList(tools);
        this.throwaway = throwaway;
        this.spawnX = spawnX;
        this.spawnZ = spawnZ;
        this.spawnRadius = spawnRadius;
    }

    static WorkContextSnapshot of(WorkerSpec spec, World world, Collection<Item> throwawayItems) {
        IItemHandler h = spec.inventory();
        List<ItemStack> tools = new ArrayList<>();
        for (int i = 0; i < h.getSlots(); i++) {
            ItemStack s = h.getStackInSlot(i);
            if (!s.isEmpty() && s.getMaxStackSize() == 1) {
                tools.add(s.copy());
            }
        }
        boolean throwaway = new WorkerInventory(h, () -> throwawayItems).hasThrowaway();
        int radius = -1;
        BlockPos spawn = BlockPos.ORIGIN;
        MinecraftServer server = world.getMinecraftServer();
        if (server != null && server.isDedicatedServer() && world.provider.getDimension() == 0
                && !server.getPlayerList().getOppedPlayers().isEmpty() && server.getSpawnProtectionSize() > 0) {
            radius = server.getSpawnProtectionSize();
            spawn = world.getSpawnPoint();
        }
        return new WorkContextSnapshot(spec, tools, throwaway, spawn.getX(), spawn.getZ(), radius);
    }

    private boolean spawnProtected(int x, int z) {
        return spawnRadius >= 0 && Math.max(Math.abs(x - spawnX), Math.abs(z - spawnZ)) <= spawnRadius;
    }

    @Override
    public boolean mayBreak(int x, int y, int z, IBlockState state) {
        return !spawnProtected(x, z) && WorkGuards.refuseBreak(spec, null, new BlockPos(x, y, z), state) == null;
    }

    @Override
    public boolean mayPlace(int x, int y, int z) {
        return !spawnProtected(x, z) && WorkGuards.refusePlace(spec, new BlockPos(x, y, z)) == null;
    }

    @Override
    public List<ItemStack> tools() {
        return tools;
    }

    @Override
    public boolean hasThrowaway() {
        return throwaway;
    }
}
