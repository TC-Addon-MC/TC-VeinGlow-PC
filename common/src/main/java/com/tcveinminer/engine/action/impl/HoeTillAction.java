package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

public final class HoeTillAction implements BlockAction {

    @SuppressWarnings("unchecked")
    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        if (!(ctx instanceof ActionContext.InteractContext ic)) return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        Hand hand = ic.getInteractHand();
        ItemStack stack = player.getStackInHand(hand);

        if (!(stack.getItem() instanceof HoeItem)) return false;

        // Use vanilla useOnBlock — handles all tilling including tilling maps
        BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        ActionResult result = stack.useOnBlock(new net.minecraft.item.ItemUsageContext(player, hand, hitResult) {});
        if (result.isAccepted()) {
            return true;
        }

        // Fallback: manual tilling for common cases (in case useOnBlock is unavailable)
        BlockState targetState = null;
        boolean dropRoots = false;

        if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.DIRT_PATH) {
            targetState = Blocks.FARMLAND.getDefaultState();
        } else if (block == Blocks.COARSE_DIRT) {
            targetState = Blocks.DIRT.getDefaultState();
        } else if (block == Blocks.ROOTED_DIRT) {
            targetState = Blocks.DIRT.getDefaultState();
            dropRoots = true;
        }

        if (targetState != null) {
            world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.setBlockState(pos, targetState, 11);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, targetState));
            if (dropRoots) {
                Block.dropStack(world, pos, new ItemStack(Items.HANGING_ROOTS));
            }
            if (!player.isCreative()) {
                EquipmentSlot slot = (hand == Hand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.damage(1, player, slot);
            }
            return true;
        }

        return false;
    }

    @Override
    public String getId() {
        return "hoe_till";
    }
}
