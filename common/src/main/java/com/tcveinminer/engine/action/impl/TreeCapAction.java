package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import com.tcveinminer.engine.skill.BreakSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/**
 * Tree capitator action — breaks logs and leaves as part of a tree.
 * Uses the same {@link BreakSkill} as regular breaking.
 * Auto-replant is handled at session finalization by the engine, not here.
 */
public final class TreeCapAction implements BlockAction {

    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        return BreakSkill.destroyBlock(player, world, pos, world.getBlockState(pos));
    }

    @Override
    public String getId() {
        return "tree_cap";
    }
}
