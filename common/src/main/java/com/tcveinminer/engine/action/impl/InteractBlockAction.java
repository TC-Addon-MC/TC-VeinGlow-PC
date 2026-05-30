package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InteractBlockAction implements BlockAction {

    @Override
    public boolean execute(ServerPlayer player, ServerLevel world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.InteractContext ic))
            return false;

        BlockState state = world.getBlockState(pos);
        InteractionHand hand = ic.getInteractHand();
        ItemStack stack = player.getItemInHand(hand);

        if (stack.isEmpty())
            return false;
        // 1. AXE & SHOVEL INTERACTIONS
        if (stack.getItem() instanceof AxeItem || stack.getItem() instanceof ShovelItem) {
            net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(
                    net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            net.minecraft.world.item.context.UseOnContext usageCtx = new net.minecraft.world.item.context.UseOnContext(player, hand,
                    hitResult);
            net.minecraft.world.InteractionResult result = stack.useOn(usageCtx);
            if (result.consumesAction()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getId() {
        return "interact_block";
    }
}
