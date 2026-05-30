package com.tcveinminer.engine.skill;

import com.tcveinminer.TCVeinMinerMod;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.util.SessionStats;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;

import java.util.*;

public final class BucketSkill {

    private BucketSkill() {}

    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player, net.minecraft.world.level.Level world, Hand hand) {
        if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

        ModConfig c = ConfigManager.get();
        if (!c.enabled || !c.enableBucketSkill) return TypedActionResult.pass(player.getStackInHand(hand));

        if (!(player instanceof ServerPlayerEntity spe)) return TypedActionResult.pass(player.getStackInHand(hand));
        
        // Kiểm tra xem người chơi có đang giữ phím V không
        if (!com.tcveinminer.engine.state.PlayerStateRegistry.isHoldingKey(spe.getUuid())) return TypedActionResult.pass(player.getStackInHand(hand));

        ItemStack stack = spe.getStackInHand(hand);
        
        // Chỉ xử lý xô không (múc chất lỏng)
        if (stack.getItem() != Items.BUCKET) {
            return TypedActionResult.pass(stack);
        }

        // Thực hiện Raycast để tìm khối chất lỏng
        BlockHitResult hitResult = world.clip(new RaycastContext(
                player.getCameraPosVec(1.0F),
                player.getCameraPosVec(1.0F).add(player.getRotationVec(1.0F).multiply(5.0)),
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.SOURCE_ONLY,
                player
        ));

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return TypedActionResult.pass(stack);
        }

        BlockPos originPos = hitResult.getBlockPos();
        BlockState originState = world.getBlockState(originPos);

        // Kiểm tra xem có phải chất lỏng nguồn (Source) không
        if (originState.getBlock() instanceof FluidBlock fluidBlock) {
            if (originState.getFluidState().isSource()) {
                return handleScoopFluid(spe, (ServerWorld) world, hand, hitResult);
            }
        } else if (originState.getBlock() == Blocks.WATER_CAULDRON || originState.getBlock() == Blocks.LAVA_CAULDRON) {
            // Không hỗ trợ vạc nước/lava để tránh phức tạp
            return TypedActionResult.pass(stack);
        }

        return TypedActionResult.pass(stack);
    }

    private static TypedActionResult<ItemStack> handleScoopFluid(ServerPlayerEntity spe, ServerWorld world, Hand hand, BlockHitResult hitResult) {
        com.tcveinminer.engine.MiningEngine engine = com.tcveinminer.engine.MiningEngine.forPlayer(spe.getUuid());
        if (!engine.isWorking()) {
            boolean handled = engine.right().onInteractTrigger(spe, world, hand, hitResult);
            if (handled) {
                return TypedActionResult.success(spe.getStackInHand(hand));
            }
        }
        
        return TypedActionResult.pass(spe.getStackInHand(hand));
    }

    private static int countItems(PlayerEntity player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItems(PlayerEntity player, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                int toTake = Math.min(stack.getCount(), remaining);
                stack.shrink(toTake);
                remaining -= toTake;
            }
        }
    }

    private static void giveItems(PlayerEntity player, Item item, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int toGive = Math.min(remaining, item.getMaxCount());
            ItemStack stack = new ItemStack(item, toGive);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
            remaining -= toGive;
        }
    }
}
