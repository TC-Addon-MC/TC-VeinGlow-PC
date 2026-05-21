package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

public final class SliderDrawUtil {

    public static void draw(DrawContext ctx, int x, int y, int w, int h, float percent) {
        ctx.fill(x, y, x + w, y + h, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, ThemeColors.BORDER_DEFAULT);
        int fillW = (int)(percent * (w - 4));
        if (fillW > 0)
            ctx.fillGradient(x + 2, y + 2, x + 2 + fillW, y + h - 2, ThemeColors.GOLD_DIM, ThemeColors.GOLD);
        int tx = Math.max(x + 2, Math.min(x + 2 + fillW - 4, x + w - 10));
        ctx.fill(tx, y - 2, tx + 8, y + h + 2, ThemeColors.GOLD);
        DrawHelper.drawSolidBorder(ctx, tx, y - 2, 8, h + 4, ThemeColors.GOLD_DIM);
    }

    private SliderDrawUtil() {}
}
