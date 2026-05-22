package com.tcveinminer.util;

/**
 * Design Tokens — Mystic Tech UI
 *
 * Style:
 * - Obsidian navy
 * - Soft gold ore
 * - Modern purple accent
 * - Minecraft tech aesthetic
 *
 * NOTE:
 * - Giữ toàn bộ biến cũ để tránh crash codebase.
 * - Chỉ nâng cấp palette + consistency.
 */
public final class ThemeColors {

    // ─────────────────────────────────────────────────────────────
    // APP
    // ─────────────────────────────────────────────────────────────

    public static final int APP_BG            = 0xFF0B1120;

    // ─────────────────────────────────────────────────────────────
    // BACKGROUNDS
    // ─────────────────────────────────────────────────────────────

    public static final int BG_SCREEN         = 0xFF030712;

    public static final int BG_WINDOW         = 0xF20B1220;

    public static final int BG_HEADER         = 0xFF101827;

    public static final int BG_PANEL          = 0xFF111827;

    public static final int BG_PANEL_INSET    = 0xFF09111D;

    public static final int BG_ROW            = 0xFF131D2E;

    public static final int BG_ROW_SELECTED   = 0xFF1B2940;

    public static final int BG_ROW_HOVER      = 0xFF182336;

    public static final int BG_INPUT          = 0xFF060B14;

    // Legacy aliases
    public static final int BG_ROW_A          = BG_ROW;
    public static final int BG_ROW_B          = BG_ROW_HOVER;

    // ─────────────────────────────────────────────────────────────
    // BORDERS
    // ─────────────────────────────────────────────────────────────

    public static final int BORDER_DEFAULT    = 0xFF334155;

    public static final int BORDER_DIM        = 0xFF475569;

    public static final int BORDER_INNER      = BORDER_DEFAULT;

    // ─────────────────────────────────────────────────────────────
    // GOLD / AMBER
    // ─────────────────────────────────────────────────────────────

    public static final int GOLD              = 0xFFD8A15B;

    public static final int GOLD_DIM          = 0xFFB07A35;

    public static final int GOLD_FILL         = 0x22D8A15B;

    public static final int GOLD_BORDER       = 0x66D8A15B;

    // Amber buttons
    public static final int BTN_AMBER_GRAD_TOP            = 0xFFF6C453;

    public static final int BTN_AMBER_GRAD_BOTTOM         = 0xFFB8791E;

    public static final int BTN_AMBER_BORDER              = 0xFFFFD978;

    public static final int BTN_AMBER_HOVER_GRAD_TOP      = 0xFFFFD978;

    public static final int BTN_AMBER_HOVER_GRAD_BOTTOM   = 0xFFD4932F;

    // ─────────────────────────────────────────────────────────────
    // EMERALD
    // ─────────────────────────────────────────────────────────────

    public static final int EMERALD           = 0xFF10B981;

    public static final int EMERALD_FILL      = 0x2210B981;

    public static final int EMERALD_BORDER    = 0x6610B981;

    public static final int EMERALD_TEXT      = 0xFF6EE7B7;

    // Legacy
    public static final int TOGGLE_ON_TEXT    = EMERALD_TEXT;

    public static final int HUD_ON_TEXT       = EMERALD_TEXT;

    // ─────────────────────────────────────────────────────────────
    // PURPLE
    // ─────────────────────────────────────────────────────────────

    public static final int PURPLE            = 0xFF8B5CF6;

    public static final int PURPLE_FILL       = 0x228B5CF6;

    public static final int PURPLE_BORDER_DIM = 0x668B5CF6;

    public static final int PURPLE_TEXT       = 0xFFD8B4FE;

    // Purple buttons
    public static final int BTN_PURPLE_GRAD_TOP           = 0xFF8B5CF6;

    public static final int BTN_PURPLE_GRAD_BOTTOM        = 0xFF5B3FD1;

    public static final int BTN_PURPLE_BORDER             = 0xFFC4B5FD;

