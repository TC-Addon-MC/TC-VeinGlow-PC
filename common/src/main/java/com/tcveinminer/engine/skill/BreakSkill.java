package com.tcveinminer.engine.skill;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public final class BreakSkill {

    private BreakSkill() {}

    /**
     * Xử lý hành động đập vỡ block cơ bản (áp dụng cho đập quặng, chặt cây thông thường).
     */
    public static boolean breakBlock(ServerPlayerEntity spe, ServerWorld world, BlockPos pos, BlockState currentState) {
        boolean success = spe.interactionManager.tryBreakBlock(pos);
        if (success) {
            // Hiển thị hiệu ứng đập block cho chính người chơi (vì server tự đập nên client không có)
            world.syncWorldEvent(null, 2001, pos, Block.getRawIdFromState(currentState));
        }
        return success;
    }
}
