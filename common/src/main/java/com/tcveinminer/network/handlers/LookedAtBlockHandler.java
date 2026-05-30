package com.tcveinminer.network.handlers;

import com.tcveinminer.network.payload.LookedAtBlockData;
import com.tcveinminer.platform.ClientBridge;

public final class LookedAtBlockHandler {
    
    public static void handle(LookedAtBlockData data, Object player) {
        ClientBridge.INSTANCE.updateLookedAtBlock(player, data.pos());
    }
    
    private LookedAtBlockHandler() {}
}
