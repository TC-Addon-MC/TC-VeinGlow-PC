package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Vẽ các loại panel / card / tag.
 *   panel()      → panel chính (dùng trong Screen.render)
 *   card()       → card nhỏ nội dung
 *   sectionBar() → thanh tiêu đề section
 *   tag()        → badge nhỏ trạng thái
 */
public final class PanelDrawUtil {

    /**
     * Panel chính toàn màn hình với viền gradient.
     */
    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        DrawHelper.drawPanel(ctx, x, y, w, h);
    }

    /**
     * Card nội dung nhỏ bên trong màn hình.
     */
    public static void card(DrawContext ctx, int x, int y, int w, int h) {
        DrawHelper.drawInsetPanel(ctx, x, y, w, h);
    }

    /**
     * Thanh tiêu đề section — nền header + underline gold.
     */
    public static void sectionBar(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, ThemeColors.BG_HEADER);
        DrawHelper.drawDivider(ctx, x, y + h - 1, w);
    }

    /**
     * Tag / badge inline.
     *   type: 0 = gold (default), 1 = emerald, 2 = redstone, 3 = stone
     */
    public static void tag(DrawContext ctx, int x, int y, int w, int h, int type) {
        int bg, border;
        switch (type) {
            case 1  -> { bg = ThemeColors.EMERALD_FILL;  border = ThemeColors.EMERALD_BORDER; }
            case 2  -> { bg = ThemeColors.REDSTONE_FILL; border = ThemeColors.REDSTONE_BORDER;}
            case 3  -> { bg = ThemeColors.BG_PANEL_INSET;border = ThemeColors.STONE_BORDER;  }
            default -> { bg = ThemeColors.GOLD_FILL;     border = ThemeColors.GOLD_BORDER;   }
        }
        ctx.fill(x, y, x + w, y + h, bg);
        DrawHelper.drawSolidBorder(ctx, x, y, w, h, border);
    }

    private PanelDrawUtil() {}
}
