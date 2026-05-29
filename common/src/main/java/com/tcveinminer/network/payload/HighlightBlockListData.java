package com.tcveinminer.network.payload;

import com.tcveinminer.network.NetworkPacket;
import net.minecraft.util.math.BlockPos;
import java.util.List;

/** S→C: Full replacement of the highlight block list (used on session start). */
public record HighlightBlockListData(
        List<BlockPos> blocks,
        String highlightStyle
) implements NetworkPacket {
    public static final String CHANNEL = "tc_veinminer:highlight_block_list";
    @Override public String channelId() { return CHANNEL; }
}
