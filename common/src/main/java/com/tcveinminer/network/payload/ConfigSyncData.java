package com.tcveinminer.network.payload;

import com.tcveinminer.network.payload.NetworkPacket;
import java.util.List;

/** S→C: Server-to-client config synchronization on join and reload. */
public record ConfigSyncData(
        int maxBlocks,
        List<String> blacklistedBlocks
) implements NetworkPacket {
    public static final String CHANNEL = "tc_veinminer:config_sync";
    @Override public String channelId() { return CHANNEL; }
}
