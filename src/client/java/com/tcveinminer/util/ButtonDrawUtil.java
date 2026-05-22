package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

public final class ButtonDrawUtil {

    // Hàm hỗ trợ vẽ nền màu trơn bo góc 1px
    private static void fillRounded(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
    }

    // Hàm hỗ trợ vẽ nền gradient bo góc 1px
    private static void fillRoundedGradient(DrawContext ctx, int x, int y, int w, int h, int colorTop, int colorBottom) {
        // Vẽ phần giữa rộng
        ctx.fillGradient(x + 1, y, x + w - 1, y + h, colorTop, colorBottom);
        // Vẽ 2 viền trái phải hẹp hơn 1 pixel ở mép trên và dưới
        ctx.fillGradient(x, y + 1, x + 1, y + h - 1, colorTop, colorBottom);
        ctx.fillGradient(x + w - 1, y + 1, x + w, y + h - 1, colorTop, colorBottom);
    }

    // Hàm hỗ trợ vẽ viền ngoài bo góc 1px
    private static void drawRoundedBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + 1, color); // Cạnh trên
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, color); // Cạnh dưới
        ctx.fill(x, y + 1, x + 1, y + h - 1, color); // Cạnh trái
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color); // Cạnh phải
    }

    public static void drawPrimary(DrawContext ctx, int x, int y, int w, int h, boolean hovered, boolean selected) {
        if (selected) {
            // Selected: nền vàng mờ + viền vàng sáng rõ ràng
            fillRounded(ctx, x, y, w, h, 0x44D8A15B);
            drawRoundedBorder(ctx, x, y, w, h, ThemeColors.BTN_PRIMARY_SELECTED_BORDER);
        } else if (hovered) {
            // Hover: nền sáng nhẹ + viền vàng mờ
            fillRoundedGradient(ctx, x, y, w, h, ThemeColors.BTN_PRIMARY_HOVER_BG, ThemeColors.APP_BG);
            drawRoundedBorder(ctx, x, y, w, h, ThemeColors.GOLD_BORDER);
        } else {
            // Normal
            fillRoundedGradient(ctx, x, y, w, h, ThemeColors.BTN_PRIMARY_BG, ThemeColors.APP_BG);
            drawRoundedBorder(ctx, x, y, w, h, ThemeColors.BTN_PRIMARY_BORDER);
        }
    }

    public static void drawAmber(DrawContext ctx, int x, int y, int w, int h, boolean hovered, boolean pressed) {
        int gradTop = hovered ? ThemeColors.BTN_AMBER_HOVER_GRAD_TOP : ThemeColors.BTN_AMBER_GRAD_TOP;
        int gradBottom = hovered ? ThemeColors.BTN_AMBER_HOVER_GRAD_BOTTOM : ThemeColors.BTN_AMBER_GRAD_BOTTOM;

        if (pressed) {
            // Trạng thái nhấn: Đảo ngược gradient và làm viền tối đi một chút
            fillRoundedGradient(ctx, x, y, w, h, gradBottom, gradTop);
            drawRoundedBorder(ctx, x, y, w, h, ThemeColors.BORDER_DEFAULT); // Dùng viền tối hơn
        } else {
            // Trạng thái bình thường hoặc hover
            fillRoundedGradient(ctx, x, y, w, h, gradTop, gradBottom);
            drawRoundedBorder(ctx, x, y, w, h, ThemeColors.BTN_AMBER_BORDER);
        }
    }

    public static void drawPurple(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        int gradTop = hovered ? ThemeColors.BTN_PURPLE_HOVER_GRAD_TOP : ThemeColors.BTN_PURPLE_GRAD_TOP;
        int gradBottom = hovered ? ThemeColors.BTN_PURPLE_HOVER_GRAD_BOTTOM : ThemeColors.BTN_PURPLE_GRAD_BOTTOM;

        fillRoundedGradient(ctx, x, y, w, h, gradTop, gradBottom);
        drawRoundedBorder(ctx, x, y, w, h, ThemeColors.BTN_PURPLE_BORDER);
    }

    public static void drawDanger(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        int bg = hovered ? 0xFF2A0808 : ThemeColors.REDSTONE_FILL;

        fillRounded(ctx, x, y, w, h, bg);
        drawRoundedBorder(ctx, x, y, w, h, ThemeColors.REDSTONE_BORDER);
    }

    public static void drawListButton(DrawContext ctx, int x, int y, int w, int h, boolean hovered, boolean selected) {
        int bg = hovered || selected ? ThemeColors.BTN_LIST_HOVER_BG : ThemeColors.BTN_LIST_BG;
        int border = selected ? ThemeColors.BTN_PRIMARY_SELECTED_BORDER : hovered ? ThemeColors.BTN_LIST_HOVER_BORDER : ThemeColors.BTN_LIST_BORDER;

        fillRounded(ctx, x, y, w, h, bg);
        drawRoundedBorder(ctx, x, y, w, h, border);
    }

    private ButtonDrawUtil() {}
}