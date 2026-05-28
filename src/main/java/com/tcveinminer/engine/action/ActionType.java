package com.tcveinminer.engine.action;

/**
 * Unified action type enum for both left-click and right-click engines.
 * Each action maps to a {@link BlockAction} implementation in the {@link ActionExecutorRegistry}.
 */
public enum ActionType {

    // ── Left-click actions ───────────────────────────────────────────────
    /** Standard block breaking (vein mine, shape mine, etc.) */
    BREAK,
    /** Tree capitator — break logs + leaves as a tree */
    TREE_CAP,

    // ── Right-click actions ──────────────────────────────────────────────
    /** Explicit item use that should pass through to vanilla (bone meal, flint & steel, lead, etc.) */
    USE_ITEM,
    /** Plant seeds on farmland */
    PLANT,
    /** Hoe tilling (convert dirt/grass to farmland) */
    HOE_TILL,
    /** Scoop fluid source blocks with empty bucket */
    FLUID_SCOOP,
    /** Harvest mature crops with replant */
    CROP_HARVEST,
    /** Block interaction (strip log, path grass, wax, etc.) */
    INTERACT_BLOCK,

    // ── Fallback ─────────────────────────────────────────────────────────
    /** No mod action — let vanilla handle the event */
    VANILLA_FALLBACK;

    /**
     * @return true if this action is handled by the left-click engine
     */
    public boolean isLeftClick() {
        return this == BREAK || this == TREE_CAP;
    }

    /**
     * @return true if this action is handled by the right-click engine
     */
    public boolean isRightClick() {
        return !isLeftClick() && this != VANILLA_FALLBACK && this != USE_ITEM;
    }
}
