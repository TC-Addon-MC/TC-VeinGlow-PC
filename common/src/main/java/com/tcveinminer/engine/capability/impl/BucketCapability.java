package com.tcveinminer.engine.capability.impl;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.ItemActionCapability;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Detects empty bucket targeting a fluid source block.
 * <p>
 * Priority 400 — checked after HoeCapability.
 * If player holds an empty bucket and targets a still fluid source → FLUID_SCOOP action.
 */
public final class BucketCapability implements ItemActionCapability {

    @Override
    public boolean canPerform(ItemStack stack, BlockState targetState, Player player) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() != Items.BUCKET) return false;

        // Target must be a fluid source block
        return targetState.getBlock() instanceof LiquidBlock
            && targetState.getFluidState().isSource();
    }

    @Override
    public int priority() {
        return 400;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.FLUID_SCOOP;
    }
}
