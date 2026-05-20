package com.tcveinminer.util;

/**
 * Design token màu cho TC-VeinMiner — phong cách Mystic Tech.
 * Tất cả màu tập trung ở đây, KHÔNG hard-code rải rắc trong các screen.
 *
 * Palette:
 *   Obsidian backgrounds  → BG_*
 *   Gold ore accents      → GOLD_*
 *   Emerald energy        → EM_*
 *   Copper warmth         → COPPER_*
 *   Redstone alert        → RS_*
 *   Stone neutrals        → STONE_*
 *   Text hierarchy        → TEXT_*
 */
public final class ThemeColors {

    // ── Backgrounds ──────────────────────────────────────────────────────
    /** Cửa sổ chính, ~85% alpha, obsidian navy */
    public static final int BG_WINDOW       = 0xD90A0E1A;
    /** Header/panel tối hơn một bậc */
    public static final int BG_HEADER       = 0xFF0F172A;
    /** Panel nội dung */
    public static final int BG_PANEL        = 0xFF1E293B;
    /** Panel nhỏ / inset */
    public static final int BG_PANEL_INSET  = 0xFF152030;
    /** Row A xen kẽ trong danh sách */
    public static final int BG_ROW_A        = 0xFF0F172A;
    /** Row B xen kẽ */
    public static final int BG_ROW_B        = 0xFF162030;
    /** Row hover */
    public static final int BG_ROW_HOVER    = 0xFF1E2D42;

    // ── Gold ore accents ─────────────────────────────────────────────────
    /** Vàng quặng chính */
    public static final int GOLD            = 0xFFD8A15B;
    /** Vàng mờ hơn cho border thường */
    public static final int GOLD_DIM        = 0xFFA07840;
    /** Vàng alpha thấp cho fill nền */
    public static final int GOLD_FILL       = 0x22D8A15B;
    /** Vàng alpha vừa cho border active */
    public static final int GOLD_BORDER     = 0x55D8A15B;
    /** Vàng gradient start (cho header divider) */
    public static final int GOLD_GRAD_A     = 0xFFD8A15B;
    /** Vàng gradient end (mờ đi) */
    public static final int GOLD_GRAD_B     = 0x33D8A15B;

    // ── Emerald energy ───────────────────────────────────────────────────
    /** Emerald on / active */
    public static final int EMERALD         = 0xFF10B981;
    /** Emerald dim */
    public static final int EMERALD_DIM     = 0xFF0A7A52;
    /** Emerald fill */
    public static final int EMERALD_FILL    = 0x1A10B981;
    /** Emerald border */
    public static final int EMERALD_BORDER  = 0x4410B981;
    /** Text màu emerald */
    public static final int EMERALD_TEXT    = 0xFF4DDFB0;

    // ── Copper warmth ────────────────────────────────────────────────────
    public static final int COPPER          = 0xFFC97C3A;
    public static final int COPPER_DIM      = 0xFF7A4520;
    public static final int COPPER_FILL     = 0x1AC97C3A;
    public static final int COPPER_TEXT     = 0xFFE8A870;

    // ── Redstone alert ───────────────────────────────────────────────────
    public static final int REDSTONE        = 0xFFE54A2E;
    public static final int REDSTONE_DIM    = 0xFF8B2213;
    public static final int REDSTONE_FILL   = 0x1AE54A2E;
    public static final int REDSTONE_BORDER = 0x44E54A2E;
    public static final int REDSTONE_TEXT   = 0xFFE8806A;

    // ── Stone neutrals ───────────────────────────────────────────────────
    public static final int STONE           = 0xFF2D3A4E;
    public static final int STONE_LIGHT     = 0xFF3D4E63;
    public static final int STONE_BORDER    = 0xFF2D3A4E;

