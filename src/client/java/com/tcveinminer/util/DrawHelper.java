package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Helper methods để vẽ UI components theo design system.
 */
public final class DrawHelper {

    /**
     * Vẽ viền gradient 2px cho window.
     * Gradient: start → mid → end (horizontal interpolation).
     */
    public static void drawGradientBorder(DrawContext ctx, int x, int y, int w, int h,
                                          int colorStart, int colorMid, int colorEnd) {
        int halfW = w / 2;
        // Top border
        ctx.fillGradient(x, y, x + halfW, y + 2, colorStart, colorMid);
        ctx.fillGradient(x + halfW, y, x + w, y + 2, colorMid, colorEnd);
        // Bottom border
        ctx.fillGradient(x, y + h - 2, x + halfW, y + h, colorStart, colorMid);
        ctx.fillGradient(x + halfW, y + h - 2, x + w, y + h, colorMid, colorEnd);
        // Left border
        ctx.fill(x, y, x + 2, y + h, colorStart);
        // Right border
        ctx.fill(x + w - 2, y, x + w, y + h, colorEnd);
    }

    /**
     * Vẽ panel nền + viền gradient đầy đủ.
     */
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, ThemeColors.BG_MAIN);
        ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, ThemeColors.BORDER_INNER);
        ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, ThemeColors.BG_MAIN);
        drawGradientBorder(ctx, x, y, w, h,
            ThemeColors.BORDER_START, ThemeColors.BORDER_MID, ThemeColors.BORDER_END);
    }

    /**
     * Vẽ header panel với divider bên dưới.
     */
    public static void drawHeader(DrawContext ctx, int x, int y, int w, int headerH) {
        ctx.fill(x + 2, y + 2, x + w - 2, y + headerH, ThemeColors.BG_HEADER);
        // Divider gradient dưới header
        int halfW = w / 2;
        ctx.fillGradient(x + 2, y + headerH, x + halfW, y + headerH + 1,
            ThemeColors.DIVIDER_START, ThemeColors.BORDER_MID);
        ctx.fillGradient(x + halfW, y + headerH, x + w - 2, y + headerH + 1,
            ThemeColors.BORDER_MID, ThemeColors.DIVIDER_END);
    }

    /**
     * Vẽ nút thường (primary).
     */
    public static void drawButton(DrawContext ctx, int x, int y, int w, int h,
                                  boolean hovered, boolean pressed) {
        int bg     = pressed ? ThemeColors.BTN_BG_PRESS : (hovered ? ThemeColors.BTN_BG_HOVER : ThemeColors.BTN_BG);
        int border = hovered ? ThemeColors.BTN_BORDER_HOVER : ThemeColors.BTN_BORDER;
        ctx.fill(x, y, x + w, y + h, bg);
        drawSolidBorder(ctx, x, y, w, h, border);
    }

    /**
     * Vẽ nút toggle ON/OFF.
     */
    public static void drawToggleButton(DrawContext ctx, int x, int y, int w, int h, boolean on) {
        if (on) {
            ctx.fillGradient(x, y, x + w, y + h,
                ThemeColors.TOGGLE_ON_BG_A, ThemeColors.TOGGLE_ON_BG_B);
            drawSolidBorder(ctx, x, y, w, h, ThemeColors.TOGGLE_ON_BORDER);
            // Dot indicator
            ctx.fill(x + 5, y + h / 2 - 3, x + 11, y + h / 2 + 3, ThemeColors.TOGGLE_ON_DOT);
        } else {
            ctx.fill(x, y, x + w, y + h, ThemeColors.TOGGLE_OFF_BG);
            drawSolidBorder(ctx, x, y, w, h, ThemeColors.TOGGLE_OFF_BORDER);
            ctx.fill(x + 5, y + h / 2 - 3, x + 11, y + h / 2 + 3, ThemeColors.TOGGLE_OFF_DOT);
        }
    }

    /**
     * Vẽ nút danger (xóa, reset).
     */
    public static void drawDangerButton(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        int bg = hovered ? 0xFF3A0A0A : ThemeColors.BTN_DANGER_BG;
        ctx.fill(x, y, x + w, y + h, bg);
        drawSolidBorder(ctx, x, y, w, h, ThemeColors.BTN_DANGER_BORDER);
    }

    /**
     * Vẽ slider track + fill gradient + thumb tròn.
     */
    public static void drawSlider(DrawContext ctx, int x, int y, int w, int h, float percent) {
        // Track
        ctx.fill(x, y, x + w, y + h, ThemeColors.SLIDER_TRACK);
        drawSolidBorder(ctx, x, y, w, h, ThemeColors.SLIDER_BORDER);
        // Fill gradient
        int fillW = (int) (percent * (w - 4));
        if (fillW > 0) {
            ctx.fillGradient(x + 2, y + 2, x + 2 + fillW, y + h - 2,
                ThemeColors.SLIDER_FILL_A, ThemeColors.SLIDER_FILL_B);
        }
        // Thumb
        int thumbX = x + 2 + fillW - 4;
        thumbX = Math.max(x + 2, Math.min(thumbX, x + w - 10));
        ctx.fill(thumbX, y - 2, thumbX + 8, y + h + 2, ThemeColors.SLIDER_THUMB);
        drawSolidBorder(ctx, thumbX, y - 2, 8, h + 4, ThemeColors.SLIDER_THUMB_B);
    }

    /**
     * Vẽ viền 1px đồng màu.
     */
    public static void drawSolidBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);           // top
        ctx.fill(x, y + h - 1, x + w, y + h, color);  // bottom
        ctx.fill(x, y, x + 1, y + h, color);           // left
        ctx.fill(x + w - 1, y, x + w, y + h, color);  // right
    }

    /**
     * Vẽ nền HUD dạng pill.
     */
    public static void drawHudPill(DrawContext ctx, int x, int y, int w, int h, boolean on) {
        int bg     = on ? ThemeColors.HUD_ON_BG     : ThemeColors.HUD_OFF_BG;
        int border = on ? ThemeColors.HUD_ON_BORDER : ThemeColors.HUD_OFF_BORDER;
        ctx.fill(x, y, x + w, y + h, bg);
        drawSolidBorder(ctx, x, y, w, h, border);
    }

    private DrawHelper() {}
}
