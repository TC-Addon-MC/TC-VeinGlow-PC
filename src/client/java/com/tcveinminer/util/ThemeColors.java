package com.tcveinminer.util;

/**
 * Design system color constants cho TC-VeinMiner GUI.
 * Tất cả màu ở đây để dễ chỉnh sửa sau này.
 */
public final class ThemeColors {

    // ── Backgrounds ──────────────────────────────────────────────────
    public static final int BG_MAIN         = 0xCC0A0A0F; // window bg ~80% alpha
    public static final int BG_HEADER       = 0xFF12122A;
    public static final int BG_ROW_A        = 0xFF0D0D1A; // list row alternating
    public static final int BG_ROW_B        = 0xFF12122A;
    public static final int BG_ROW_HOVER    = 0xFF1E1E3A;

    // ── Border gradient ───────────────────────────────────────────────
    public static final int BORDER_START    = 0xFF7B2FBE; // purple
    public static final int BORDER_MID      = 0xFF2F86BE; // blue
    public static final int BORDER_END      = 0xFF2FBE7B; // green
    public static final int BORDER_INNER    = 0xFF1A1A2E;

    // ── Header divider ────────────────────────────────────────────────
    public static final int DIVIDER_START   = 0xFF7B2FBE;
    public static final int DIVIDER_END     = 0xFF2FBE7B;

    // ── Button Normal ─────────────────────────────────────────────────
    public static final int BTN_BG          = 0xFF1E1E3A;
    public static final int BTN_BORDER      = 0xFF4A3F7A;
    public static final int BTN_TEXT        = 0xFFC8A8FF;
    public static final int BTN_BG_HOVER    = 0xFF2A2A52;
    public static final int BTN_BORDER_HOVER= 0xFF7B5FFF;
    public static final int BTN_BG_PRESS    = 0xFF0E0E1F;

    // ── Toggle ON ─────────────────────────────────────────────────────
    public static final int TOGGLE_ON_BG_A  = 0xFF1B4332;
    public static final int TOGGLE_ON_BG_B  = 0xFF2D6A4F;
    public static final int TOGGLE_ON_BORDER= 0xFF52B788;
    public static final int TOGGLE_ON_TEXT  = 0xFF95D5B2;
    public static final int TOGGLE_ON_DOT   = 0xFF52B788;

    // ── Toggle OFF ────────────────────────────────────────────────────
    public static final int TOGGLE_OFF_BG   = 0xFF1A0A0A;
    public static final int TOGGLE_OFF_BORDER=0xFF6B2737;
    public static final int TOGGLE_OFF_TEXT = 0xFFFF8FA3;
    public static final int TOGGLE_OFF_DOT  = 0xFF6B2737;

    // ── Slider ───────────────────────────────────────────────────────
    public static final int SLIDER_TRACK    = 0xFF0D0D1A;
    public static final int SLIDER_BORDER   = 0xFF3A3A6A;
    public static final int SLIDER_FILL_A   = 0xFF7B2FBE;
    public static final int SLIDER_FILL_B   = 0xFF2F86BE;
    public static final int SLIDER_THUMB    = 0xFFC8A8FF;
    public static final int SLIDER_THUMB_B  = 0xFF7B5FFF;

    // ── Text ─────────────────────────────────────────────────────────
    public static final int TEXT_TITLE      = 0xFFE8D5FF;
    public static final int TEXT_LABEL      = 0xFFA8A8C8;
    public static final int TEXT_VALUE      = 0xFF7BDFFF;
    public static final int TEXT_WARNING    = 0xFFFFB347;
    public static final int TEXT_ERROR      = 0xFFFF6B8A;
    public static final int TEXT_WHITE      = 0xFFFFFFFF;

    // ── Danger button ─────────────────────────────────────────────────
    public static final int BTN_DANGER_BG   = 0xFF2A0A0A;
    public static final int BTN_DANGER_BORDER=0xFF8B2737;
    public static final int BTN_DANGER_TEXT  = 0xFFFF6B8A;

    // ── Stats / HUD ───────────────────────────────────────────────────
    public static final int HUD_ON_BG       = 0xCC1B4332;
    public static final int HUD_ON_TEXT     = 0xFF95D5B2;
    public static final int HUD_ON_BORDER   = 0xFF52B788;
    public static final int HUD_OFF_BG      = 0xCC1A0A0A;
    public static final int HUD_OFF_TEXT    = 0xFFFF8FA3;
    public static final int HUD_OFF_BORDER  = 0xFF6B2737;

    private ThemeColors() {}
}
