package com.tcveinminer.api;

import com.tcveinminer.api.query.EngineQuery;
import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.engine.action.ActionExecutorRegistry;
import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.action.BlockAction;
import com.tcveinminer.engine.capability.CapabilityRegistry;
import com.tcveinminer.engine.capability.ItemActionCapability;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Main entry point for the TC VeinMiner API.
 * Use this class to register custom strategies, capabilities, actions,
 * or to query engine states.
 */
public final class TCVeinMinerAPI {

    private TCVeinMinerAPI() {}

    /**
     * Registers a custom mining strategy (e.g., custom vein spread or shape).
     * @param strategy The strategy implementation.
     */
    public static void registerStrategy(MiningStrategy strategy) {
        StrategyRegistry.register(strategy);
    }

    /**
     * Registers an item action capability.
     * This defines what action an item can perform on a target block.
     * @param capability The capability implementation.
     */
    public static void registerCapability(ItemActionCapability capability) {
        CapabilityRegistry.register(capability);
    }

    /**
     * Registers a block action executor for a given action type.
     * This defines *how* a specific action type is executed on a block.
     * @param type The action type.
     * @param action The executor logic.
     */
    public static void registerBlockAction(ActionType type, BlockAction action) {
        ActionExecutorRegistry.register(type, action);
    }

    /**
     * Queries the mining engine state for a specific player.
     * @param uuid The player's UUID.
     * @return A read-only query object, or null if the player has no engine (e.g. offline).
     */
    public static EngineQuery queryPlayer(UUID uuid) {
        if (!MiningEngine.hasEngine(uuid)) {
            return null;
        }
        return MiningEngine.forPlayer(uuid);
    }

    /**
     * Queries the mining engine state for a specific player.
     * @param player The player entity.
     * @return A read-only query object, or null if the player has no engine.
     */
    public static EngineQuery queryPlayer(Player player) {
        return queryPlayer(player.getUUID());
    }
}
