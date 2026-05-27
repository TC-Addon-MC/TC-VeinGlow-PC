package com.tcveinminer.engine.state;

/**
 * State machine cho một mining session của một player.
 *
 * Transitions hợp lệ:
 *   IDLE          → PREVIEW        (activation valid)
 *   PREVIEW       → LOCKED_MINING  (player breaks first block)
 *   PREVIEW       → CANCELLED      (activation disabled)
 *   PREVIEW       → IDLE           (reset)
 *   LOCKED_MINING → FINISHED       (queue rỗng — hoàn thành)
 *   LOCKED_MINING → CANCELLED      (player đổi item (nếu không allow) / lỗi)
 *   FINISHED      → IDLE           (cleanup xong ở tick tiếp theo)
 *   CANCELLED     → IDLE           (cleanup xong ở tick tiếp theo)
 */
public final class MiningStateMachine {

    public enum State {
        IDLE,
        PREVIEW,
        LOCKED_MINING,
        FINISHED,
        CANCELLED
    }

    private State current = State.IDLE;

    public State get() { return current; }

    public boolean is(State s) { return current == s; }

    /** Chuyển state với validation. Trả về false nếu transition không hợp lệ. */
    public boolean transition(State next) {
        if (!isValidTransition(current, next)) return false;
        current = next;
        return true;
    }

    /** Force transition (dùng cho interrupt / reset). Luôn thành công. */
    public void force(State next) {
        current = next;
    }

    private boolean isValidTransition(State from, State to) {
        return switch (from) {
            case IDLE          -> to == State.PREVIEW;
            case PREVIEW       -> to == State.LOCKED_MINING || to == State.CANCELLED || to == State.IDLE;
            case LOCKED_MINING -> to == State.FINISHED      || to == State.CANCELLED;
            case FINISHED      -> to == State.IDLE;
            case CANCELLED     -> to == State.IDLE;
        };
    }
}
