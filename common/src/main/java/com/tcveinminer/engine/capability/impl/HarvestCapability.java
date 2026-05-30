package com.tcveinminer.engine.capability.impl;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.ItemActionCapability;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Detects mature crops that can be harvested.
 * <p>
 * Priority 500 — checked after BucketCapability.
 * Works with any item or bare hand — if target is a mature crop → CROP_HARVEST action.
 */
public final class HarvestCapability implements ItemActionCapability {

    @Override
    public boolean canPerform(ItemStack stack, BlockState targetState, Player player) {
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
            return state.getValue(net.minecraft.world.level.block.CropBlock.AGE) == crop.getMaxAge();
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(BlockStateProperties.AGE_3) == 3;
        }
        if (state.getBlock() instanceof CocoaBlock) {
            return state.getValue(BlockStateProperties.AGE_2) == 2;
        }
        return false;
    }
}
