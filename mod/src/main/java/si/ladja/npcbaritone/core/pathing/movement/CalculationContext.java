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

package si.ladja.npcbaritone.core.pathing.movement;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Enchantments;
import net.minecraft.util.math.BlockPos;
import si.ladja.npcbaritone.core.api.IBaritone;
import si.ladja.npcbaritone.core.api.Settings;
import si.ladja.npcbaritone.core.api.pathing.movement.ActionCosts;
import si.ladja.npcbaritone.core.pathing.precompute.PrecomputedData;
import si.ladja.npcbaritone.core.utils.BlockStateInterface;
import si.ladja.npcbaritone.core.utils.ToolSet;
import si.ladja.npcbaritone.core.utils.pathing.BetterWorldBorder;
import si.ladja.npcbaritone.core.world.ChunkSnapshot;

import java.util.ArrayList;
import java.util.List;

import static si.ladja.npcbaritone.core.api.pathing.movement.ActionCosts.COST_INF;

/**
 * @author Brady
 * @since 8/7/2018
 */
public class CalculationContext {

    public final boolean safeForThreadedUse;
    public final IBaritone baritone;
    /** NPC Baritone (D-016): nastavitve, iz katerih je kontekst narejen (profil instance = {@code bsi.settings}). */
    public final Settings settings;
    public final BlockStateInterface bsi;
    public final ToolSet toolSet;
    public final boolean hasWaterBucket;
    public final boolean hasThrowaway;
    public final boolean canSprint;
    protected final double placeBlockCost; // protected because you should call the function instead
    public final boolean allowBreak;
    public final List<Block> allowBreakAnyway;
    public final boolean allowParkour;
    public final boolean allowParkourPlace;
    public final boolean allowJumpAt256;
    public final boolean allowParkourAscend;
    public final boolean assumeWalkOnWater;
    public boolean allowFallIntoLava;
    public final int frostWalker;
    public final boolean allowDiagonalDescend;
    public final boolean allowDiagonalAscend;
    public final boolean allowDownward;
    public int minFallHeight;
    public int maxFallHeightNoWater;
    public final int maxFallHeightBucket;
    public final double waterWalkSpeed;
    public final double breakBlockAdditionalCost;
    public double backtrackCostFavoringCoefficient;
    public double jumpPenalty;
    public final double walkOnWaterOnePenalty;
    /** D-040: {@code npcWaterPenalty} (CNPC U5). */
    public final double npcWaterPenalty;
    public final BetterWorldBorder worldBorder;

    public final PrecomputedData precomputedData;

    /** M8.1: velikost entitete, za katero se išče pot (Automatone {@code 324bd259}). */
    public final EntitySize size;
    /** Dodatni stolpci na vsako stran (0 za širino ≤ 1). */
    public final int requiredSideSpace;
    /** Višina v blokih. */
    public final int height;
    /**
     * Premiki uporabijo splošno (size-aware) vejo. Za standardno velikost je false in premiki
     * računajo kot upstream; testi ga lahko vsilijo, da dokažejo enakost obeh vej.
     */
    public final boolean sizeAware;

    public CalculationContext(IBaritone baritone) {
        this(baritone, false);
    }

    public CalculationContext(IBaritone baritone, boolean forUseOnAnotherThread) {
        this(baritone, forUseOnAnotherThread ? ChunkSnapshot.Bounds.ALL : null);
    }

    /**
     * @param snapshot null = BSI nad živimi chunki (glavna nit); sicer omejena kopija (D-013),
     *                 kontekst je varen za iskalno nit.
     */
    public CalculationContext(IBaritone baritone, ChunkSnapshot.Bounds snapshot) {
        this(baritone, baritone.getEntityContext().entity(),
                new BlockStateInterface(baritone.getEntityContext(), snapshot), snapshot != null);
    }

    /**
     * Headless (golden testi, D-022): brez entitete in brez instance. Iskanje poti deluje;
     * premikov iz take poti ni mogoče izvajati.
     */
    public static CalculationContext headless(BlockStateInterface bsi) {
        return headless(bsi, EntitySize.STANDARD);
    }

    /** Headless za dano velikost entitete (M8.8 golden testi). */
    public static CalculationContext headless(BlockStateInterface bsi, EntitySize size) {
        return new CalculationContext(null, null, bsi, true, size, false);
    }

    /**
     * Headless s splošno vejo premikov tudi za standardno velikost (test enakosti vej, M8).
     */
    public static CalculationContext headlessSizeAware(BlockStateInterface bsi, EntitySize size) {
        return new CalculationContext(null, null, bsi, true, size, true);
    }

    /**
     * @param entity lahko null (headless): brez orodja, očarov in učinkov
     */
    public CalculationContext(IBaritone baritone, EntityLivingBase entity, BlockStateInterface bsi, boolean forUseOnAnotherThread) {
        this(baritone, entity, bsi, forUseOnAnotherThread, EntitySize.of(entity), false);
    }

