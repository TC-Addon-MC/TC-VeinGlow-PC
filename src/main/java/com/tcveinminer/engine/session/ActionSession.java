package com.tcveinminer.engine.session;

import com.tcveinminer.engine.action.ActionContext;
import com.tcveinminer.engine.action.ActionType;
import com.tcveinminer.engine.queue.BlockActionQueue;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates all mutable per-session state for an engine.
 * <p>
 * No action-specific fields — those live in {@link ActionContext} subclasses.
 * Each engine creates a new session for each trigger event.
 */
public final class ActionSession {

    private final BlockActionQueue queue = new BlockActionQueue();
    private Set<BlockPos> lockedSnapshot = new HashSet<>();
    private volatile Set<BlockPos> renderSnapshot = Collections.emptySet();
    private int processedCount = 0;
    private int targetCount = 0;
    private long lastUpdateTime = System.currentTimeMillis();

    /**
     * Per-session re-entry guard.
     * Set to true while the engine is executing actions in the tick processor.
     * Prevents AFTER break events from recursing back into the engine.
     */
    private boolean isProcessing = false;

    private ActionType actionType;
    private ActionContext actionContext;
    private Item initialItem;

    // ── Getters ───────────────────────────────────────────────────────────

    public BlockActionQueue getQueue() { return queue; }
    public Set<BlockPos> getLockedSnapshot() { return lockedSnapshot; }
    public Set<BlockPos> getRenderSnapshot() { return renderSnapshot; }
    public int getProcessedCount() { return processedCount; }
    public int getTargetCount() { return targetCount; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    public boolean isProcessing() { return isProcessing; }
    public ActionType getActionType() { return actionType; }
    public ActionContext getActionContext() { return actionContext; }
    public Item getInitialItem() { return initialItem; }

    // ── Setters ───────────────────────────────────────────────────────────

    public void setActionType(ActionType type) { this.actionType = type; }
    public void setActionContext(ActionContext ctx) { this.actionContext = ctx; }
    public void setInitialItem(Item item) { this.initialItem = item; }
    public void updateTime() { this.lastUpdateTime = System.currentTimeMillis(); }

    // ── Processing guard ──────────────────────────────────────────────────

    public void beginProcessing() { isProcessing = true; }
    public void endProcessing() { isProcessing = false; }

    // ── Snapshot management ───────────────────────────────────────────────

    public void initSnapshot(Set<BlockPos> positions) {
        lockedSnapshot = new HashSet<>(positions);
        renderSnapshot = new HashSet<>(lockedSnapshot);
        targetCount = lockedSnapshot.size();
        processedCount = 0;
    }

    public void removeFromSnapshot(BlockPos pos) {
        lockedSnapshot.remove(pos);
    }

    public void updateRenderSnapshot() {
        renderSnapshot = new HashSet<>(lockedSnapshot);
    }

    public void incrementProcessed() {
        processedCount++;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    /**
     * Full reset — prepare for a new session.
     */
    public void reset() {
        queue.reset();
        lockedSnapshot.clear();
        renderSnapshot = Collections.emptySet();
        processedCount = 0;
        targetCount = 0;
        isProcessing = false;
        actionType = null;
        actionContext = null;
        initialItem = null;
    }

    /**
     * Clear visuals and queue (used for stop/cancel).
     */
    public void clear() {
        isProcessing = false;
        queue.interrupt();
        lockedSnapshot.clear();
        renderSnapshot = Collections.emptySet();
    }
}