    // ── Text hierarchy ───────────────────────────────────────────────────
    /** Tiêu đề chính — kem vàng */
    public static final int TEXT_TITLE      = 0xFFE8DCC8;
    /** Label thường — gray xanh */
    public static final int TEXT_LABEL      = 0xFF8A9AB0;
    /** Giá trị số — vàng ore */
    public static final int TEXT_VALUE      = 0xFFD8A15B;
    /** Giá trị tốt — emerald */
    public static final int TEXT_GOOD       = 0xFF4DDFB0;
    /** Cảnh báo — copper */
    public static final int TEXT_WARN       = 0xFFE8A870;
    /** Lỗi / danger — redstone */
    public static final int TEXT_ERROR      = 0xFFE8806A;
    /** Trắng thuần */
    public static final int TEXT_WHITE      = 0xFFFFFFFF;
    /** Mờ nhạt / hint */
    public static final int TEXT_HINT       = 0xFF576070;

    // ── Legacy aliases (giữ để không vỡ code cũ dùng tạm) ───────────────
    /** @deprecated dùng GOLD_DIM */
    @Deprecated public static final int BTN_BG          = 0xFF1E293B;
    @Deprecated public static final int BTN_BORDER      = 0xFF2D3A4E;
    @Deprecated public static final int BTN_TEXT        = 0xFFD8A15B;
    @Deprecated public static final int BTN_BG_HOVER    = 0xFF243044;
    @Deprecated public static final int BTN_BORDER_HOVER= 0xFFA07840;
    @Deprecated public static final int BTN_BG_PRESS    = 0xFF0F172A;
    @Deprecated public static final int TOGGLE_ON_BG_A  = EMERALD_FILL;
    @Deprecated public static final int TOGGLE_ON_BG_B  = 0xFF0A2A1E;
    @Deprecated public static final int TOGGLE_ON_BORDER= EMERALD;
    @Deprecated public static final int TOGGLE_ON_TEXT  = EMERALD_TEXT;
    @Deprecated public static final int TOGGLE_ON_DOT   = EMERALD;
    @Deprecated public static final int TOGGLE_OFF_BG   = 0xFF1A0A0A;
    @Deprecated public static final int TOGGLE_OFF_BORDER=REDSTONE_DIM;
    @Deprecated public static final int TOGGLE_OFF_TEXT = REDSTONE_TEXT;
    @Deprecated public static final int TOGGLE_OFF_DOT  = REDSTONE_DIM;
    @Deprecated public static final int SLIDER_TRACK    = BG_PANEL_INSET;
    @Deprecated public static final int SLIDER_BORDER   = STONE;
    @Deprecated public static final int SLIDER_FILL_A   = GOLD_DIM;
    @Deprecated public static final int SLIDER_FILL_B   = GOLD;
    @Deprecated public static final int SLIDER_THUMB    = GOLD;
    @Deprecated public static final int SLIDER_THUMB_B  = GOLD_DIM;
    @Deprecated public static final int BORDER_START    = GOLD_DIM;
    @Deprecated public static final int BORDER_MID      = GOLD;
    @Deprecated public static final int BORDER_END      = COPPER;
    @Deprecated public static final int BORDER_INNER    = BG_PANEL;
    @Deprecated public static final int DIVIDER_START   = GOLD_DIM;
    @Deprecated public static final int DIVIDER_END     = COPPER_DIM;
    @Deprecated public static final int BTN_DANGER_BG   = 0xFF1A0808;
    @Deprecated public static final int BTN_DANGER_BORDER=REDSTONE_DIM;
    @Deprecated public static final int BTN_DANGER_TEXT  = REDSTONE_TEXT;
    @Deprecated public static final int HUD_ON_BG       = 0xCC0A1A0F;
    @Deprecated public static final int HUD_ON_TEXT     = EMERALD_TEXT;
    @Deprecated public static final int HUD_ON_BORDER   = EMERALD;
    @Deprecated public static final int HUD_OFF_BG      = 0xCC1A0A0A;
    @Deprecated public static final int HUD_OFF_TEXT    = REDSTONE_TEXT;
    @Deprecated public static final int HUD_OFF_BORDER  = REDSTONE_DIM;

    private ThemeColors() {}
}
