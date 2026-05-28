package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class PlantAction implements BlockAction {

    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.InteractContext ic)) return false;

        BlockPos plantPos = pos.up();
        if (!world.getBlockState(plantPos).isAir()) {
            return false;
        }

        Hand hand = ic.getInteractHand();
        ItemStack seedStack = player.getStackInHand(hand);
        if (seedStack.isEmpty()) return false;

        if (seedStack.getItem() instanceof BlockItem blockItem) {
            BlockState plantState = blockItem.getBlock().getDefaultState();
            world.setBlockState(plantPos, plantState, 3);
            world.playSound(null, plantPos, plantState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
            if (!player.isCreative()) {
                seedStack.decrement(1);
            }
            return true;
        }
        return false;
    }

    @Override
    public String getId() {
        return "plant";
    }
}
