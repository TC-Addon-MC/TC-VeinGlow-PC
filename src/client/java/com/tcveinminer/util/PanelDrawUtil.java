package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

public final class PanelDrawUtil {

    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        DrawHelper.drawPanel(ctx, x, y, w, h);
    }

    public static void card(DrawContext ctx, int x, int y, int w, int h) {
        DrawHelper.drawCard(ctx, x, y, w, h);
    }

    /** type: 0=gold, 1=emerald, 2=redstone, 3=stone, 4=purple */
    public static void tag(DrawContext ctx, int x, int y, int w, int h, int type) {
        int bg, border;
        switch (type) {
            case 1  -> { bg = ThemeColors.EMERALD_FILL;   border = ThemeColors.EMERALD_BORDER;   }
            case 2  -> { bg = ThemeColors.REDSTONE_FILL;  border = ThemeColors.REDSTONE_BORDER;  }
            case 4  -> { bg = ThemeColors.PURPLE_FILL;    border = ThemeColors.PURPLE_BORDER_DIM; }
            default -> { bg = ThemeColors.GOLD_FILL;      border = ThemeColors.GOLD_BORDER;       }
        }
        ctx.fill(x, y, x + w, y + h, bg);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, border);
    }

    private PanelDrawUtil() {}
}
