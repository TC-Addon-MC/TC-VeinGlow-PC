package com.tcveinminer.engine.capability.impl;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.ItemActionCapability;
import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;

/**
 * Detects mature crops that can be harvested.
 * <p>
 * Priority 500 — checked after BucketCapability.
 * Works with any item or bare hand — if target is a mature crop → CROP_HARVEST action.
 */
public final class HarvestCapability implements ItemActionCapability {

    @Override
    public boolean canPerform(ItemStack stack, BlockState targetState, PlayerEntity player) {
        return isMatureCrop(targetState);
    }

    @Override
    public int priority() {
        return 500;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CROP_HARVEST;
    }

    /**
     * Check if the given block state is a mature crop ready for harvest.
     */
    public static boolean isMatureCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMature(state);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.get(Properties.AGE_3) == 3;
        }
        if (state.getBlock() instanceof CocoaBlock) {
            return state.get(Properties.AGE_2) == 2;
        }
        return false;
    }
}
