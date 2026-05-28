package com.tcveinminer.engine.state;

/**
 * State machine for one action engine session.
 * <p>
 * Each engine (left/right) has its own instance — no shared state.
 *
 * <p>Valid transitions:
 * <pre>
 *   IDLE        → PREVIEW      (activation valid)
 *   PREVIEW     → PROCESSING   (trigger event fires)
 *   PREVIEW     → CANCELLED    (activation disabled)
 *   PREVIEW     → IDLE         (reset)
 *   PROCESSING  → FINISHED     (queue empty — completed)
 *   PROCESSING  → CANCELLED    (item change / key released / error)
 *   FINISHED    → IDLE         (cleanup at next tick)
 *   CANCELLED   → IDLE         (cleanup at next tick)
 * </pre>
 */
public final class EngineStateMachine {

    private EngineState current = EngineState.IDLE;

    public EngineState get() { return current; }

    public boolean is(EngineState s) { return current == s; }

    /**
     * Transition with validation. Returns false if the transition is invalid.
     */
    public boolean transition(EngineState next) {
        if (!isValidTransition(current, next)) return false;
        current = next;
        return true;
    }

    /**
     * Force transition (used for interrupt / reset). Always succeeds.
     */
    public void force(EngineState next) {
        current = next;
    }

    private static boolean isValidTransition(EngineState from, EngineState to) {
        return switch (from) {
            case IDLE       -> to == EngineState.PREVIEW;
            case PREVIEW    -> to == EngineState.PROCESSING || to == EngineState.CANCELLED || to == EngineState.IDLE;
            case PROCESSING -> to == EngineState.FINISHED   || to == EngineState.CANCELLED;
            case FINISHED   -> to == EngineState.IDLE;
            case CANCELLED  -> to == EngineState.IDLE;
        };
    }
}
