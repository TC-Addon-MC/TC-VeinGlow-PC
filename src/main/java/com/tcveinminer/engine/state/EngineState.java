package com.tcveinminer.engine.state;

/**
 * Unified state enum used by both left-click and right-click engines.
 * Each engine has its own {@link EngineStateMachine} instance — no shared state.
 */
public enum EngineState {
    /** No active session */
    IDLE,
    /** Block preview/highlight mode (player holding activation key) */
    PREVIEW,
    /** Actively processing queue (MINING for left, INTERACTING for right) */
    PROCESSING,
    /** Session completed successfully */
    FINISHED,
    /** Session cancelled (item change, key release, error) */
    CANCELLED
}
