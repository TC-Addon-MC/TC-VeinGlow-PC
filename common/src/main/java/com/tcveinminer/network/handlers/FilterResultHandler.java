package com.tcveinminer.network.handlers;

import com.tcveinminer.network.payload.FilterResultData;
import com.tcveinminer.platform.ClientBridge;

public final class FilterResultHandler {
    
    public static void handle(FilterResultData data) {
        ClientBridge.INSTANCE.updateFilterResult(data.allowHighlight());
    }
    
    private FilterResultHandler() {}
}
