package com.tcveinminer.client.platform;

import com.tcveinminer.platform.ClientBridge;
import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.network.payload.FilterResultData;
import com.tcveinminer.network.payload.HighlightBlockListData;
import com.tcveinminer.network.payload.HighlightDeltaData;
import com.tcveinminer.network.payload.LookedAtBlockData;

import java.util.List;
import java.util.Optional;

public class ClientBridgeImpl implements ClientBridge {

    @Override
    public void onClientTick(Object client) {
        // No-op: Fabric handles tick directly in TCVeinMinerFabricClient
    }

    @Override
    public void onRenderWorld(Object renderContext) {
        // No-op: Fabric hooks render events directly
    }

    @Override
    public void updateLookedAtBlock(Object player, Optional<Long> pos) {
        VeinGlowClient.handleLookedAtBlock(new LookedAtBlockData(pos));
    }

    @Override
    public void updateFilterResult(boolean allowHighlight) {
        VeinGlowClient.handleFilterResult(new FilterResultData(allowHighlight));
    }

    @Override
    public void updateHighlightList(List<Long> blocks, String highlightStyle, String source) {
        VeinGlowClient.handleHighlightBlockList(new HighlightBlockListData(blocks, highlightStyle, source));
    }

    @Override
    public void applyHighlightDelta(List<Long> addedBlocks, List<Long> removedBlocks, String highlightStyle, String source) {
        VeinGlowClient.handleHighlightDelta(new HighlightDeltaData(addedBlocks, removedBlocks, highlightStyle, source));
    }
}