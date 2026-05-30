package com.tcveinminer.engine.filter;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class ValidationPipeline {
    
    public static boolean validatePlayer(Player player) {
        if (player == null || player.isSpectator()) return false;
        return true;
    }

    public static boolean validateWorld(ServerLevel world, BlockPos pos) {
        if (world == null || pos == null) return false;
        return world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean validateDistance(Player player, BlockPos targetPos, double maxReachSq) {
        if (player == null || targetPos == null) return false;
        double distSq = player.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        return distSq <= maxReachSq;
    }

    public static boolean validateBlock(ServerLevel world, BlockPos pos, BlockState expectedState) {
        if (world == null || pos == null) return false;
        BlockState currentState = world.getBlockState(pos);
        if (expectedState != null && currentState.getBlock() != expectedState.getBlock()) {
            return false;
        }
        return !currentState.isAir();
    }
}
