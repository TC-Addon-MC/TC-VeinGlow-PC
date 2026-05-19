package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

/**
 * Core abstraction for mining modes.
 * collectBlocks now receives full orientation context so shape-mining
 * modes (Area, Tunnel, Stair) can orient correctly relative to the player.
 */
public interface MiningStrategy {

    String getId();
    String getLabel();
    String getIcon();

    /**
     * Collect all blocks to mine starting from origin.
     *
     * @param world       world to query
     * @param origin      block that was broken / is targeted
     * @param target      block state to match (for vein modes)
     * @param maxBlocks   max results (NOT including origin)
     * @param ctx         orientation context: player facing, hit face, axes
     * @return ordered list, origin excluded
     */
    List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                  int maxBlocks, OrientationContext ctx);

    /**
     * Legacy overload for callers that do not have orientation yet.
     * Falls back to a default south-facing context.
     */
    default List<BlockPos> collectBlocks(World world, BlockPos origin,
                                          BlockState target, int maxBlocks) {
        return collectBlocks(world, origin, target, maxBlocks,
                             OrientationContext.defaultContext());
    }

    /**
     * Convenience: build context from player + hit face directly.
     */
    default List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target,
                                          int maxBlocks, PlayerEntity player, Direction hitFace) {
        OrientationContext ctx = OrientationContext.of(
            hitFace,
            OrientationContext.facingFromYaw(player.getYaw())
        );
        return collectBlocks(world, origin, target, maxBlocks, ctx);
    }
}
