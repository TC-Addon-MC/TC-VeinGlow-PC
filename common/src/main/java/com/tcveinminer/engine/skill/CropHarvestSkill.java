package com.tcveinminer.engine.skill;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class CropHarvestSkill {

    private CropHarvestSkill() {}

    public static boolean harvest(ServerPlayerEntity spe, ServerWorld world, BlockPos pos, BlockState currentState) {
        if (!world.isClient) {
            // Get drops directly
            List<ItemStack> drops = Block.getDroppedStacks(currentState, world, pos, world.getBlockEntity(pos), spe, spe.getMainHandStack());
            
            Item seedItem = currentState.getBlock().asItem();
            boolean seedConsumed = false;
            
            for (ItemStack drop : drops) {
                if (!seedConsumed && drop.getItem() == seedItem) {
                    drop.decrement(1);
                    seedConsumed = true;
                }
                if (!drop.isEmpty()) {
                    Block.dropStack(world, pos, drop);
                }
            }
            
            // Break sound and particle effects
            world.syncWorldEvent(null, 2001, pos, Block.getRawIdFromState(currentState));
            
            // Check below block for valid farmland/soul sand
            BlockState below = world.getBlockState(pos.down());
            if (below.isOf(Blocks.FARMLAND) || below.isOf(Blocks.SOUL_SAND) || below.isOf(Blocks.JUNGLE_LOG) || below.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) {
                world.setBlockState(pos, currentState.getBlock().getDefaultState(), 3);
            } else {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            }
            return true;
        }
        return false;
    }
}
