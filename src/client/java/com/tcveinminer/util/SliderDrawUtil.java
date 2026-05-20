package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Vẽ slider track + fill gradient + thumb — phong cách mining energy bar.
 */
public final class SliderDrawUtil {

    /**
     * @param percent  0.0f → 1.0f
     */
    public static void draw(DrawContext ctx, int x, int y, int w, int h, float percent) {
        // Track
        ctx.fill(x, y, x + w, y + h, ThemeColors.BG_PANEL_INSET);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, ThemeColors.STONE);

        // Fill gradient gold
        int fillW = (int) (percent * (w - 4));
        if (fillW > 0) {
            ctx.fillGradient(x + 2, y + 2, x + 2 + fillW, y + h - 2,
                ThemeColors.GOLD_DIM, ThemeColors.GOLD);
        }

        // Thumb
        int thumbX = x + 2 + fillW - 4;
        thumbX = Math.max(x + 2, Math.min(thumbX, x + w - 10));
        ctx.fill(thumbX, y - 2, thumbX + 8, y + h + 2, ThemeColors.GOLD);
        DrawHelper.drawSolidBorder(ctx, thumbX, y - 2, 8, h + 4, ThemeColors.GOLD_DIM);
    }

    private SliderDrawUtil() {}
}
