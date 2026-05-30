package com.tcveinminer.api.event;

import com.tcveinminer.engine.action.ActionType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Event data for individual block processing during a mining session.
 */
public record BlockBreakEvent(
        Player player,
        Level world,
        BlockPos pos,
        BlockState state,
        ActionType actionType
) {}
