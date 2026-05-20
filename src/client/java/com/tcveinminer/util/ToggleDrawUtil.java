package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Vẽ toggle switch ON/OFF kiểu mining core.
 *   ON  → emerald border + dot sáng
 *   OFF → redstone dim border + dot tắt
 */
public final class ToggleDrawUtil {

    public static void draw(DrawContext ctx, int x, int y, int w, int h, boolean on) {
        if (on) {
            ctx.fill(x, y, x + w, y + h, ThemeColors.EMERALD_FILL);
            DrawHelper.drawSolidBorder(ctx, x, y, w, h, ThemeColors.EMERALD);
            // dot bên phải
            ctx.fill(x + w - 9, y + h / 2 - 3, x + w - 3, y + h / 2 + 3, ThemeColors.EMERALD);
        } else {
            ctx.fill(x, y, x + w, y + h, ThemeColors.REDSTONE_FILL);
            DrawHelper.drawSolidBorder(ctx, x, y, w, h, ThemeColors.REDSTONE_DIM);
            // dot bên trái
            ctx.fill(x + 3, y + h / 2 - 3, x + 9, y + h / 2 + 3, ThemeColors.REDSTONE_DIM);
        }
    }

    private ToggleDrawUtil() {}
}
