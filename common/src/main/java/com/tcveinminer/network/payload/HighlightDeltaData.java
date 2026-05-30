package com.tcveinminer.network.payload;

import com.tcveinminer.network.payload.NetworkPacket;
import java.util.List;

/**
 * S→C: Incremental highlight update — only added/removed blocks are sent.
 * More bandwidth-efficient than resending the full list every tick.
 */
public record HighlightDeltaData(
        List<Long> addedBlocks,
        List<Long> removedBlocks,
        String highlightStyle,
        String source  // "LEFT" or "RIGHT"
) implements NetworkPacket {
    public static final String CHANNEL = "tc_veinminer:highlight_delta";
    @Override public String channelId() { return CHANNEL; }
}
