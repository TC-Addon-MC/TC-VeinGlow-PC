package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import com.tcveinminer.engine.skill.BreakSkill;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Standard block breaking action — wraps {@link BreakSkill}.
 * Used for vein mining, shape mining, and general block breaking.
 */
public final class BreakBlockAction implements BlockAction {

    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        return BreakSkill.breakBlock(player, world, pos, world.getBlockState(pos));
    }

    @Override
    public String getId() {
        return "break";
    }
}
