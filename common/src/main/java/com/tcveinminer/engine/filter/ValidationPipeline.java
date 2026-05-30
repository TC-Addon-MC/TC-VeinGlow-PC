package com.tcveinminer.engine.filter;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.config.ModConfig;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class ValidationPipeline {
    
    public static boolean validatePlayer(PlayerEntity player) {
        if (player == null || player.isSpectator()) return false;
        return true;
    }

    public static boolean validateWorld(ServerWorld world, BlockPos pos) {
        if (world == null || pos == null) return false;
        return world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean validateDistance(PlayerEntity player, BlockPos targetPos, double maxReachSq) {
        if (player == null || targetPos == null) return false;
        double distSq = player.squaredDistanceTo(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        return distSq <= maxReachSq;
    }

    public static boolean validateBlock(ServerWorld world, BlockPos pos, BlockState expectedState) {
        if (world == null || pos == null) return false;
        BlockState currentState = world.getBlockState(pos);
        if (expectedState != null && currentState.getBlock() != expectedState.getBlock()) {
            return false;
        }
        return !currentState.isAir();
    }
}
