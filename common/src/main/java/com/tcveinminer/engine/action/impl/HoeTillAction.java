package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.gameevent.GameEvent;

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
        ActionResult result = stack.useOnBlock(new net.minecraft.world.item.context.UseOnContext(player, hand, hitResult) {});
        if (result.isAccepted()) {
            return true;
        }

        // Fallback: manual tilling for common cases (in case useOnBlock is unavailable)
        BlockState targetState = null;
        boolean dropRoots = false;

        if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.DIRT_PATH) {
            targetState = Blocks.FARMLAND.defaultBlockState();
        } else if (block == Blocks.COARSE_DIRT) {
            targetState = Blocks.DIRT.defaultBlockState();
        } else if (block == Blocks.ROOTED_DIRT) {
            targetState = Blocks.DIRT.defaultBlockState();
            dropRoots = true;
        }

        if (targetState != null) {
            world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.setBlock(pos, targetState, 11);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, targetState));
            if (dropRoots) {
                Block.dropStack(world, pos, new ItemStack(Items.HANGING_ROOTS));
            }
            if (!player.isCreative()) {
                EquipmentSlot slot = (hand == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
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
