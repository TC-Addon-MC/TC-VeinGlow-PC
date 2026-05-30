package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;

public final class PlantAction implements BlockAction {

    @Override
    public boolean execute(ServerPlayer player, ServerLevel world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.InteractContext ic)) return false;

        BlockPos plantPos = pos.above();
        if (!world.getBlockState(plantPos).isAir()) {
            return false;
        }

        InteractionHand hand = ic.getInteractHand();
        ItemStack seedStack = player.getItemInHand(hand);
        if (seedStack.isEmpty()) return false;

        if (seedStack.getItem() instanceof BlockItem blockItem) {
            BlockState plantState = blockItem.getBlock().defaultBlockState();
            world.setBlock(plantPos, plantState, 3);
            world.playSound(null, plantPos, plantState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!player.isCreative()) {
                seedStack.shrink(1);
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
