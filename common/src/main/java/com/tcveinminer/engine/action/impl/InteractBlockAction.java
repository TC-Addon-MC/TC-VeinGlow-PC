package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.state.property.Property;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InteractBlockAction implements BlockAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(InteractBlockAction.class);

    @SuppressWarnings("unchecked")
    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.InteractContext ic))
            return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        Hand hand = ic.getInteractHand();
        ItemStack stack = player.getStackInHand(hand);

        if (stack.isEmpty())
            return false;

        EquipmentSlot slot = (hand == Hand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

        // 1. AXE & SHOVEL INTERACTIONS
        if (stack.getItem() instanceof AxeItem || stack.getItem() instanceof ShovelItem) {
            net.minecraft.util.hit.BlockHitResult hitResult = new net.minecraft.util.hit.BlockHitResult(
                    net.minecraft.util.math.Vec3d.ofCenter(pos), net.minecraft.util.math.Direction.UP, pos, false);
            net.minecraft.item.ItemUsageContext usageCtx = new net.minecraft.item.ItemUsageContext(player, hand,
                    hitResult);
            net.minecraft.util.ActionResult result = stack.useOnBlock(usageCtx);
            if (result.isAccepted()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getId() {
        return "interact_block";
    }

    private static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : from.getProperties()) {
            if (result.contains(property)) {
                result = copyProperty(from, result, property);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to,
            Property<T> property) {
        return to.with(property, from.get(property));
    }
}
