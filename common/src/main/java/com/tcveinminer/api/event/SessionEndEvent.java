package com.tcveinminer.api.event;

import com.tcveinminer.engine.action.ActionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Event data when a mining session ends (either finished naturally or cancelled).
 */
public record SessionEndEvent(
        PlayerEntity player,
        World world,
        ActionType actionType,
        int processedBlocks,
        int targetBlocks,
        boolean isCancelled
) {}
