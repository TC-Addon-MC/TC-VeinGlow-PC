package com.tcveinminer.network.handlers;

import com.tcveinminer.network.payload.HighlightBlockListData;
import com.tcveinminer.platform.ClientBridge;

public final class HighlightBlockListHandler {
    
    public static void handle(HighlightBlockListData data) {
        ClientBridge.INSTANCE.updateHighlightList(data.blocks(), data.highlightStyle(), data.source());
    }
    
    private HighlightBlockListHandler() {}
}
