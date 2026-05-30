package com.tcveinminer.engine.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;

/**
 * @deprecated Action executors now use direct execution instead of simulated clicks.
 */
@Deprecated
public final class InteractSkill {

    private InteractSkill() {}

    /**
     * @deprecated Use direct execution actions instead.
     */
    @Deprecated
    public static boolean interact(ServerPlayer spe, ServerLevel world, BlockPos pos, InteractionHand interactHand, BlockHitResult originalHit) {
        // Deprecated: Simulated interactions trigger infinite callbacks.
        return false;
    }
}