    public static final int BTN_PURPLE_HOVER_GRAD_TOP     = 0xFFA78BFA;

    public static final int BTN_PURPLE_HOVER_GRAD_BOTTOM  = 0xFF6D55E2;

    // ─────────────────────────────────────────────────────────────
    // REDSTONE / DANGER
    // ─────────────────────────────────────────────────────────────

    public static final int REDSTONE          = 0xFFEF4444;

    public static final int REDSTONE_FILL     = 0x22EF4444;

    public static final int REDSTONE_BORDER   = 0x66EF4444;

    public static final int REDSTONE_TEXT     = 0xFFFCA5A5;

    // Red buttons
    public static final int BTN_RED_GRAD_TOP            = 0xFFEF4444;

    public static final int BTN_RED_GRAD_BOTTOM         = 0xFF991B1B;

    public static final int BTN_RED_BORDER              = 0xFFF87171;

    public static final int BTN_RED_HOVER_GRAD_TOP      = 0xFFF87171;

    public static final int BTN_RED_HOVER_GRAD_BOTTOM   = 0xFFB91C1C;

    // ─────────────────────────────────────────────────────────────
    // COPPER / STONE
    // ─────────────────────────────────────────────────────────────

    public static final int COPPER_DIM        = 0xFF8B5A2B;

    public static final int STONE             = 0xFF475569;

    public static final int STONE_BORDER      = 0xFF334155;

    // ─────────────────────────────────────────────────────────────
    // TEXT
    // ─────────────────────────────────────────────────────────────

    public static final int TEXT_TITLE        = 0xFFFFD978;

    public static final int TEXT_WHITE        = 0xFFFFFFFF;

    public static final int TEXT_BRIGHT       = 0xFFF1F5F9;

    public static final int TEXT_LABEL        = 0xFF94A3B8;

    public static final int TEXT_DIM          = 0xFF64748B;

    public static final int TEXT_VALUE        = 0xFFFFD978;

    public static final int TEXT_GOOD         = 0xFF6EE7B7;

    public static final int TEXT_ERROR        = 0xFFFCA5A5;

    public static final int TEXT_HINT         = 0xFF475569;

    public static final int TEXT_SELECTED     = 0xFFFFD978;

    public static final int TEXT_HOVER        = 0xFFFFFFFF;

    // Legacy
    public static final int BTN_TEXT          = TEXT_LABEL;

    public static final int HUD_OFF_TEXT      = TEXT_LABEL;

    // ─────────────────────────────────────────────────────────────
    // GENERIC BUTTONS
    // ─────────────────────────────────────────────────────────────

    public static final int BTN_BG            = 0xFF111827;

    public static final int BTN_BG_HOVER      = 0xFF1B2940;

    public static final int BTN_BG_PRESS      = 0xFF0A1220;

    public static final int BTN_BORDER        = 0xFF334155;

    public static final int BTN_BORDER_HOVER  = 0xFFD8A15B;

    // ─────────────────────────────────────────────────────────────
    // PRIMARY / TAB BUTTONS
    // ─────────────────────────────────────────────────────────────

    public static final int BTN_PRIMARY_BG                 = 0xFF131D2E;

    public static final int BTN_PRIMARY_HOVER_BG           = 0xFF1B2940;

    public static final int BTN_PRIMARY_BORDER             = 0xFF334155;

    public static final int BTN_PRIMARY_SELECTED_BORDER    = 0xFFD8A15B;

    // ─────────────────────────────────────────────────────────────
    // LIST BUTTONS
    // ─────────────────────────────────────────────────────────────

    public static final int BTN_LIST_BG                    = 0x221E293B;

    public static final int BTN_LIST_HOVER_BG              = 0x44334155;

    public static final int BTN_LIST_BORDER                = 0x55334155;

    public static final int BTN_LIST_HOVER_BORDER          = 0x88738AA3;

    // ─────────────────────────────────────────────────────────────
    // CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────

    private ThemeColors() {}
}