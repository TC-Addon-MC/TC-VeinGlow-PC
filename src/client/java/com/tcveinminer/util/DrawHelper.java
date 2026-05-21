package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Primitive draw calls — panel, border, divider, card.
 */
public final class DrawHelper {
    public static void drawHudPill(DrawContext ctx, int x, int y, int w, int h, boolean active) {
        // Tạo màu nền hơi trong suốt (thêm alpha 0xAA vào mã màu)
        int bg = active ? 0xAA0F172A : 0xAA020408;
        int border = active ? ThemeColors.GOLD_DIM : ThemeColors.BORDER_DIM;

        ctx.fill(x, y, x + w, y + h, bg);
        drawSolidBorder(ctx, x, y, w, h, border);
    }
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, ThemeColors.BG_WINDOW);
        drawGradientBorder(ctx, x, y, w, h);
    }

    public static void drawGradientBorder(DrawContext ctx, int x, int y, int w, int h) {
        int half = x + w / 2;
        ctx.fillGradient(x, y,        half,  y + 2,    ThemeColors.GOLD_DIM, ThemeColors.GOLD);
        ctx.fillGradient(half, y,      x + w, y + 2,    ThemeColors.GOLD,     ThemeColors.COPPER_DIM);
        ctx.fillGradient(x, y + h - 2, half,  y + h,   ThemeColors.GOLD_DIM, ThemeColors.GOLD);
        ctx.fillGradient(half, y + h - 2, x + w, y + h, ThemeColors.GOLD,   ThemeColors.COPPER_DIM);
        ctx.fill(x,         y + 2, x + 2,     y + h - 2, ThemeColors.GOLD_DIM);
        ctx.fill(x + w - 2, y + 2, x + w,     y + h - 2, ThemeColors.COPPER_DIM);
    }

    public static void drawHeader(DrawContext ctx, int x, int y, int w, int hh) {
        ctx.fill(x + 2, y + 2, x + w - 2, y + hh, ThemeColors.BG_HEADER);
        int half = x + w / 2;
        ctx.fillGradient(x + 2, y + hh, half,    y + hh + 1, ThemeColors.GOLD,     ThemeColors.GOLD_DIM);
        ctx.fillGradient(half,  y + hh, x + w - 2, y + hh + 1, ThemeColors.GOLD_DIM, ThemeColors.COPPER_DIM);
    }

    public static void drawCard(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, ThemeColors.BG_PANEL);
        drawSolidBorder(ctx, x, y, w, h, ThemeColors.BORDER_DEFAULT);
    }

    public static void drawSolidBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,         y,         x + w,     y + 1,     c);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     c);
        ctx.fill(x,         y + 1,     x + 1,     y + h - 1, c);
        ctx.fill(x + w - 1, y + 1,     x + w,     y + h - 1, c);
    }

    public static void drawDivider(DrawContext ctx, int x, int y, int w) {
        int half = x + w / 2;
        ctx.fillGradient(x,    y, half,  y + 1, ThemeColors.GOLD_BORDER, ThemeColors.GOLD_DIM);
        ctx.fillGradient(half, y, x + w, y + 1, ThemeColors.GOLD_DIM,    0x00000000);
    }

    // Legacy alias used by old code
    public static void drawInsetPanel(DrawContext ctx, int x, int y, int w, int h) {
        drawCard(ctx, x, y, w, h);
    }

    
    private DrawHelper() {}
}
