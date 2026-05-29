package com.tcveinminer.engine.state;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized, thread-safe player state store.
 * <p>
 * Replaces the Fabric-coupled {@code TCVeinMinerMod.playersHoldingV} static set.
 * All loader modules write to this registry via their event bridges.
 * The engine reads from it without knowing which loader is active.
 */
public final class PlayerStateRegistry {

    /** Players currently holding the vein-mine activation key. */
    private static final Set<UUID> holdingKey =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Rate limiter for ActivationRequest packets: UUID → lastTimestamp. */
    private static final Map<UUID, Long> activationRateLimit = new ConcurrentHashMap<>();

    // ── Key State ─────────────────────────────────────────────────────────

    public static void setHolding(UUID uuid, boolean holding) {
        if (holding) holdingKey.add(uuid);
        else holdingKey.remove(uuid);
    }

    public static boolean isHoldingKey(UUID uuid) {
        return holdingKey.contains(uuid);
    }

    // ── Rate Limiting ─────────────────────────────────────────────────────

    /**
     * Check whether the given player is allowed to send another ActivationRequest.
     * Enforces a minimum interval of 50 ms (≈ 20 packets/sec max).
     *
     * @return true if the request is allowed; false if rate-limited.
     */
    public static boolean checkActivationRateLimit(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = activationRateLimit.getOrDefault(uuid, 0L);
        if (now - last < 50) return false;
        activationRateLimit.put(uuid, now);
        return true;
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    /**
     * Remove all state for a player (call on disconnect).
     */
    public static void cleanup(UUID uuid) {
        holdingKey.remove(uuid);
        activationRateLimit.remove(uuid);
    }

    private PlayerStateRegistry() {}
}
