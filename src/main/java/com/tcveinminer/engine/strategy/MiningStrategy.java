package com.tcveinminer.engine.strategy;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Core abstraction: mỗi mining mode là một Strategy riêng biệt.
 * Strategy quyết định cách scan, traversal order, và mining priority.
 * Renderer và Executor đều dùng chung kết quả từ Strategy.
 */
public interface MiningStrategy {

    /** Unique key để register, ví dụ "FACE", "SPIRAL", "TUNNEL" */
    String getId();

    /** Label hiện trong UI */
    String getLabel();

    /** Icon emoji hiện trong radial menu */
    String getIcon();

    /**
     * Tìm tất cả blocks cần đào từ điểm origin.
     * Kết quả này được dùng chung bởi cả Executor lẫn Renderer —
     * KHÔNG được gọi riêng từ render thread.
     *
     * @param world  world để query block state
     * @param origin block vừa bị break (hoặc đang nhắm vào)
     * @param target block state cần match
     * @param maxBlocks giới hạn tối đa
     * @return ordered list: thứ tự ưu tiên đào, không chứa origin
     */
    List<BlockPos> collectBlocks(World world, BlockPos origin, BlockState target, int maxBlocks);
}
