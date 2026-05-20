package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Vẽ các loại button: primary, danger, active.
 * Tách riêng khỏi DrawHelper để dễ mở rộng kiểu nút mới.
 */
public final class ButtonDrawUtil {

    /**
     * Nút chính (primary) — gold border, hover sáng hơn.
     */
    public static void drawPrimary(DrawContext ctx, int x, int y, int w, int h,
                                   boolean hovered, boolean pressed) {
        int bg = pressed ? ThemeColors.BG_HEADER
               : hovered ? ThemeColors.BG_ROW_HOVER
               :           ThemeColors.BG_PANEL;
        int border = hovered ? ThemeColors.GOLD_DIM : ThemeColors.STONE_BORDER;
        ctx.fill(x, y, x + w, y + h, bg);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, border);
    }

    /**
     * Nút nguy hiểm — redstone.
     */
    public static void drawDanger(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        int bg = hovered ? 0xFF2A0808 : ThemeColors.REDSTONE_FILL;
        ctx.fill(x, y, x + w, y + h, bg);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, ThemeColors.REDSTONE_DIM);
    }

    /**
     * Nút active / selected — gold fill.
     */
    public static void drawActive(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, ThemeColors.GOLD_FILL);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, ThemeColors.GOLD_DIM);
    }

    private ButtonDrawUtil() {}
}
