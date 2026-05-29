package com.tcveinminer.engine.filter;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.strategy.FilterModeManager;
import com.tcveinminer.engine.strategy.FilterModeManager.MiningMode;

/**
 * Filter pipeline for left-click engine only.
 * <p>
 * Composes atomic filters from {@link FilterModeManager.Filters} into
 * left-click-specific pipelines. Only checks mining-related conditions.
 * <p>
 * Does NOT check: interactability, crop maturity, planting, fluid.
 */
public final class LeftClickFilterPipeline {

    private LeftClickFilterPipeline() {}

    /**
     * Build filter for the given left-click action type.
     *
     * @param type     must be BREAK or TREE_CAP
     * @param mode     the mining mode from the strategy
     * @param maxBlocks maximum blocks to include
     * @return composed filter pipeline
     */
    public static FilterModeManager.BlockFilter forAction(ActionType type, MiningMode mode, int maxBlocks) {
        return switch (type) {
            case TREE_CAP -> FilterModeManager.Presets.TREE_CAPITATOR(maxBlocks);
            case BREAK -> FilterModeManager.resolveFilter(mode, maxBlocks);
            default -> throw new IllegalArgumentException("Not a left-click action: " + type);
        };
    }
}
