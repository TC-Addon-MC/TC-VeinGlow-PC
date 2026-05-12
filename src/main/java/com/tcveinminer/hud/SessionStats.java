package com.tcveinminer.hud;

import java.util.*;

/** Thống kê session — RAM only, reset khi gọi reset(). */
public final class SessionStats {
    public static long   totalBlocks    = 0;
    public static long   activations    = 0;
    public static long   durabilityUsed = 0;
    public static String rarestBlock    = "—";

    private static long enabledAt = -1;
    private static final Map<String, Integer> counts = new HashMap<>();

    public static void onActivate()        { activations++; }
    public static void onDurability(int n) { durabilityUsed += n; }
    public static void onModEnabled()      { if (enabledAt < 0) enabledAt = System.currentTimeMillis(); }
    public static void onModDisabled()     { enabledAt = -1; }

    public static void onBlock(String id) {
        totalBlocks++;
        counts.merge(id, 1, Integer::sum);
        counts.entrySet().stream().min(Map.Entry.comparingByValue())
              .map(Map.Entry::getKey).ifPresent(b -> rarestBlock = b);
    }

    public static void reset() {
        totalBlocks = activations = durabilityUsed = 0;
        rarestBlock = "—"; enabledAt = -1; counts.clear();
    }

    public static String uptime() {
        if (enabledAt < 0) return "00:00:00";
        long s = (System.currentTimeMillis() - enabledAt) / 1000;
        return String.format("%02d:%02d:%02d", s/3600, (s%3600)/60, s%60);
    }

    private SessionStats() {}
}
