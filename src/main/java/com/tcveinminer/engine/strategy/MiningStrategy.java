package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Core abstraction for mining modes.
 * Sử dụng MiningRequest để đóng gói toàn bộ dữ liệu cần thiết cho FilterModeManager.
 */
public interface MiningStrategy {

    String getId();
    String getLabel();
    String getIcon();
    FilterModeManager.MiningMode getModeType();

    /**
     * Thu thập danh sách các block cần đào.
     */
    List<BlockPos> collectBlocks(MiningRequest request);

    /**
     * DTO chứa toàn bộ state tĩnh cho một phiên đào.
     */
    record MiningRequest(
            World world,
            PlayerEntity player,
            ItemStack tool,
            BlockPos origin,
            BlockState targetState,
            int maxBlocks,
            OrientationContext orientCtx,
            FilterModeManager.BlockFilter filter,
            FilterModeManager.FilterCache cache
    ) {}
}