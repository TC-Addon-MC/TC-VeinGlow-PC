package com.tcveinminer.engine.preview;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import com.tcveinminer.network.NetworkManager;
import com.tcveinminer.network.payload.HighlightBlockListData;

import java.util.*;

/**
 * Preview manager — handles dual-source highlight rendering.
 * <p>
 * This is a rendering concern, separated from the action engines.
 * Manages left and right preview snapshots independently and sends
 * merged highlight packets to the minecraft.
 */
public final class PreviewManager {

    private Set<BlockPos> leftPreview = Collections.emptySet();
    private Set<BlockPos> rightPreview = Collections.emptySet();
    private String leftStyle = "FACE";
    private String rightStyle = "FACE";

    /**
     * Update preview for the given source.
     *
     * @param source "LEFT" or "RIGHT"
     * @param blocks the block positions to highlight
     * @param style  the highlight style
     */
    public void updatePreview(String source, Set<BlockPos> blocks, String style) {
        if ("LEFT".equals(source)) {
            leftPreview = blocks != null ? blocks : Collections.emptySet();
            leftStyle = style;
        } else {
            rightPreview = blocks != null ? blocks : Collections.emptySet();
            rightStyle = style;
        }
    }

    /**
     * Clear preview for the given source.
     */
    public void clear(String source) {
        updatePreview(source, Collections.emptySet(), "FACE");
    }

    /**
     * Clear all previews.
     */
    public void clearAll() {
        leftPreview = Collections.emptySet();
        rightPreview = Collections.emptySet();
    }

    /**
     * Get the left preview blocks.
     */
    public Set<BlockPos> getLeftPreview() { return leftPreview; }

    /**
     * Get the right preview blocks.
     */
    public Set<BlockPos> getRightPreview() { return rightPreview; }

    /**
     * Send highlight update for the given source to the minecraft.
     */
    public void sendHighlight(ServerPlayerEntity spe, String source, Set<BlockPos> blocks, String style) {
        updatePreview(source, blocks, style);
        NetworkManager.sendToPlayer(spe, new HighlightBlockListData(
                blocks.stream().map(BlockPos::asLong).toList(), style, source));
    }

    /**
     * Send empty highlight for the given source.
     */
    public void sendClearHighlight(ServerPlayerEntity spe, String source) {
        clear(source);
        NetworkManager.sendToPlayer(spe, new HighlightBlockListData(
                Collections.emptyList(), "FACE", source));
    }
}