    private CalculationContext(IBaritone baritone, EntityLivingBase entity, BlockStateInterface bsi, boolean forUseOnAnotherThread,
                               EntitySize size, boolean forceSizeAware) {
        Settings settings = bsi.settings;
        this.size = size;
        this.requiredSideSpace = size.sideSpace;
        this.height = size.heightBlocks;
        this.sizeAware = forceSizeAware || !size.isStandard();
        this.precomputedData = new PrecomputedData(settings);
        this.safeForThreadedUse = forUseOnAnotherThread;
        this.baritone = baritone;
        this.settings = settings;
        this.bsi = bsi;
        this.toolSet = new ToolSet(entity, settings);
        // D-015: brez inventarja ni metnih blokov ne vedra
        this.hasThrowaway = false;
        this.hasWaterBucket = false;
        // Mobi nimajo lakote; igralčeva meja 6 hrane odpade
        this.canSprint = settings.allowSprint.value;
        this.placeBlockCost = settings.blockPlacementPenalty.value;
        this.allowBreak = settings.allowBreak.value;
        this.allowBreakAnyway = new ArrayList<>(settings.allowBreakAnyway.value);
        this.allowParkour = settings.allowParkour.value;
        this.allowParkourPlace = settings.allowParkourPlace.value;
        this.allowJumpAt256 = settings.allowJumpAt256.value;
        this.allowParkourAscend = settings.allowParkourAscend.value;
        this.assumeWalkOnWater = settings.assumeWalkOnWater.value;
        this.allowFallIntoLava = false; // Super secret internal setting (upstream: za letenje; pri NPC vedno false)
        this.frostWalker = entity == null ? 0 : EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.FROST_WALKER, entity);
        this.allowDiagonalDescend = settings.allowDiagonalDescend.value;
        this.allowDiagonalAscend = settings.allowDiagonalAscend.value;
        this.allowDownward = settings.allowDownward.value;
        this.minFallHeight = 3; // Minimum fall height used by MovementFall
        this.maxFallHeightNoWater = settings.maxFallHeightNoWater.value;
        this.maxFallHeightBucket = settings.maxFallHeightBucket.value;
        int depth = entity == null ? 0 : EnchantmentHelper.getDepthStriderModifier(entity);
        if (depth > 3) {
            depth = 3;
        }
        float mult = depth / 3.0F;
        this.waterWalkSpeed = ActionCosts.WALK_ONE_IN_WATER_COST * (1 - mult) + ActionCosts.WALK_ONE_BLOCK_COST * mult;
        this.breakBlockAdditionalCost = settings.blockBreakAdditionalPenalty.value;
        this.backtrackCostFavoringCoefficient = settings.backtrackCostFavoringCoefficient.value;
        this.jumpPenalty = settings.jumpPenalty.value;
        this.walkOnWaterOnePenalty = settings.walkOnWaterOnePenalty.value;
        this.npcWaterPenalty = settings.npcWaterPenalty.value;
        // why cache these things here, why not let the movements just get directly from settings?
        // because if some movements are calculated one way and others are calculated another way,
        // then you get a wildly inconsistent path that isn't optimal for either scenario.
        this.worldBorder = bsi.worldBorder;
    }

    public final IBaritone getBaritone() {
        return baritone;
    }

    public IBlockState get(int x, int y, int z) {
        return bsi.get0(x, y, z); // laughs maniacally
    }

    public boolean isLoaded(int x, int z) {
        return bsi.isLoaded(x, z);
    }

    public IBlockState get(BlockPos pos) {
        return get(pos.getX(), pos.getY(), pos.getZ());
    }

    public Block getBlock(int x, int y, int z) {
        return get(x, y, z).getBlock();
    }

    public double costOfPlacingAt(int x, int y, int z, IBlockState current) {
        if (!hasThrowaway) { // only true if allowPlace is true, see constructor
            return COST_INF;
        }
        if (isPossiblyProtected(x, y, z)) {
            return COST_INF;
        }
        if (!worldBorder.canPlaceAt(x, z)) {
            return COST_INF;
        }
        return placeBlockCost;
    }

    public double breakCostMultiplierAt(int x, int y, int z, IBlockState current) {
        if (!allowBreak && !allowBreakAnyway.contains(current.getBlock())) {
            return COST_INF;
        }
        if (isPossiblyProtected(x, y, z)) {
            return COST_INF;
        }
        return 1;
    }

    public double placeBucketCost() {
        return placeBlockCost; // shrug
    }

    public boolean isPossiblyProtected(int x, int y, int z) {
        // TODO more protection logic here; see #220
        return false;
    }
}
