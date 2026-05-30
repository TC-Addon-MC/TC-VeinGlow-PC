package com.tcveinminer.engine.action;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/**
 * Core interface for all block actions.
 * <p>
 * Replaces the old giant {@code if (action == BREAK) / else if (action == INTERACT)} pattern.
 * Each implementation handles exactly one action type on a single block position.
 * Bulk orchestration (queue, tick slicing) is handled by {@link com.tcveinminer.engine.AbstractActionEngine}.
 */
public interface BlockAction {

    /**
     * Execute this action on a single block position.
     *
     * @param player the server player performing the action
     * @param world  the server world
     * @param pos    the block position to act on
     * @param ctx    action context carrying session-specific data (may be subclassed)
     * @return {@code true} if the action succeeded (block was processed)
     */
    boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx);

    /**
     * @return unique identifier for this action (used for logging/debugging)
     */
    String getId();
}
