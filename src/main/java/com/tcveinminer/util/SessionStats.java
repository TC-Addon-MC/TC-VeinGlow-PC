package com.tcveinminer.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Thống kê session hiện tại, lưu trong RAM — reset khi thoát game.
 */
public final class SessionStats {

    private static long totalBlocks = 0;
    private static long activations = 0;
    private static long durabilityUsed = 0;
    private static long modEnabledAt = -1;
    private static String rarestBlock = "—";
    private static final Map<String, Integer> blockCounts = new HashMap<>();

    public static void onVeinMineStart() {
        activations++;
    }

    public static void onBlockBroken(String blockId) {
        totalBlocks++;
        blockCounts.merge(blockId, 1, Integer::sum);
        // Track "rarest" = least broken block
        blockCounts.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .ifPresent(b -> rarestBlock = b);
    }

    public static void onDurabilityUsed(int amount) {
        durabilityUsed += amount;
    }

    public static void onModEnabled() {
        if (modEnabledAt < 0) modEnabledAt = System.currentTimeMillis();
    }

    public static void onModDisabled() {
        modEnabledAt = -1;
    }

    public static void reset() {
        totalBlocks = 0;
        activations = 0;
        durabilityUsed = 0;
        modEnabledAt = -1;
        rarestBlock = "—";
        blockCounts.clear();
    }

    public static long getTotalBlocks()    { return totalBlocks; }
    public static long getActivations()    { return activations; }
    public static long getDurabilityUsed() { return durabilityUsed; }
    public static String getRarestBlock()  { return rarestBlock; }

    public static String getUptimeFormatted() {
        if (modEnabledAt < 0) return "00:00:00";
        long sec = (System.currentTimeMillis() - modEnabledAt) / 1000;
        return String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
    }

    private SessionStats() {}
}
