package com.tcveinminer.platform;

import java.util.List;
import java.util.Optional;

/**
 * Bridge interface for the common module to interact with client-side logic safely.
 * This ensures no net.minecraft.client.* imports exist in common.
 */
public interface ClientBridge {

    // Stub implementation that does nothing on the server
    ClientBridge INSTANCE = java.util.ServiceLoader.load(ClientBridge.class).findFirst().orElse(new ClientBridge() {
        @Override public void onClientTick(Object client) {}
        @Override public void onRenderWorld(Object renderContext) {}
        @Override public void updateLookedAtBlock(Object player, Optional<Long> pos) {}
        @Override public void updateFilterResult(boolean allowHighlight) {}
        @Override public void updateHighlightList(List<Long> blocks, String highlightStyle, String source) {}
        @Override public void applyHighlightDelta(List<Long> addedBlocks, List<Long> removedBlocks, String highlightStyle, String source) {}
    });

    void onClientTick(Object client);
    void onRenderWorld(Object renderContext);

    // S2C packet handlers
    void updateLookedAtBlock(Object player, Optional<Long> pos);
    void updateFilterResult(boolean allowHighlight);
    void updateHighlightList(List<Long> blocks, String highlightStyle, String source);
    void applyHighlightDelta(List<Long> addedBlocks, List<Long> removedBlocks, String highlightStyle, String source);
}