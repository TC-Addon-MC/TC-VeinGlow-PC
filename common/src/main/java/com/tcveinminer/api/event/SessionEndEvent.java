package com.tcveinminer.api.event;

import com.tcveinminer.engine.action.ActionType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Event data when a mining session ends (either finished naturally or cancelled).
 */
public record SessionEndEvent(
        Player player,
        Level world,
        ActionType actionType,
        int processedBlocks,
        int targetBlocks,
        boolean isCancelled
) {}
