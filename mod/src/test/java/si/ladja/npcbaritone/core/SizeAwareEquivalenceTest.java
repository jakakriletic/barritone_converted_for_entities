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

package si.ladja.npcbaritone.core;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.junit.ClassRule;
import org.junit.Test;
import si.ladja.npcbaritone.core.api.NpcProfile;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.api.pathing.movement.ActionCosts;
import si.ladja.npcbaritone.core.pathing.movement.CalculationContext;
import si.ladja.npcbaritone.core.pathing.movement.EntitySize;
import si.ladja.npcbaritone.core.pathing.movement.Moves;
import si.ladja.npcbaritone.core.utils.BlockStateInterface;
import si.ladja.npcbaritone.core.utils.pathing.MutableMoveResult;
import si.ladja.npcbaritone.harness.BootstrapOnce;
import si.ladja.npcbaritone.harness.SyntheticWorld;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * M8: splošna (size-aware) veja premikov za standardno velikost 0,6 × 1,8 računa natanko
 * isto kot upstream veja. Naključni tereni z bloki, ki jih premiki posebej obravnavajo;
 * vsak premik iz vsakega položaja; dva profila (NPC in z rušenjem/parkourjem, da se
 * izvedejo tudi veje s ceno rušenja).
 *
 * <p>Test je dokaz, da M8 ne spremeni obnašanja pri standardni velikosti (CNPC size 3–5),
 * čeprav se veji v kodi razlikujeta.
 */
public class SizeAwareEquivalenceTest {

    @ClassRule
    public static final BootstrapOnce BOOTSTRAP = new BootstrapOnce();

    static {
        BootstrapOnce.ensure();
    }

    private static final IBlockState[] PALETTE = {
            Blocks.STONE.getDefaultState(),
            Blocks.DIRT.getDefaultState(),
            Blocks.SAND.getDefaultState(),
            Blocks.GRAVEL.getDefaultState(),
            Blocks.WATER.getDefaultState(),
            Blocks.LAVA.getDefaultState(),
            Blocks.SOUL_SAND.getDefaultState(),
            Blocks.LADDER.getDefaultState(),
            Blocks.VINE.getDefaultState(),
            Blocks.CACTUS.getDefaultState(),
            Blocks.MAGMA.getDefaultState(),
            Blocks.OAK_FENCE.getDefaultState(),
            Blocks.OAK_FENCE_GATE.getDefaultState(),
            Blocks.OAK_DOOR.getDefaultState(),
            Blocks.OAK_DOOR.getDefaultState().withProperty(BlockDoor.HALF, BlockDoor.EnumDoorHalf.UPPER),
            Blocks.STONE_SLAB.getDefaultState().withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.STONE).withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM),
            Blocks.STONE_SLAB.getDefaultState().withProperty(BlockStoneSlab.VARIANT, BlockStoneSlab.EnumType.STONE).withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP),
            Blocks.CARPET.getDefaultState(),
            Blocks.SNOW_LAYER.getDefaultState(),
            Blocks.WEB.getDefaultState(),
            Blocks.TALLGRASS.getDefaultState(),
            Blocks.GLASS.getDefaultState(),
            Blocks.FARMLAND.getDefaultState(),
            Blocks.WATERLILY.getDefaultState(),
    };

    private static SyntheticWorld randomWorld(long seed, double density) {
        Random r = new Random(seed);
        SyntheticWorld w = SyntheticWorld.create().ensureChunks(0, 0, 1, 1);
        w.floor(0, 0, 31, 31, 60, Blocks.STONE.getDefaultState());
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                // stolpci različnih višin (stopnice, padci) + naključni bloki
                int top = 60 + r.nextInt(4);
                for (int y = 61; y <= top; y++) {
                    w.set(x, y, z, Blocks.STONE.getDefaultState());
                }
                for (int y = 61; y < 70; y++) {
                    if (r.nextDouble() < density) {
                        w.set(x, y, z, PALETTE[r.nextInt(PALETTE.length)]);
                    }
                }
            }
        }
        return w;
    }

    @Test
    public void sizeAwareBranchEqualsUpstreamForStandardSize() {
        Settings npc = NpcProfile.create();
        Settings wild = NpcProfile.create();
        wild.allowBreak.value = true;
        wild.allowParkour.value = true;
        wild.allowDiagonalAscend.value = true;
        wild.allowDiagonalDescend.value = true;
        wild.allowDownward.value = true;
        long compared = 0;
        long finite = 0;
        for (long seed = 1; seed <= 6; seed++) {
            SyntheticWorld w = randomWorld(seed, seed % 2 == 0 ? 0.08 : 0.2);
            for (Settings settings : new Settings[]{npc, wild}) {
                CalculationContext upstream = CalculationContext.headless(new BlockStateInterface(w.chunks(), null, null, settings));
                CalculationContext sized = CalculationContext.headlessSizeAware(new BlockStateInterface(w.chunks(), null, null, settings), EntitySize.STANDARD);
                assertTrue(!upstream.sizeAware && sized.sizeAware);
                for (int x = 5; x < 27; x++) {
                    for (int z = 5; z < 27; z++) {
                        for (int y = 61; y < 68; y++) {
                            for (Moves m : Moves.values()) {
                                MutableMoveResult a = new MutableMoveResult();
                                MutableMoveResult b = new MutableMoveResult();
                                m.apply(upstream, x, y, z, a);
                                m.apply(sized, x, y, z, b);
                                String where = m + " @ " + x + "," + y + "," + z + " seed " + seed + (settings == wild ? " wild" : " npc");
                                boolean infA = a.cost >= ActionCosts.COST_INF;
                                boolean infB = b.cost >= ActionCosts.COST_INF;
                                assertEquals(where + " feasibility", infA, infB);
                                if (!infA) {
                                    assertEquals(where + " cost", a.cost, b.cost, 1e-9);
                                    assertEquals(where + " x", a.x, b.x);
                                    assertEquals(where + " y", a.y, b.y);
                                    assertEquals(where + " z", a.z, b.z);
                                    finite++;
                                }
                                compared++;
                            }
                        }
                    }
                }
            }
        }
        assertTrue("too few feasible moves compared: " + finite, finite > 20_000);
        assertTrue(compared > 500_000);
    }
}
