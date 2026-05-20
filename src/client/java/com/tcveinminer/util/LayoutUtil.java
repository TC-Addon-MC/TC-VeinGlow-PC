package com.tcveinminer.util;

/**
 * Hằng số layout dùng chung — spacing, row step, column width, etc.
 * Các Screen đọc từ đây thay vì tự định nghĩa magic number.
 */
public final class LayoutUtil {

    // ── Kích thước cửa sổ chuẩn ──────────────────────────────────────────
    public static final int SCREEN_W_SM   = 280;
    public static final int SCREEN_W_MD   = 320;
    public static final int SCREEN_W_LG   = 380;

    // ── Header ───────────────────────────────────────────────────────────
    public static final int HEADER_H      = 26;
    public static final int HEADER_PAD_X  = 12;
    public static final int HEADER_PAD_Y  = 8;

    // ── Tab bar ──────────────────────────────────────────────────────────
    public static final int TAB_H         = 20;
    public static final int TAB_PAD_X     = 10;

    // ── Content area ─────────────────────────────────────────────────────
    public static final int CONTENT_PAD_X = 14;
    public static final int CONTENT_PAD_Y = 10;

    // ── Row spacing ──────────────────────────────────────────────────────
    public static final int ROW_STEP      = 26;
    public static final int ROW_STEP_SM   = 20;
    public static final int ROW_H         = 16;

    // ── Button ───────────────────────────────────────────────────────────
    public static final int BTN_H_SM      = 14;
    public static final int BTN_H         = 18;
    public static final int BTN_H_LG      = 22;
    public static final int BTN_W_CLOSE   = 90;
    public static final int BTN_W_HALF    = 145;

    // ── Section ──────────────────────────────────────────────────────────
    public static final int SECTION_H     = 14;

    // ── Footer ───────────────────────────────────────────────────────────
    public static final int FOOTER_PAD    = 28;

    /**
     * Tính Y bắt đầu vùng content (sau header + tab nếu có).
     */
    public static int contentY(int screenY, boolean hasTabs) {
        int y = screenY + HEADER_H;
        if (hasTabs) y += TAB_H + 4;
        return y + CONTENT_PAD_Y;
    }

    /**
     * Tính Y của footer buttons (cách đáy window FOOTER_PAD).
     */
    public static int footerY(int screenY, int screenH) {
        return screenY + screenH - FOOTER_PAD;
    }

    /**
     * Căn giữa ngang một widget có chiều rộng w trong panel có chiều rộng panelW.
     */
    public static int centerX(int panelX, int panelW, int w) {
        return panelX + (panelW - w) / 2;
    }

    private LayoutUtil() {}
}
