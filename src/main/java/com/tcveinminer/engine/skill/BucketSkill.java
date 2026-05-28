package com.tcveinminer.engine.skill;

import com.tcveinminer.TCVeinMinerMod;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.util.SessionStats;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.*;

public final class BucketSkill {

    private BucketSkill() {}

    public static TypedActionResult<ItemStack> onUseItem(PlayerEntity player, net.minecraft.world.World world, Hand hand) {
        if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

        ModConfig c = ConfigManager.get();
        if (!c.enabled || !c.enableBucketSkill) return TypedActionResult.pass(player.getStackInHand(hand));

        if (!(player instanceof ServerPlayerEntity spe)) return TypedActionResult.pass(player.getStackInHand(hand));
        
        // Kiểm tra xem người chơi có đang giữ phím V không
        if (!TCVeinMinerMod.playersHoldingV.contains(spe.getUuid())) return TypedActionResult.pass(player.getStackInHand(hand));

        ItemStack stack = spe.getStackInHand(hand);
        
        // Chỉ xử lý xô không (múc chất lỏng)
        if (stack.getItem() != Items.BUCKET) {
            return TypedActionResult.pass(stack);
        }

        // Thực hiện Raycast để tìm khối chất lỏng
        BlockHitResult hitResult = world.raycast(new RaycastContext(
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
            if (originState.getFluidState().isStill()) {
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
                stack.decrement(toTake);
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
