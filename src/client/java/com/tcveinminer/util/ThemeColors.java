package com.tcveinminer.util;

/**
 * Design tokens — Mystic Tech dark theme (obsidian navy + gold ore).
 * Tất cả màu theo đúng giao diện tham khảo.
 */
public final class ThemeColors {

    // ── Backgrounds ──────────────────────────────────────────────────────
    public static final int BG_SCREEN        = 0xFF030610;
    public static final int BG_WINDOW        = 0xF2080D17;
    public static final int BG_HEADER        = 0xFF0A0F1B;
    public static final int BG_PANEL         = 0xFF0B101C;
    public static final int BG_PANEL_INSET   = 0xFF060A12;
    public static final int BG_ROW           = 0xFF0B101B;
    public static final int BG_ROW_SELECTED  = 0xFF0F172A;
    public static final int BG_ROW_HOVER     = 0xFF0C1322;
    public static final int BG_INPUT         = 0xFF020408;
    // Legacy aliases (giữ cho code cũ compile)
    public static final int BG_ROW_A         = BG_ROW;
    public static final int BG_ROW_B         = BG_ROW_HOVER;

    // ── Borders ──────────────────────────────────────────────────────────
    public static final int BORDER_DEFAULT   = 0xFF1E293B;
    public static final int BORDER_DIM       = 0xFF334155;
    // Legacy
    public static final int BORDER_INNER     = BORDER_DEFAULT;

    // ── Gold ore accents ─────────────────────────────────────────────────
    public static final int GOLD             = 0xFFD8A15B;
    public static final int GOLD_DIM         = 0xFFA07840;
    public static final int GOLD_FILL        = 0x22D8A15B;
    public static final int GOLD_BORDER      = 0x55D8A15B;

    // ── Emerald ──────────────────────────────────────────────────────────
    public static final int EMERALD          = 0xFF10B981;
    public static final int EMERALD_FILL     = 0x1A10B981;
    public static final int EMERALD_BORDER   = 0x4410B981;
    public static final int EMERALD_TEXT     = 0xFF4DDFB0;
    // Legacy
    public static final int TOGGLE_ON_TEXT   = EMERALD_TEXT;

    // ── Purple (custom/designer) ──────────────────────────────────────────
    public static final int PURPLE           = 0xFFA855F7;
    public static final int PURPLE_FILL      = 0x22A855F7;
    public static final int PURPLE_BORDER_DIM= 0x66A855F7;
    public static final int PURPLE_TEXT      = 0xFFD8B4FE;

    // ── Redstone ─────────────────────────────────────────────────────────
    public static final int REDSTONE         = 0xFFEF4444;
    public static final int REDSTONE_FILL    = 0x1AEF4444;
    public static final int REDSTONE_BORDER  = 0x66EF4444;
    public static final int REDSTONE_TEXT    = 0xFFFCA5A5;

    // ── Copper ───────────────────────────────────────────────────────────
    public static final int COPPER_DIM       = 0xFF7A4520;

    // ── Stone neutrals ───────────────────────────────────────────────────
    public static final int STONE            = 0xFF334155;
    public static final int STONE_BORDER     = 0xFF2D3A4E;

    // ── Text ─────────────────────────────────────────────────────────────
    public static final int TEXT_TITLE       = 0xFFD8A15B;
    public static final int TEXT_WHITE       = 0xFFFFFFFF;
    public static final int TEXT_BRIGHT      = 0xFFE2E8F0;
    public static final int TEXT_LABEL       = 0xFF94A3B8;
    public static final int TEXT_DIM         = 0xFF64748B;
    public static final int TEXT_VALUE       = 0xFFD8A15B;
    public static final int TEXT_GOOD        = 0xFF4DDFB0;
    public static final int TEXT_ERROR       = 0xFFFCA5A5;
    public static final int TEXT_HINT        = 0xFF475569;
    // Legacy
    public static final int BTN_TEXT         = TEXT_LABEL;

    // ── Button ────────────────────────────────────────────────────────────
    public static final int BTN_BG           = 0xFF0F172A;
    public static final int BTN_BG_HOVER     = 0xFF1E293B;
    public static final int BTN_BORDER       = 0xFF1E293B;
    public static final int BTN_BORDER_HOVER = 0xFFA07840;
    public static final int BTN_BG_PRESS     = 0xFF060A12;
    // Thêm vào nhóm Text
    public static final int HUD_ON_TEXT      = EMERALD_TEXT;
    public static final int HUD_OFF_TEXT     = TEXT_LABEL;
    private ThemeColors() {}
}
