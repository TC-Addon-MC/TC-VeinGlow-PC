package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;

import java.util.function.BiPredicate;

/**
 * Quản lý các bộ lọc block — dùng chung cho tất cả các chế độ đào.
 *
 * Mỗi chế độ tự chọn filter phù hợp:
 *   - Vein (FACE/EDGES/CORNERS/TALL): dùng sameBlock
 *   - Tunnel/Shape:                   dùng notAir
 *   - TreeCapitator:                  dùng isLogOrLeaves
 */
public final class FilterModeManager {

    /** Chỉ đào block cùng loại với target (dùng cho vein mining). */
    public static BiPredicate<BlockPos, BlockState> sameBlock(BlockState target) {
        return (pos, state) -> state.getBlock() == target.getBlock();
    }

    /** Đào mọi block không phải air (dùng cho tunnel / shape). */
    public static BiPredicate<BlockPos, BlockState> notAir() {
        return (pos, state) -> !state.isAir();
    }

    /** Chỉ đào log (dùng kiểm tra ban đầu của TreeCapitator). */
    public static BiPredicate<BlockPos, BlockState> isLog() {
        return (pos, state) -> state.isIn(BlockTags.LOGS);
    }

    /** Đào log + lá cây (dùng trong SpreadModeManager.TREE_CAP). */
    public static BiPredicate<BlockPos, BlockState> isLogOrLeaves() {
        return (pos, state) -> state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.LEAVES);
    }

    private FilterModeManager() {}
}
