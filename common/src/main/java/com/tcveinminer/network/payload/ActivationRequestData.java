package com.tcveinminer.network.payload;

import com.tcveinminer.network.payload.NetworkPacket;
import java.util.Optional;

/** C→S: Client requests activation preview for the looked-at block. */
public record ActivationRequestData(boolean active, Optional<Long> targetPos) implements NetworkPacket {
    public static final String CHANNEL = "tc_veinminer:activation_request";
    @Override public String channelId() { return CHANNEL; }
}
