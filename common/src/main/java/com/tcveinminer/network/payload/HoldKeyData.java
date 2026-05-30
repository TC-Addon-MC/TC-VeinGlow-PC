package com.tcveinminer.network.payload;

import com.tcveinminer.network.payload.NetworkPacket;
import java.util.List;

/**
 * C→S: Sent every tick the player changes key/shape state.
 * Pure data record — no loader coupling.
 */
public record HoldKeyData(
        boolean isHolding,
        String shapeId,
        int maxBlocks,
        String equation,
        List<String> blacklist
) implements NetworkPacket {
    public static final String CHANNEL = "tc_veinminer:hold_key";

    @Override public String channelId() { return CHANNEL; }
}
