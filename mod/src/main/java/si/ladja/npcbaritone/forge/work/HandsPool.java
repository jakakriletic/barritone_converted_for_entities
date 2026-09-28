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

import com.mojang.authlib.GameProfile;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * M11.3 (D-032, D-044): {@code FakePlayer} rok na svet in lastnika, z {@link HandsNetHandler}.
 * Ne {@code FakePlayerFactory}: ta hrani roke samo po profilu, s svetom ob prvem klicu, zato bi
 * worker po menjavi dimenzije rušil v napačnem svetu. Svet se ob razložitvi pobriše.
 *
 * <p>Zajem dropov (D-034): med {@code tryHarvestBlock} rok se dropi iz
 * {@code HarvestDropsEvent} (najnižja prioriteta — drugi modi jih najprej spremenijo) preberejo z
 * vanilla verjetnostjo in odstranijo iz sveta; roke jih nato vrnejo v inventar.
 */
public final class HandsPool {

    public static final HandsPool INSTANCE = new HandsPool();

    private final Map<World, Map<UUID, FakePlayer>> byWorld = new HashMap<>();
    @Nullable
    private FakePlayer capturing;
    private final List<ItemStack> captured = new ArrayList<>();

    private HandsPool() {
    }

    public synchronized FakePlayer get(WorldServer world, @Nullable GameProfile owner) {
        GameProfile id = HandsIdentity.forOwner(owner);
        return byWorld.computeIfAbsent(world, w -> new HashMap<>()).computeIfAbsent(id.getId(), k -> {
            FakePlayer f = new FakePlayer(world, id);
            HandsNetHandler.install(f);
            return f;
        });
    }

    @SubscribeEvent
    public synchronized void onWorldUnload(WorldEvent.Unload e) {
        byWorld.remove(e.getWorld());
    }

    /** Samo glavna nit, samo okoli enega {@code tryHarvestBlock}. */
    void beginCapture(FakePlayer hands) {
        capturing = hands;
        captured.clear();
    }

    List<ItemStack> endCapture() {
        capturing = null;
        List<ItemStack> out = new ArrayList<>(captured);
        captured.clear();
        return out;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrops(BlockEvent.HarvestDropsEvent e) {
        FakePlayer c = capturing;
        if (c == null || e.getHarvester() != c) {
            return;
        }
        for (ItemStack s : e.getDrops()) {
            if (!s.isEmpty() && e.getWorld().rand.nextFloat() <= e.getDropChance()) { // kot Block.dropBlockAsItemWithChance
                captured.add(s.copy());
            }
        }
        e.getDrops().clear();
    }
}
