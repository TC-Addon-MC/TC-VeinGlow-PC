package com.tcveinminer.network.payload;

import com.tcveinminer.network.payload.NetworkPacket;

/** S→C: Server confirms whether vein-mine is active for the current target. */
public record ActivationConfirmData(boolean allowContinuous) implements NetworkPacket {
    public static final String CHANNEL = "tc_veinminer:activation_confirm";
    @Override public String channelId() { return CHANNEL; }
}
