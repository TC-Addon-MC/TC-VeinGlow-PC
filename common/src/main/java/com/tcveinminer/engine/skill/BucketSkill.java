package com.tcveinminer.engine.skill;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;

import java.util.*;

public final class BucketSkill {

    private BucketSkill() {}

    public static InteractionResultHolder<ItemStack> onUseItem(Player player, net.minecraft.world.level.Level world, InteractionHand hand) {
        if (world.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));

        ModConfig c = ConfigManager.get();
        if (!c.enabled || !c.enableBucketSkill) return InteractionResultHolder.pass(player.getItemInHand(hand));

        if (!(player instanceof ServerPlayer spe)) return InteractionResultHolder.pass(player.getItemInHand(hand));
        
        // Kiểm tra xem người chơi có đang giữ phím V không
        if (!com.tcveinminer.engine.state.PlayerStateRegistry.isHoldingKey(spe.getUUID())) return InteractionResultHolder.pass(player.getItemInHand(hand));

        ItemStack stack = spe.getItemInHand(hand);
        
        // Chỉ xử lý xô không (múc chất lỏng)
        if (stack.getItem() != Items.BUCKET) {
            return InteractionResultHolder.pass(stack);
        }

        // Thực hiện Raycast để tìm khối chất lỏng
        BlockHitResult hitResult = world.clip(new ClipContext(
                player.getEyePosition(1.0F),
                player.getEyePosition(1.0F).add(player.getViewVector(1.0F).scale(5.0)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.SOURCE_ONLY,
                player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos originPos = hitResult.getBlockPos();
        BlockState originState = world.getBlockState(originPos);

        // Kiểm tra xem có phải chất lỏng nguồn (Source) không
        if (originState.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            if (originState.getFluidState().isSource()) {
                return handleScoopFluid(spe, (ServerLevel) world, hand, hitResult);
            }
        } else if (originState.getBlock() == Blocks.WATER_CAULDRON || originState.getBlock() == Blocks.LAVA_CAULDRON) {
            // Không hỗ trợ vạc nước/lava để tránh phức tạp
            return InteractionResultHolder.pass(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    private static InteractionResultHolder<ItemStack> handleScoopFluid(ServerPlayer spe, ServerLevel world, InteractionHand hand, BlockHitResult hitResult) {
        com.tcveinminer.engine.MiningEngine engine = com.tcveinminer.engine.MiningEngine.forPlayer(spe.getUUID());
        if (!engine.isWorking()) {
            boolean handled = engine.right().onInteractTrigger(spe, world, hand, hitResult);
            if (handled) {
                return InteractionResultHolder.success(spe.getItemInHand(hand));
            }
        }
        
        return InteractionResultHolder.pass(spe.getItemInHand(hand));
    }
}
