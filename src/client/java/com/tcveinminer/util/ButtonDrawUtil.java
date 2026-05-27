package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

public final class ButtonDrawUtil {

    // ── Primitive shapes ────────────────────────────────────────────────────

    /** Nền màu trơn bo góc 1px */
    private static void fillRounded(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y,         x + w - 1, y + h,     color); // thân chính
        ctx.fill(x,     y + 1,     x + 1,     y + h - 1, color); // cạnh trái
        ctx.fill(x + w - 1, y + 1, x + w,     y + h - 1, color); // cạnh phải
    }

    /** Nền gradient bo góc 1px */
    private static void fillRoundedGradient(DrawContext ctx, int x, int y, int w, int h, int cTop, int cBot) {
        ctx.fillGradient(x + 1,     y,     x + w - 1, y + h,     cTop, cBot);
        ctx.fillGradient(x,         y + 1, x + 1,     y + h - 1, cTop, cBot);
        ctx.fillGradient(x + w - 1, y + 1, x + w,     y + h - 1, cTop, cBot);
    }

    /** Viền ngoài bo góc 1px */
    private static void drawRoundedBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x + 1,     y,         x + w - 1, y + 1,     c); // trên
        ctx.fill(x + 1,     y + h - 1, x + w - 1, y + h,     c); // dưới
        ctx.fill(x,         y + 1,     x + 1,     y + h - 1, c); // trái
        ctx.fill(x + w - 1, y + 1,     x + w,     y + h - 1, c); // phải
    }

    /**
     * Highlight sáng ở hàng pixel thứ 2 từ top — tạo cảm giác nổi 3D pixel-art.
     * Vẽ bên trong viền (x+2 … x+w-2) để không đè lên góc bo.
     */
    private static void drawTopHighlight(DrawContext ctx, int x, int y, int w, int color) {
        ctx.fill(x + 2, y + 1, x + w - 2, y + 2, color);
    }

    // ── Lerp ────────────────────────────────────────────────────────────────

    public static int lerpColor(int c1, int c2, float t) {
        if (t <= 0) return c1;
        if (t >= 1) return c2;
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        return ((int)(a1 + (a2 - a1) * t) << 24)
             | ((int)(r1 + (r2 - r1) * t) << 16)
             | ((int)(g1 + (g2 - g1) * t) << 8)
             |  (int)(b1 + (b2 - b1) * t);
    }

    // ── Button styles ────────────────────────────────────────────────────────

    /**
     * PRIMARY — tab / toggle thông thường.
     * Nền xanh navy tối sâu, viền xanh sáng để nổi bật trên nền menu,
     * viền vàng khi selected với glow hiệu ứng.
     */
    public static void drawPrimary(DrawContext ctx, int x, int y, int w, int h,
                                   boolean hovered, float selectProgress) {
        int bgTop  = hovered ? 0xFF1E2D45 : 0xFF16243A;
        int bgBot  = hovered ? 0xFF0E1926 : 0xFF08111E;
        int border = hovered ? 0xFF5B7FA6 : 0xFF3A5370;

        int selBgTop  = 0xFF1A2B42;
        int selBgBot  = 0xFF081018;
        int selBorder = ThemeColors.BTN_PRIMARY_SELECTED_BORDER; // vàng

        int fBgTop  = lerpColor(bgTop,  selBgTop,  selectProgress);
        int fBgBot  = lerpColor(bgBot,  selBgBot,  selectProgress);
        int fBorder = lerpColor(border, selBorder, selectProgress);

        if (selectProgress > 0f) {
            int glow1 = lerpColor(0x00D8A15B, 0x44D8A15B, selectProgress);
            int glow2 = lerpColor(0x00D8A15B, 0x18D8A15B, selectProgress);
            drawRoundedBorder(ctx, x - 1, y - 1, w + 2, h + 2, glow1);
            drawRoundedBorder(ctx, x - 2, y - 2, w + 4, h + 4, glow2);
        }

        fillRoundedGradient(ctx, x, y, w, h, fBgTop, fBgBot);
        drawRoundedBorder(ctx, x, y, w, h, fBorder);
        drawTopHighlight(ctx, x, y, w, hovered ? 0x28FFFFFF : 0x18FFFFFF);
    }

    /**
     * AMBER — action buttons chính: Add, Save, Confirm...
     * Nền amber tối sâu (không vàng đặc), viền vàng sáng nổi bật.
     * Chữ phải là TRẮNG để đọc rõ trên nền tối này.
     */
    public static void drawAmber(DrawContext ctx, int x, int y, int w, int h,
                                  boolean hovered, boolean pressed) {
        int bgTop, bgBot, border;
        if (pressed) {
            bgTop  = 0xFF3D2A00;
            bgBot  = 0xFF1E1400;
            border = 0xFFAA7808;
        } else if (hovered) {
            bgTop  = 0xFF6B4A00;
            bgBot  = 0xFF3C2800;
            border = 0xFFFFCE50;
        } else {
            bgTop  = 0xFF523C00;
            bgBot  = 0xFF2C1E00;
            border = 0xFFCCA828;
        }

        fillRoundedGradient(ctx, x, y, w, h, bgTop, bgBot);
        drawRoundedBorder(ctx, x, y, w, h, border);
        drawTopHighlight(ctx, x, y, w, hovered ? 0x40FFD060 : 0x28FFBE00);
    }

    /**
     * PURPLE — custom / magic actions.
     * Nền tím tối, viền tím sáng.
     */
    public static void drawPurple(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        int bgTop  = hovered ? 0xFF4C2FA0 : 0xFF3B2284;
        int bgBot  = hovered ? 0xFF2A1A70 : 0xFF1C1158;
        int border = hovered ? 0xFFCEAAFF : 0xFF9E7AF2;

        fillRoundedGradient(ctx, x, y, w, h, bgTop, bgBot);
        drawRoundedBorder(ctx, x, y, w, h, border);
        drawTopHighlight(ctx, x, y, w, hovered ? 0x32D8BAFF : 0x22B898FF);
    }

    /**
     * DANGER — xóa, reset nguy hiểm.
     * Nền đỏ tối sâu, viền đỏ rõ.
     */
    public static void drawDanger(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        int bgTop  = hovered ? 0xFF5E1212 : 0xFF440E0E;
        int bgBot  = hovered ? 0xFF340808 : 0xFF220404;
        int border = hovered ? 0xFFFF6868 : 0xFFCC3636;

        fillRoundedGradient(ctx, x, y, w, h, bgTop, bgBot);
        drawRoundedBorder(ctx, x, y, w, h, border);
        drawTopHighlight(ctx, x, y, w, 0x28FF5050);
    }

    /**
     * LIST BUTTON — dùng trong danh sách cuộn, trạng thái selected/hover.
     */
    public static void drawListButton(DrawContext ctx, int x, int y, int w, int h,
                                       boolean hovered, boolean selected) {
        int bg     = (hovered || selected) ? ThemeColors.BTN_LIST_HOVER_BG : ThemeColors.BTN_LIST_BG;
        int border = selected ? ThemeColors.BTN_PRIMARY_SELECTED_BORDER
                   : hovered  ? ThemeColors.BTN_LIST_HOVER_BORDER
                              : ThemeColors.BTN_LIST_BORDER;

        fillRounded(ctx, x, y, w, h, bg);
        drawRoundedBorder(ctx, x, y, w, h, border);
    }

    private ButtonDrawUtil() {}
}