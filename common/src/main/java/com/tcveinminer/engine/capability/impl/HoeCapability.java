package com.tcveinminer.engine.capability.impl;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.ItemActionCapability;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;

/**
 * Detects hoe items targeting tillable blocks (dirt, grass, coarse dirt, etc.).
 * <p>
 * Priority 300 — checked after PlantCapability.
 * If player holds a hoe and targets a tillable block → HOE_TILL action.
 */
public final class HoeCapability implements ItemActionCapability {

    @Override
    public boolean canPerform(ItemStack stack, BlockState targetState, PlayerEntity player) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof HoeItem)) return false;

        // Check if target is a tillable block
        Block target = targetState.getBlock();
        return target == Blocks.DIRT
            || target == Blocks.GRASS_BLOCK
            || target == Blocks.DIRT_PATH
            || target == Blocks.COARSE_DIRT
            || target == Blocks.ROOTED_DIRT;
    }

    @Override
    public int priority() {
        return 300;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.HOE_TILL;
    }
}
