package com.tcveinminer.api.addon;

import com.tcveinminer.api.VeinMineEvents;
import com.tcveinminer.api.addon.AddonContext;
import com.tcveinminer.api.query.EngineQuery;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.engine.MiningEngine;
import com.tcveinminer.engine.action.ActionExecutorRegistry;
import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.action.BlockAction;
import com.tcveinminer.engine.capability.CapabilityRegistry;
import com.tcveinminer.engine.capability.ItemActionCapability;
import com.tcveinminer.engine.strategy.MiningStrategy;
import com.tcveinminer.engine.strategy.StrategyRegistry;
import com.tcveinminer.platform.Services;

import java.util.UUID;

/**
 * Fabric implementation of {@link AddonContext}.
 * Provides addon mods with a stable, loader-independent API surface.
 */
public final class AddonContextImpl implements AddonContext {

    @Override
    public void registerMiningStrategy(MiningStrategy strategy) {
        StrategyRegistry.register(strategy);
    }

    @Override
    public void registerItemCapability(ItemActionCapability capability) {
        CapabilityRegistry.register(capability);
    }

    @Override
    public void registerBlockAction(ActionType type, BlockAction action) {
        ActionExecutorRegistry.register(type, action);
    }

    @Override
    public VeinMineEvents events() {
        return null; // Static class — callers use VeinMineEvents.SESSION_START.register(...) directly
    }

    @Override
    public EngineQuery queryPlayer(UUID uuid) {
        return MiningEngine.hasEngine(uuid) ? MiningEngine.forPlayer(uuid) : null;
    }

    @Override
    public ModConfig getServerConfig() {
        return ConfigManager.get();
    }

    @Override
    public String getPlatformName() {
        return Services.PLATFORM().getPlatformName();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return Services.PLATFORM().isModLoaded(modId);
    }
}
