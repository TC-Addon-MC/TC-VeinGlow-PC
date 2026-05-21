package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

public final class ButtonDrawUtil {

    public static void drawPrimary(DrawContext ctx, int x, int y, int w, int h,
                                   boolean hovered, boolean pressed) {
        int bg     = pressed ? ThemeColors.BTN_BG_PRESS : hovered ? ThemeColors.BTN_BG_HOVER : ThemeColors.BTN_BG;
        int border = hovered ? ThemeColors.BTN_BORDER_HOVER : ThemeColors.BTN_BORDER;
        ctx.fill(x, y, x + w, y + h, bg);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, border);
    }

    public static void drawAmber(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        ctx.fillGradient(x, y, x + w, y + h,
            hovered ? 0xFFF59E0B : 0xFFD97706,
                         0xFFB45309);
    }

    public static void drawPurple(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        ctx.fill(x, y, x + w, y + h, hovered ? ThemeColors.PURPLE_FILL : 0x1A7C3AED);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, hovered ? ThemeColors.PURPLE : ThemeColors.PURPLE_BORDER_DIM);
    }

    public static void drawDanger(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        ctx.fill(x, y, x + w, y + h, hovered ? 0xFF2A0808 : ThemeColors.REDSTONE_FILL);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, ThemeColors.REDSTONE_BORDER);
    }

    private ButtonDrawUtil() {}
}
