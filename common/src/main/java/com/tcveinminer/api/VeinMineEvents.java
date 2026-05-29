package com.tcveinminer.api;

import com.tcveinminer.api.event.BlockBreakEvent;
import com.tcveinminer.api.event.SessionEndEvent;
import com.tcveinminer.api.event.SessionStartEvent;
import com.tcveinminer.event.EventBus;

/**
 * Public events exposed by TC VeinGlow.
 * <p>
 * Addon mods and compatibility layers register callbacks here to hook into the mining lifecycle.
 * This class has ZERO loader-specific imports — it works on Fabric, NeoForge, and Forge.
 * <p>
 * Replaces the old {@code TCVeinMinerEvents} which imported {@code net.fabricmc.fabric.api.event.*}.
 *
 * <h3>Event Result semantics</h3>
 * <ul>
 *   <li>{@link EventResult#PASS} — allow the action to proceed</li>
 *   <li>{@link EventResult#DENY} — cancel/skip the action</li>
 * </ul>
 */
public final class VeinMineEvents {

    /**
     * Result type for cancellable events.
     * Replaces {@code net.minecraft.util.ActionResult} in event callbacks
     * so the API is 100% loader-agnostic.
     */
    public enum EventResult { PASS, DENY }

    // ── Session lifecycle ─────────────────────────────────────────────────

    /**
     * Fired when a vein-mining session is about to start.
     * Return {@link EventResult#DENY} to cancel the entire session.
     */
    public static final EventBus<SessionStartCallback> SESSION_START = EventBus.create(
            listeners -> event -> {
                for (var l : listeners) {
                    if (l.onSessionStart(event) == EventResult.DENY) return EventResult.DENY;
                }
                return EventResult.PASS;
            }
    );

    /**
     * Fired when a session finishes naturally or is cancelled.
     * Not cancellable.
     */
    public static final EventBus<SessionEndCallback> SESSION_END = EventBus.create(
            listeners -> event -> {
                for (var l : listeners) l.onSessionEnd(event);
            }
    );

    // ── Per-block events ──────────────────────────────────────────────────

    /**
     * Fired right before each block is processed in a session.
     * Return {@link EventResult#DENY} to skip this specific block
     * (the session continues with the next block).
     */
    public static final EventBus<BlockBreakCallback> BLOCK_BREAK_PRE = EventBus.create(
            listeners -> event -> {
                for (var l : listeners) {
                    if (l.onBlockBreakPre(event) == EventResult.DENY) return EventResult.DENY;
                }
                return EventResult.PASS;
            }
    );

    /**
     * Fired right after a block is successfully processed.
     * Not cancellable.
     */
    public static final EventBus<BlockBreakPostCallback> BLOCK_BREAK_POST = EventBus.create(
            listeners -> event -> {
                for (var l : listeners) l.onBlockBreakPost(event);
            }
    );

    // ── Callback interfaces ───────────────────────────────────────────────

    @FunctionalInterface
    public interface SessionStartCallback {
        EventResult onSessionStart(SessionStartEvent event);
    }

    @FunctionalInterface
    public interface SessionEndCallback {
        void onSessionEnd(SessionEndEvent event);
    }

    @FunctionalInterface
    public interface BlockBreakCallback {
        EventResult onBlockBreakPre(BlockBreakEvent event);
    }

    @FunctionalInterface
    public interface BlockBreakPostCallback {
        void onBlockBreakPost(BlockBreakEvent event);
    }

    private VeinMineEvents() {}
}
