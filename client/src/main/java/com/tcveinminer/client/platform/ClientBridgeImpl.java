package com.tcveinminer.client.platform;

import com.tcveinminer.platform.ClientBridge;
import com.tcveinminer.client.ClientApi;
import com.tcveinminer.client.logic.BlockHighlighter;
import com.tcveinminer.client.gui.screens.tabs.ShapesTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.List;
import java.util.Optional;

public class ClientBridgeImpl implements ClientBridge {

    @Override
    public void onClientTick(Object client) {
        if (client instanceof MinecraftClient mc) {
            ClientApi.onClientTick(mc);
        }
    }

    @Override
    public void onRenderWorld(Object renderContext) {
        if (renderContext instanceof DrawContext ctx) {
            BlockHighlighter.onRenderWorld(ctx);
        }
    }


    @Override
    public void updateLookedAtBlock(Object player, Optional<Long> pos) {
        if (player instanceof ClientPlayerEntity clientPlayer) {
            BlockHighlighter.updateLookedAtBlock(clientPlayer, pos);
        }
    }

    @Override
    public void updateFilterResult(boolean allowHighlight) {
        BlockHighlighter.updateFilterResult(allowHighlight);
    }

    @Override
    public void updateHighlightList(List<Long> blocks, String highlightStyle, String source) {
        BlockHighlighter.updateHighlightList(blocks, highlightStyle, source);
    }

    @Override
    public void applyHighlightDelta(List<Long> addedBlocks, List<Long> removedBlocks, String highlightStyle, String source) {
        BlockHighlighter.applyHighlightDelta(addedBlocks, removedBlocks, highlightStyle, source);
    }
}
