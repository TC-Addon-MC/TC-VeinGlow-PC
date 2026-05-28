package com.tcveinminer.engine.skill;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

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
    public static boolean interact(ServerPlayerEntity spe, ServerWorld world, BlockPos pos, Hand interactHand, BlockHitResult originalHit) {
        // Deprecated: Simulated interactions trigger infinite callbacks.
        return false;
    }
}
