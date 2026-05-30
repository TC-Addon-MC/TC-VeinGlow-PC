package com.tcveinminer.api;

import com.tcveinminer.api.event.BlockBreakEvent;
import com.tcveinminer.api.event.SessionEndEvent;
import com.tcveinminer.api.event.SessionStartEvent;
import com.tcveinminer.api.Event;
import net.minecraft.util.ActionResult;

/**
 * Public events exposed by TC VeinMiner.
 * Addons can register callbacks here to hook into the mining lifecycle.
 */
public final class TCVeinMinerEvents {


    /**
     * Fired when a mining session is about to start.
     * Return ActionResult.FAIL to cancel the session.
     */
    public static final Event<SessionStartCallback> SESSION_START = Event.createArrayBacked(SessionStartCallback.class,
        (listeners) -> (event) -> {
            for (SessionStartCallback listener : listeners) {
                ActionResult result = listener.onSessionStart(event);
                if (result != ActionResult.PASS) {
                    return result;
                }
            }
            return ActionResult.PASS;
        });

    /**
     * Fired when a mining session finishes naturally or is cancelled/stopped.
     */
    public static final Event<SessionEndCallback> SESSION_END = Event.createArrayBacked(SessionEndCallback.class,
        (listeners) -> (event) -> {
            for (SessionEndCallback listener : listeners) {
                listener.onSessionEnd(event);
            }
        });

    /**
     * Fired right before each block is broken/processed during a session.
     * Return ActionResult.FAIL to skip processing this specific block (it won't cancel the whole session).
     */
    public static final Event<BlockBreakCallback> BLOCK_BREAK_PRE = Event.createArrayBacked(BlockBreakCallback.class,
        (listeners) -> (event) -> {
            for (BlockBreakCallback listener : listeners) {
                ActionResult result = listener.onBlockBreakPre(event);
                if (result != ActionResult.PASS) {
                    return result;
                }
            }
            return ActionResult.PASS;
        });

    /**
     * Fired right after each block is successfully broken/processed.
     */
    public static final Event<BlockBreakPostCallback> BLOCK_BREAK_POST = Event.createArrayBacked(BlockBreakPostCallback.class,
        (listeners) -> (event) -> {
            for (BlockBreakPostCallback listener : listeners) {
                listener.onBlockBreakPost(event);
            }
        });

    @FunctionalInterface
    public interface SessionStartCallback {
        ActionResult onSessionStart(SessionStartEvent event);
    }

    @FunctionalInterface
    public interface SessionEndCallback {
        void onSessionEnd(SessionEndEvent event);
    }

    @FunctionalInterface
    public interface BlockBreakCallback {
        ActionResult onBlockBreakPre(BlockBreakEvent event);
    }

    @FunctionalInterface
    public interface BlockBreakPostCallback {
        void onBlockBreakPost(BlockBreakEvent event);
    }

    private TCVeinMinerEvents() {}
}
