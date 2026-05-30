package com.tcveinminer.engine.skill;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class CropHarvestSkill {

    private CropHarvestSkill() {}

    public static boolean harvest(ServerPlayerEntity spe, ServerWorld world, BlockPos pos, BlockState currentState) {
        if (!world.isClient) {
            // Get drops directly
            List<ItemStack> drops = Block.getDroppedStacks(currentState, world, pos, world.getBlockEntity(pos), spe, spe.getMainHandItem());
            
            Item seedItem = currentState.getBlock().asItem();
            boolean seedConsumed = false;
            
            for (ItemStack drop : drops) {
                if (!seedConsumed && drop.getItem() == seedItem) {
                    drop.shrink(1);
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
            if (below.isOf(Blocks.FARMLAND) || below.isOf(Blocks.SOUL_SAND) || below.isOf(Blocks.JUNGLE_LOG) || below.isIn(net.minecraft.tags.BlockTags.LOGS)) {
                world.setBlock(pos, currentState.getBlock().defaultBlockState(), 3);
            } else {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            return true;
        }
        return false;
    }
}
