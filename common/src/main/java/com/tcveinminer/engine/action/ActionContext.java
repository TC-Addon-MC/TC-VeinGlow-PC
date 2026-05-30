package com.tcveinminer.engine.action;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;

/**
 * Base context carrying shared action data for a session.
 * <p>
 * Subclassed for action-specific data (TreeCap, Interact, Plant, Bucket).
 * This avoids coupling action-specific fields (e.g. treeOriginPos) into the session.
 */
public class ActionContext {

    private final BlockState originalState;
    private final Item initialItem;

    public ActionContext(BlockState originalState, Item initialItem) {
        this.originalState = originalState;
        this.initialItem = initialItem;
    }

    public BlockState getOriginalState() { return originalState; }
    public Item getInitialItem() { return initialItem; }

    // ══════════════════════════════════════════════════════════════════
    // Subclasses for action-specific data
    // ══════════════════════════════════════════════════════════════════

    /**
     * Context for TreeCapitator actions — carries tree origin and sapling type
     * for auto-replant on session finalization.
     */
    public static class TreeCapContext extends ActionContext {
        private final BlockPos treeOriginPos;
        private BlockState treeSaplingType;

        public TreeCapContext(BlockState originalState, Item initialItem,
                              BlockPos treeOriginPos, BlockState treeSaplingType) {
            super(originalState, initialItem);
            this.treeOriginPos = treeOriginPos;
            this.treeSaplingType = treeSaplingType;
        }

        public BlockPos getTreeOriginPos() { return treeOriginPos; }
        public BlockState getTreeSaplingType() { return treeSaplingType; }
        public void setTreeSaplingType(BlockState sapling) { this.treeSaplingType = sapling; }
    }

    /**
     * Context for interaction-based actions (strip log, path grass, hoe till, plant).
     * Carries the hand and hit result needed by InteractSkill.
     */
    public static class InteractContext extends ActionContext {
        private final InteractionHand interactHand;
        private final BlockHitResult hitResult;

        public InteractContext(BlockState originalState, Item initialItem,
                               InteractionHand interactHand, BlockHitResult hitResult) {
            super(originalState, initialItem);
            this.interactHand = interactHand;
            this.hitResult = hitResult;
        }

        public InteractionHand getInteractHand() { return interactHand; }
        public BlockHitResult getHitResult() { return hitResult; }
    }

    /**
     * Context for planting actions — carries the seed item for validation.
     */
    public static class PlantContext extends InteractContext {
        public PlantContext(BlockState originalState, Item initialItem,
                            InteractionHand interactHand, BlockHitResult hitResult) {
            super(originalState, initialItem, interactHand, hitResult);
        }
    }

    /**
     * Context for fluid scooping — carries fluid type and available bucket count.
     */
    public static class BucketContext extends ActionContext {
        private final Block fluidType;
        private final int availableBuckets;

        public BucketContext(BlockState originalState, Item initialItem,
                             Block fluidType, int availableBuckets) {
            super(originalState, initialItem);
            this.fluidType = fluidType;
            this.availableBuckets = availableBuckets;
        }

        public Block getFluidType() { return fluidType; }
        public int getAvailableBuckets() { return availableBuckets; }
    }
}
