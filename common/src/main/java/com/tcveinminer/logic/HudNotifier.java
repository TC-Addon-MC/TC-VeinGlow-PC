package com.tcveinminer.logic;

/**
 * Static bridge: VeinMinerLogic (main) ghi vào đây,
 * VeinMinerHud (client) đọc ra để hiện HUD message.
 * Không import bất kỳ class client-only nào.
 */
public final class HudNotifier {
    public static int  lastMined = 0;
    public static int  lastMax   = 64;
    public static long notifyAt  = 0; // System.currentTimeMillis() + duration
    public static boolean lastCancelled = false;

    private HudNotifier() {}
}
