package com.tcveinminer.engine.capability.impl;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.ItemActionCapability;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.tags.BlockTags;

/**
 * Detects tool + block combinations that produce vanilla interactions:
 * <ul>
 *   <li>Axe + log → strip log</li>
 *   <li>Shovel + grass → path</li>
 *   <li>Axe + copper → de-wax/de-oxidize</li>
 * </ul>
 * <p>
 * Priority 600 — lowest priority, checked last before vanilla fallback.
 */
public final class InteractBlockCapability implements ItemActionCapability {

    @Override
    public boolean canPerform(ItemStack stack, BlockState targetState, Player player) {
        if (stack.isEmpty()) return false;

        // Axe interactions: strip log, scrape copper, de-wax
        if (stack.getItem() instanceof AxeItem) {
            Block target = targetState.getBlock();
            // Strippable logs
            if (targetState.is(BlockTags.LOGS)) return true;
            // Copper blocks (oxidized/waxed)
            if (isCopper(target)) return true;
            return false;
        }

        // Shovel interactions: path grass block
        if (stack.getItem() instanceof ShovelItem) {
            Block target = targetState.getBlock();
            return target == Blocks.GRASS_BLOCK
                || target == Blocks.DIRT
                || target == Blocks.COARSE_DIRT
                || target == Blocks.PODZOL
                || target == Blocks.MYCELIUM
                || target == Blocks.ROOTED_DIRT;
        }

        return false;
    }

    @Override
    public int priority() {
        return 600;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.INTERACT_BLOCK;
    }

    private static boolean isCopper(Block block) {
        return block == Blocks.COPPER_BLOCK
            || block == Blocks.EXPOSED_COPPER
            || block == Blocks.WEATHERED_COPPER
            || block == Blocks.OXIDIZED_COPPER
            || block == Blocks.WAXED_COPPER_BLOCK
            || block == Blocks.WAXED_EXPOSED_COPPER
            || block == Blocks.WAXED_WEATHERED_COPPER
            || block == Blocks.WAXED_OXIDIZED_COPPER
            || block == Blocks.CUT_COPPER
            || block == Blocks.EXPOSED_CUT_COPPER
            || block == Blocks.WEATHERED_CUT_COPPER
            || block == Blocks.OXIDIZED_CUT_COPPER;
    }
}
