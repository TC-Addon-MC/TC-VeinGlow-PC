package com.tcveinminer.network.payload;

import com.tcveinminer.network.payload.NetworkPacket;

/** S→C: Whether the looked-at block passes the filter (should highlight). */
public record FilterResultData(boolean allowHighlight) implements NetworkPacket {
    public static final String CHANNEL = "tc_veinminer:filter_result";
    @Override public String channelId() { return CHANNEL; }
}
