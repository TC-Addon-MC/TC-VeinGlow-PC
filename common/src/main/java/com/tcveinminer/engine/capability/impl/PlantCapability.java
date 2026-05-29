package com.tcveinminer.engine.capability.impl;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.ItemActionCapability;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;

/**
 * Detects seed items that can be planted on farmland.
 * <p>
 * Priority 200 — checked after UseItemCapability but before HoeCapability.
 * If player holds seeds and targets farmland → PLANT action.
 */
public final class PlantCapability implements ItemActionCapability {

    @Override
    public boolean canPerform(ItemStack stack, BlockState targetState, PlayerEntity player) {
        if (stack.isEmpty()) return false;

        // Target must be farmland
        if (targetState.getBlock() != Blocks.FARMLAND) return false;

        // Check if the held item is a plantable seed
        Item item = stack.getItem();
        return item == Items.WHEAT_SEEDS
            || item == Items.BEETROOT_SEEDS
            || item == Items.CARROT
            || item == Items.POTATO
            || item == Items.MELON_SEEDS
            || item == Items.PUMPKIN_SEEDS
            || item == Items.TORCHFLOWER_SEEDS
            || item == Items.PITCHER_POD;
    }

    @Override
    public int priority() {
        return 200;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.PLANT;
    }
}
