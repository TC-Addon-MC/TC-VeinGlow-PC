package com.tcveinminer.engine.filter;

import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.capability.impl.HarvestCapability;
import com.tcveinminer.engine.strategy.FilterModeManager;
import com.tcveinminer.engine.strategy.FilterModeManager.Composite;
import com.tcveinminer.engine.strategy.FilterModeManager.Filters;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

/**
 * Filter pipeline for right-click engine only.
 * <p>
 * Composes atomic filters into right-click-specific pipelines.
 * Only checks interaction-related conditions.
 * <p>
 * Does NOT check: mining speed, harvest level, break permission, tree logic.
 */
public final class RightClickFilterPipeline {

    private RightClickFilterPipeline() {}

    /**
     * Build filter for the given right-click action type.
     *
     * @param type      must be a right-click action
     * @param maxBlocks maximum blocks to include
     * @return composed filter pipeline
     */
    public static FilterModeManager.BlockFilter forAction(ActionType type, int maxBlocks) {
        return switch (type) {
            case CROP_HARVEST  -> cropHarvestFilter(maxBlocks);
            case INTERACT_BLOCK -> interactFilter(maxBlocks);
            case HOE_TILL      -> hoeTillFilter(maxBlocks);
            case PLANT         -> plantFilter(maxBlocks);
            case FLUID_SCOOP   -> FilterModeManager.Presets.FLUID_SCOOP(maxBlocks);
            default            -> ctx -> true; // Should not reach here
        };
    }

    /**
     * Filter for crop harvest — same block type + must be mature.
     */
    private static FilterModeManager.BlockFilter cropHarvestFilter(int maxBlocks) {
        return Composite.and(
                Filters.chunkLoadedOnly(),
                Filters.NOT_AIR,
                Filters.maxVisited(maxBlocks),
                Filters.sameBlock(),
                ctx -> HarvestCapability.isMatureCrop(ctx.currentState()),
                ctx -> FilterModeManager.Filters.blacklist(ctx.blacklist()).test(ctx)
        );
    }

    /**
     * Filter for block interactions (strip log, path grass, etc.) — same block type.
     */
    private static FilterModeManager.BlockFilter interactFilter(int maxBlocks) {
        return Composite.and(
                Filters.chunkLoadedOnly(),
                Filters.NOT_AIR,
                Filters.maxVisited(maxBlocks),
                Filters.sameBlock(),
                ctx -> FilterModeManager.Filters.blacklist(ctx.blacklist()).test(ctx)
        );
    }

    /**
     * Filter for hoe tilling — target must be a tillable block.
     */
    private static FilterModeManager.BlockFilter hoeTillFilter(int maxBlocks) {
        return Composite.and(
                Filters.chunkLoadedOnly(),
                Filters.NOT_AIR,
                Filters.maxVisited(maxBlocks),
                Filters.sameBlock(),
                ctx -> isTillable(ctx.currentState().getBlock()),
                ctx -> FilterModeManager.Filters.blacklist(ctx.blacklist()).test(ctx)
        );
    }

    /**
     * Filter for planting — target must be farmland (block below the plant position).
     */
    private static FilterModeManager.BlockFilter plantFilter(int maxBlocks) {
        return Composite.and(
                Filters.chunkLoadedOnly(),
                Filters.NOT_AIR,
                Filters.maxVisited(maxBlocks),
                Filters.sameBlock(),
                ctx -> FilterModeManager.Filters.blacklist(ctx.blacklist()).test(ctx)
        );
    }

    /**
     * Check if a block can be tilled by a hoe.
     */
    private static boolean isTillable(Block block) {
        return block == Blocks.DIRT
            || block == Blocks.GRASS_BLOCK
            || block == Blocks.DIRT_PATH
            || block == Blocks.COARSE_DIRT
            || block == Blocks.ROOTED_DIRT;
    }
}
