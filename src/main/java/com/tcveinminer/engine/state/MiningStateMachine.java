package com.tcveinminer.engine.state;

/**
 * State machine cho một mining session của một player.
 *
 * Transitions hợp lệ:
 *   IDLE       → SCANNING   (player break block + hold key)
 *   SCANNING   → QUEUEING   (scan hoàn thành, có kết quả)
 *   SCANNING   → IDLE       (không có block nào match)
 *   QUEUEING   → MINING     (queue đã build xong)
 *   MINING     → IDLE       (queue rỗng — hoàn thành)
 *   MINING     → INTERRUPTED (player thả phím / invalid state)
 *   INTERRUPTED → IDLE       (cleanup xong)
 *   * → FAILED               (lỗi bất ngờ)
 */
public final class MiningStateMachine {

    public enum State {
        IDLE,
        SCANNING,
        QUEUEING,
        MINING,
        INTERRUPTED,
        FAILED
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
            case IDLE        -> to == State.SCANNING;
            case SCANNING    -> to == State.QUEUEING || to == State.IDLE || to == State.FAILED;
            case QUEUEING    -> to == State.MINING   || to == State.IDLE || to == State.FAILED;
            case MINING      -> to == State.IDLE     || to == State.INTERRUPTED || to == State.FAILED;
            case INTERRUPTED -> to == State.IDLE;
            case FAILED      -> to == State.IDLE;
        };
    }
}
