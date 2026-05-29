package com.tcveinminer.engine.action;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registry mapping {@link ActionType} to {@link BlockAction} implementations.
 * <p>
 * Replaces the old giant {@code if/else} chain in {@code MiningEngine.onServerTick()}.
 * Extensible — addons can register custom actions at runtime.
 */
public final class ActionExecutorRegistry {

    private static final Map<ActionType, BlockAction> EXECUTORS = new EnumMap<>(ActionType.class);

    static {
        register(ActionType.BREAK, new com.tcveinminer.engine.action.impl.BreakBlockAction());
        register(ActionType.TREE_CAP, new com.tcveinminer.engine.action.impl.TreeCapAction());
        register(ActionType.CROP_HARVEST, new com.tcveinminer.engine.action.impl.HarvestCropAction());
        register(ActionType.INTERACT_BLOCK, new com.tcveinminer.engine.action.impl.InteractBlockAction());
        register(ActionType.PLANT, new com.tcveinminer.engine.action.impl.PlantAction());
        register(ActionType.HOE_TILL, new com.tcveinminer.engine.action.impl.HoeTillAction());
        register(ActionType.FLUID_SCOOP, new com.tcveinminer.engine.action.impl.FluidScoopAction());
    }

    private ActionExecutorRegistry() {}

    /**
     * Register a {@link BlockAction} for the given action type.
     * Overwrites any previously registered action for the same type.
     */
    public static void register(ActionType type, BlockAction action) {
        EXECUTORS.put(type, action);
    }

    /**
     * Get the registered {@link BlockAction} for the given type.
     *
     * @return the registered action, or {@code null} if none registered
     */
    public static BlockAction get(ActionType type) {
        return EXECUTORS.get(type);
    }

    /**
     * @return true if an executor is registered for the given type
     */
    public static boolean has(ActionType type) {
        return EXECUTORS.containsKey(type);
    }
}
