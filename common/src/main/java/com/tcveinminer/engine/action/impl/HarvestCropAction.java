package com.tcveinminer.engine.action.impl;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.BlockAction;
import com.tcveinminer.engine.skill.CropHarvestSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

/**
 * Harvest mature crop action — wraps {@link CropHarvestSkill}.
 * Breaks the crop block and replants with default state (age 0).
 */
public final class HarvestCropAction implements BlockAction {

    @Override
    public boolean execute(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ActionContext ctx) {
        return CropHarvestSkill.harvest(player, world, pos, world.getBlockState(pos));
    }

    @Override
    public String getId() {
        return "harvest_crop";
    }
}
