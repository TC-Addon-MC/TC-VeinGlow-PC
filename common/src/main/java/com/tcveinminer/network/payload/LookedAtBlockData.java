package com.tcveinminer.network.payload;

import com.tcveinminer.network.NetworkPacket;
import net.minecraft.util.math.BlockPos;

/** S→C: Reports the exact block position the server resolved as the target. */
public record LookedAtBlockData(BlockPos pos) implements NetworkPacket {
    public static final String CHANNEL = "tc_veinminer:looked_at_block";
    @Override public String channelId() { return CHANNEL; }
}
