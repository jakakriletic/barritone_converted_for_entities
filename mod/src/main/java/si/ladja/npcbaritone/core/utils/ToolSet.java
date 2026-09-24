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

import si.ladja.npcbaritone.core.api.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A cached list of the best tools on the hotbar for any block
 *
 * @author Avery, Brady, leijurv
 */
public class ToolSet {

    /**
     * A cache mapping a {@link Block} to how long it will take to break
     * with this toolset, given the optimum tool is used.
     */
    private final Map<Block, Double> breakStrengthCache;

    /**
     * My buddy leijurv owned me so we have this to not create a new lambda instance.
     */
    private final Function<Block, Double> backendCalculation;

    /** NPC Baritone: entiteta ali null (headless iskanje: prazna roka, brez učinkov). */
    private final EntityLivingBase player;

    private final Settings settings;

    public ToolSet(EntityLivingBase player, Settings settings) {
        breakStrengthCache = new HashMap<>();
        this.player = player;
        this.settings = settings;

        if (settings.considerPotionEffects.value) {
            double amplifier = potionAmplifier();
            Function<Double, Double> amplify = x -> amplifier * x;
            backendCalculation = amplify.compose(this::getBestDestructionTime);
        } else {
            backendCalculation = this::getBestDestructionTime;
        }
    }

    /**
     * Using the best tool on the hotbar, how fast we can mine this block
     *
     * @param state the blockstate to be mined
     * @return the speed of how fast we'll mine it. 1/(time in ticks)
     */
    public double getStrVsBlock(IBlockState state) {
        return breakStrengthCache.computeIfAbsent(state.getBlock(), backendCalculation);
    }

    public boolean hasSilkTouch(ItemStack stack) {
        return EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, stack) > 0;
    }

    /**
     * Calculate which tool on the hotbar is best for mining, depending on an override setting,
     * related to auto tool movement cost, it will either return current selected slot, or the best slot.
     *
     * @param b the blockstate to be mined
     * @return An int containing the index in the tools array that worked best
     */

    public int getBestSlot(Block b, boolean preferSilkTouch) {
        return getBestSlot(b, preferSilkTouch, false);
    }

    public int getBestSlot(Block b, boolean preferSilkTouch, boolean pathingCalculation) {
        // NPC Baritone (D-015): entiteta nima hotbara; orodje je vedno tisto v glavni roki.
        return 0;
    }

    private ItemStack heldItem() {
        return player == null ? ItemStack.EMPTY : player.getHeldItemMainhand();
    }

    /**
     * Calculate how effectively a block can be destroyed
     *
     * @param b the blockstate to be mined
     * @return A double containing the destruction ticks with the best tool
     */
    private double getBestDestructionTime(Block b) {
        ItemStack stack = heldItem();
        return calculateSpeedVsBlock(stack, b.getDefaultState()) * avoidanceMultiplier(b);
    }

    private double avoidanceMultiplier(Block b) {
        return settings.blocksToAvoidBreaking.value.contains(b) ? settings.avoidBreakingMultiplier.value : 1;
    }

    /**
     * Calculates how long would it take to mine the specified block given the best tool
     * in this toolset is used. A negative value is returned if the specified block is unbreakable.
     *
     * @param item  the item to mine it with
     * @param state the blockstate to be mined
     * @return how long it would take in ticks
     */
    public static double calculateSpeedVsBlock(ItemStack item, IBlockState state) {
        float hardness = state.getBlockHardness(null, null);
        if (hardness < 0) {
            return -1;
        }

        float speed = item.getDestroySpeed(state);
        if (speed > 1) {
            int effLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, item);
            if (effLevel > 0 && !item.isEmpty()) {
                speed += effLevel * effLevel + 1;
            }
        }

        speed /= hardness;
        if (state.getMaterial().isToolNotRequired() || (!item.isEmpty() && item.canHarvestBlock(state))) {
            return speed / 30;
        } else {
            return speed / 100;
        }
    }

    /**
     * Calculates any modifier to breaking time based on status effects.
     *
     * @return a double to scale block breaking speed.
     */
    private double potionAmplifier() {
        double speed = 1;
        if (player == null) {
            return speed;
        }
        if (player.isPotionActive(MobEffects.HASTE)) {
            speed *= 1 + (player.getActivePotionEffect(MobEffects.HASTE).getAmplifier() + 1) * 0.2;
        }
        if (player.isPotionActive(MobEffects.MINING_FATIGUE)) {
            switch (player.getActivePotionEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    speed *= 0.3;
                    break;
                case 1:
                    speed *= 0.09;
                    break;
                case 2:
                    speed *= 0.0027; // you might think that 0.09*0.3 = 0.027 so that should be next, that would make too much sense. it's 0.0027.
                    break;
                default:
                    speed *= 0.00081;
                    break;
            }
        }
        return speed;
    }
}
