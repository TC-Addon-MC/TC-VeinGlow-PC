package com.tcveinminer.network.payload;

import com.tcveinminer.network.NetworkPacket;

/**
 * S→C: Mining session state update.
 * state: 1=mining, 2=finished, 3=cancelled.
 */
public record MiningStateData(int state, int broken, int target) implements NetworkPacket {
    public static final String CHANNEL = "tc_veinminer:mining_state";
    @Override public String channelId() { return CHANNEL; }
}
