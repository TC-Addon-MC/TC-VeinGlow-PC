package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Vẽ các thành phần nền/border/divider cơ bản.
 * Chỉ chứa primitive draw calls — KHÔNG chứa logic layout.
 *
 * Dùng cùng với:
 *   ButtonDrawUtil   → buttons
 *   ToggleDrawUtil   → toggles
 *   SliderDrawUtil   → sliders
 *   PanelDrawUtil    → panels / cards
 */
public final class DrawHelper {

    /**
     * Viền gradient 2px quanh window — gold → copper.
     */
    public static void drawGradientBorder(DrawContext ctx, int x, int y, int w, int h,
                                          int colorA, int colorMid, int colorB) {
        int half = w / 2;
        ctx.fillGradient(x,        y,          x + half, y + 2,      colorA,   colorMid);
        ctx.fillGradient(x + half, y,          x + w,    y + 2,      colorMid, colorB);
        ctx.fillGradient(x,        y + h - 2,  x + half, y + h,      colorA,   colorMid);
        ctx.fillGradient(x + half, y + h - 2,  x + w,    y + h,      colorMid, colorB);
        ctx.fill(x,          y, x + 2,     y + h, colorA);
        ctx.fill(x + w - 2,  y, x + w,     y + h, colorB);
    }

    /**
     * Toàn bộ panel: nền + viền gradient gold/copper.
     */
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, ThemeColors.BG_WINDOW);
        ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, ThemeColors.BG_PANEL);
        drawGradientBorder(ctx, x, y, w, h,
            ThemeColors.GOLD_DIM, ThemeColors.GOLD, ThemeColors.COPPER);
    }

    /**
     * Header trong panel + divider bên dưới.
     */
    public static void drawHeader(DrawContext ctx, int x, int y, int w, int headerH) {
        ctx.fill(x + 2, y + 2, x + w - 2, y + headerH, ThemeColors.BG_HEADER);
        int half = w / 2;
        ctx.fillGradient(x + 2,    y + headerH, x + half, y + headerH + 1,
            ThemeColors.GOLD_GRAD_A, ThemeColors.GOLD_GRAD_B);
        ctx.fillGradient(x + half, y + headerH, x + w - 2, y + headerH + 1,
            ThemeColors.GOLD_GRAD_B, ThemeColors.COPPER_DIM);
    }

    /**
     * Panel nhỏ / inset (dùng cho section bên trong màn hình).
     */
    public static void drawInsetPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, ThemeColors.BG_PANEL_INSET);
        drawSolidBorder(ctx, x, y, w, h, ThemeColors.GOLD_BORDER);
    }

    /**
     * Divider ngang đơn giản.
     */
    public static void drawDivider(DrawContext ctx, int x, int y, int w) {
        int half = w / 2;
        ctx.fillGradient(x,        y, x + half, y + 1, ThemeColors.GOLD_BORDER, ThemeColors.GOLD_DIM);
        ctx.fillGradient(x + half, y, x + w,    y + 1, ThemeColors.GOLD_DIM,   0x00000000);
    }

    /**
     * Viền 1px đồng màu.
     */
    public static void drawSolidBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,          y,          x + w, y + 1,     color);
        ctx.fill(x,          y + h - 1,  x + w, y + h,     color);
        ctx.fill(x,          y,          x + 1, y + h,     color);
        ctx.fill(x + w - 1,  y,          x + w, y + h,     color);
    }

    /**
     * Nền pill HUD (dùng trong VeinMinerHudOverlay).
     */
    public static void drawHudPill(DrawContext ctx, int x, int y, int w, int h, boolean on) {
        int bg     = on ? ThemeColors.EMERALD_FILL  : ThemeColors.REDSTONE_FILL;
        int border = on ? ThemeColors.EMERALD       : ThemeColors.REDSTONE_DIM;
        ctx.fill(x, y, x + w, y + h, bg);
        drawSolidBorder(ctx, x, y, w, h, border);
    }

    private DrawHelper() {}
}
