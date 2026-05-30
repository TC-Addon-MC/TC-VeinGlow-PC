package com.tcveinminer.engine.strategy;

import com.tcveinminer.engine.traversal.OrientationContext;
import com.tcveinminer.engine.state.EngineState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

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
            Level Level,
            Player player,
            ItemStack tool,
            BlockPos origin,
            BlockState targetState,
            int maxBlocks,
            OrientationContext orientCtx,
            FilterModeManager.BlockFilter filter,
            FilterModeManager.FilterCache cache,
            Set<String> blacklist,
            boolean requireHarvestCapability,
            EngineState currentStateEnum,
            Item initialItem,
            boolean allowHeldItemChange
    ) {}
}
