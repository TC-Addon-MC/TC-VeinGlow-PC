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

    public static boolean harvest(ServerPlayer spe, ServerLevel world, BlockPos pos, BlockState currentState) {
        if (!world.isClientSide) {
            // Get drops directly
            List<ItemStack> drops = Block.getDrops(currentState, world, pos, world.getBlockEntity(pos), spe, spe.getMainHandItem());
            
            Item seedItem = currentState.getBlock().asItem();
            boolean seedConsumed = false;
            
            for (ItemStack drop : drops) {
                if (!seedConsumed && drop.getItem() == seedItem) {
                    drop.shrink(1);
                    seedConsumed = true;
                }
                if (!drop.isEmpty()) {
                    Block.popResource(world, pos, drop);
                }
            }
            
            // Break sound and particle effects
            world.levelEvent(null, 2001, pos, Block.getId(currentState));
            
            // Check below block for valid farmland/soul sand
            BlockState below = world.getBlockState(pos.below());
            if (below.is(Blocks.FARMLAND) || below.is(Blocks.SOUL_SAND) || below.is(Blocks.JUNGLE_LOG) || below.is(net.minecraft.tags.BlockTags.LOGS)) {
                world.setBlock(pos, currentState.getBlock().defaultBlockState(), 3);
            } else {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            return true;
        }
        return false;
    }
}
