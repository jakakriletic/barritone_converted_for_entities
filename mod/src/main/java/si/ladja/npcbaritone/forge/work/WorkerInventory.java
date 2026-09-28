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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import si.ladja.npcbaritone.core.utils.ToolSet;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * M11.4 (D-034): {@link IHandsInventory} nad porabnikovim {@code IItemHandler}, ki je edini vir
 * resnice. Orodje: največja hitrost po Baritonovi formuli ({@link ToolSet#calculateSpeedVsBlock}),
 * samo če je boljše od roke. Metni bloki: Baritonov {@code acceptableThrowawayItems} instance.
 */
public final class WorkerInventory implements IHandsInventory {

    private final IItemHandler handler;
    private final Supplier<? extends Collection<Item>> throwaway;

    public WorkerInventory(IItemHandler handler, Supplier<? extends Collection<Item>> throwaway) {
        this.handler = handler;
        this.throwaway = throwaway;
    }

    public IItemHandler handler() {
        return handler;
    }

    @Override
    public ItemStack takeTool(IBlockState state) {
        double best = ToolSet.calculateSpeedVsBlock(ItemStack.EMPTY, state);
        int slot = -1;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (s.isEmpty() || s.getMaxStackSize() != 1) {
                continue; // orodja se ne zlagajo; bloki in hrana niso orodje
            }
            double v = ToolSet.calculateSpeedVsBlock(s, state);
            if (v > best) {
                best = v;
                slot = i;
            }
        }
        return slot < 0 ? ItemStack.EMPTY : handler.extractItem(slot, 1, false);
    }

    @Override
    public ItemStack takeThrowaway() {
        int slot = throwawaySlot();
        return slot < 0 ? ItemStack.EMPTY : handler.extractItem(slot, 1, false);
    }

    @Override
    public boolean hasThrowaway() {
        return throwawaySlot() >= 0;
    }

    private int throwawaySlot() {
        Collection<Item> ok = throwaway.get();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty() && ok.contains(s.getItem()) && !handler.extractItem(i, 1, true).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean putBack(ItemStack stack, Consumer<ItemStack> spill) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack rest = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        if (rest.isEmpty()) {
            return true;
        }
        spill.accept(rest);
        return false;
    }
}
