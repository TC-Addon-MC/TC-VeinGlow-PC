package com.tcveinminer.client.util;

import net.minecraft.client.gui.GuiGraphics;

public final class ToggleDrawUtil {

    public static void draw(GuiGraphics ctx, int x, int y, int w, int h, boolean on) {
        if (on) {
            ctx.fill(x, y, x + w, y + h, ThemeColors.EMERALD_FILL);
            DrawHelper.drawSolidBorder(ctx, x, y, w, h, ThemeColors.EMERALD_BORDER);
            ctx.fill(x + w - 9, y + h / 2 - 3, x + w - 3, y + h / 2 + 3, ThemeColors.EMERALD);
        } else {
            ctx.fill(x, y, x + w, y + h, ThemeColors.BG_INPUT);
            DrawHelper.drawSolidBorder(ctx, x, y, w, h, ThemeColors.BORDER_DEFAULT);
            ctx.fill(x + 3, y + h / 2 - 3, x + 9, y + h / 2 + 3, ThemeColors.BORDER_DIM);
        }
    }

    private ToggleDrawUtil() {}
}
