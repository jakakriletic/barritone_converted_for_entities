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

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.harness.BootstrapOnce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * M11.4 (D-034): porabnikov {@code IItemHandler} je edini vir resnice. Roke vzamejo orodje ali blok
 * za eno dejanje in ga v istem ticku vrnejo; kar ne gre nazaj (poln inventar), gre v svet — nič se
 * ne izgubi in nič ne podvoji (M11 A4).
 */
public class WorkerInventoryTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    /** Po Bootstrapu (statično polje bi se inicializiralo pred {@code @ClassRule}). */
    private static List<Item> throwaway() {
        return Arrays.asList(Item.getItemFromBlock(Blocks.DIRT), Item.getItemFromBlock(Blocks.COBBLESTONE));
    }

    private static int total(IItemHandler h, List<ItemStack> spilled) {
        int n = 0;
        for (int i = 0; i < h.getSlots(); i++) {
            n += h.getStackInSlot(i).getCount();
        }
        for (ItemStack s : spilled) {
            n += s.getCount();
        }
        return n;
    }

    @Test
    public void bestToolForStoneIsPickaxeAndGoesBack() {
        ItemStackHandler h = new ItemStackHandler(4);
        h.setStackInSlot(0, new ItemStack(Items.WOODEN_SHOVEL));
        h.setStackInSlot(1, new ItemStack(Items.STONE_PICKAXE));
        h.setStackInSlot(2, new ItemStack(Items.WOODEN_PICKAXE));
        WorkerInventory inv = new WorkerInventory(h, WorkerInventoryTest::throwaway);
        ItemStack tool = inv.takeTool(Blocks.STONE.getDefaultState());
        assertSame(Items.STONE_PICKAXE, tool.getItem());
        assertTrue("vzeto iz inventarja", h.getStackInSlot(1).isEmpty());
        List<ItemStack> spilled = new ArrayList<>();
        assertTrue(inv.putBack(tool, spilled::add));
        assertEquals(3, total(h, spilled));
        assertTrue(spilled.isEmpty());
    }

    @Test
    public void noUsefulToolMeansEmptyHand() {
        ItemStackHandler h = new ItemStackHandler(2);
        h.setStackInSlot(0, new ItemStack(Items.WOODEN_PICKAXE));
        WorkerInventory inv = new WorkerInventory(h, WorkerInventoryTest::throwaway);
        assertTrue("za zemljo kramp ni boljši od roke", inv.takeTool(Blocks.DIRT.getDefaultState()).isEmpty());
        assertSame(Items.WOODEN_PICKAXE, h.getStackInSlot(0).getItem());
        assertSame(Items.WOODEN_PICKAXE, inv.takeTool(Blocks.STONE.getDefaultState()).getItem());
    }

    @Test
    public void throwawayTakesOneAcceptableBlock() {
        ItemStackHandler h = new ItemStackHandler(3);
        h.setStackInSlot(0, new ItemStack(Blocks.PLANKS, 10)); // ni na seznamu
        h.setStackInSlot(1, new ItemStack(Blocks.COBBLESTONE, 5));
        WorkerInventory inv = new WorkerInventory(h, WorkerInventoryTest::throwaway);
        assertTrue(inv.hasThrowaway());
        ItemStack b = inv.takeThrowaway();
        assertEquals(1, b.getCount());
        assertSame(Item.getItemFromBlock(Blocks.COBBLESTONE), b.getItem());
        assertEquals(4, h.getStackInSlot(1).getCount());
        assertEquals(10, h.getStackInSlot(0).getCount());
    }

    @Test
    public void noThrowaway() {
        ItemStackHandler h = new ItemStackHandler(1);
        h.setStackInSlot(0, new ItemStack(Blocks.PLANKS, 10));
        WorkerInventory inv = new WorkerInventory(h, WorkerInventoryTest::throwaway);
        assertFalse(inv.hasThrowaway());
        assertTrue(inv.takeThrowaway().isEmpty());
    }

    /** Drop, ki ne gre v poln inventar, gre v svet; skupna vsota ostane. */
    @Test
    public void fullInventorySpillsRemainder() {
        ItemStackHandler h = new ItemStackHandler(1);
        h.setStackInSlot(0, new ItemStack(Blocks.COBBLESTONE, 63));
        WorkerInventory inv = new WorkerInventory(h, WorkerInventoryTest::throwaway);
        List<ItemStack> spilled = new ArrayList<>();
        assertFalse(inv.putBack(new ItemStack(Blocks.COBBLESTONE, 3), spilled::add));
        assertEquals(64, h.getStackInSlot(0).getCount());
        assertEquals(1, spilled.size());
        assertEquals(2, spilled.get(0).getCount());
        assertEquals(66, total(h, spilled));
    }

    /** Orodje vzeto, slot se med dejanjem napolni z dropom — orodje ne sme izginiti. */
    @Test
    public void toolNeverLostWhenSlotFillsDuringAction() {
        ItemStackHandler h = new ItemStackHandler(1);
        h.setStackInSlot(0, new ItemStack(Items.STONE_PICKAXE));
        WorkerInventory inv = new WorkerInventory(h, WorkerInventoryTest::throwaway);
        ItemStack tool = inv.takeTool(Blocks.STONE.getDefaultState());
        List<ItemStack> spilled = new ArrayList<>();
        assertTrue(inv.putBack(new ItemStack(Blocks.COBBLESTONE, 1), spilled::add)); // drop zasede prazen slot
        assertFalse(inv.putBack(tool, spilled::add));
        assertEquals(1, spilled.size());
        assertSame(Items.STONE_PICKAXE, spilled.get(0).getItem());
        assertEquals(2, total(h, spilled));
    }

    @Test
    public void emptyPutBackIsNoop() {
        ItemStackHandler h = new ItemStackHandler(1);
        WorkerInventory inv = new WorkerInventory(h, WorkerInventoryTest::throwaway);
        List<ItemStack> spilled = new ArrayList<>();
        assertTrue(inv.putBack(ItemStack.EMPTY, spilled::add));
        assertTrue(spilled.isEmpty());
    }
}
