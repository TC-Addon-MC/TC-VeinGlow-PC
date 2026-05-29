package com.tcveinminer.api.addon;

import com.tcveinminer.api.VeinMineEvents;
import com.tcveinminer.api.query.EngineQuery;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.action.BlockAction;
import com.tcveinminer.engine.capability.ItemActionCapability;
import com.tcveinminer.engine.strategy.MiningStrategy;

import java.util.UUID;

/**
 * Injection context provided to addon mods during initialization.
 * <p>
 * Addon mods receive this via {@link VeinMinerAddon#onAddonInit(AddonContext)}.
 * The interface is 100% loader-agnostic — addons need zero loader imports.
 */
public interface AddonContext {

    // ── Registration APIs ─────────────────────────────────────────────────

    /** Register a custom mining strategy (new shape/spread algorithm). */
    void registerMiningStrategy(MiningStrategy strategy);

    /** Register an item action capability (what an item does on a block). */
    void registerItemCapability(ItemActionCapability capability);

    /** Register a custom block action executor for an ActionType. */
    void registerBlockAction(ActionType type, BlockAction action);

    // ── Event API ─────────────────────────────────────────────────────────

    /**
     * Access the public event registry.
     * Register listeners on the returned object, e.g.:
     * <pre>{@code
     * context.events().SESSION_START.register(e -> EventResult.PASS);
     * }</pre>
     * Note: {@link VeinMineEvents} is a static class; this method exists for
     * discoverability. You may also call {@code VeinMineEvents.SESSION_START.register(...)} directly.
     */
    VeinMineEvents events();

    // ── Query API ─────────────────────────────────────────────────────────

    /**
     * Query the mining engine state for a player (read-only).
     * Returns null if the player has no active engine.
     */
    EngineQuery queryPlayer(UUID uuid);

    // ── Config API ────────────────────────────────────────────────────────

    /** Read the current server configuration. Do not modify directly. */
    ModConfig getServerConfig();

    // ── Platform Info ─────────────────────────────────────────────────────

    /** @return "Fabric", "NeoForge", or "Forge" */
    String getPlatformName();

    /** Check if another mod is loaded. */
    boolean isModLoaded(String modId);
}
