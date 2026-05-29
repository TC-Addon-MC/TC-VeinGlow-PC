package com.tcveinminer.engine.preview;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.tcveinminer.network.HighlightBlockListPayload;

import java.util.*;

/**
 * Preview manager — handles dual-source highlight rendering.
 * <p>
 * This is a rendering concern, separated from the action engines.
 * Manages left and right preview snapshots independently and sends
 * merged highlight packets to the client.
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
     * Send highlight update for the given source to the client.
     */
    public void sendHighlight(ServerPlayerEntity spe, String source, Set<BlockPos> blocks, String style) {
        updatePreview(source, blocks, style);
        ServerPlayNetworking.send(spe, new HighlightBlockListPayload(
                new ArrayList<>(blocks), style, source));
    }

    /**
     * Send empty highlight for the given source.
     */
    public void sendClearHighlight(ServerPlayerEntity spe, String source) {
        clear(source);
        ServerPlayNetworking.send(spe, new HighlightBlockListPayload(
                Collections.emptyList(), "FACE", source));
    }
}
