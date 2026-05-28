package com.tcveinminer.api.event;

import com.tcveinminer.engine.action.ActionType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
