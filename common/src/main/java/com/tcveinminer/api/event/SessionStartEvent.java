package com.tcveinminer.api.event;

import com.tcveinminer.engine.action.ActionType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Event data when a mining session starts.
 */
public record SessionStartEvent(
        PlayerEntity player,
        World world,
        BlockPos origin,
        ActionType actionType,
        int targetBlocks
) {}
