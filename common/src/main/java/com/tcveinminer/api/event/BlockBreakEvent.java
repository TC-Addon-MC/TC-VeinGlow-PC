package com.tcveinminer.api.event;

import com.tcveinminer.engine.action.ActionType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Event data for individual block processing during a mining session.
 */
public record BlockBreakEvent(
        PlayerEntity player,
        World world,
        BlockPos pos,
        BlockState state,
        ActionType actionType
) {}
