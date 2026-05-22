package com.tcveinminer.util;

import net.minecraft.client.gui.DrawContext;

/**
 * Primitive draw calls — panel, border, divider, card.
 * Đã được cập nhật sang phong cách hiện đại (Modern Dark Theme).
 */
public final class DrawHelper {

    // Màu sắc hiện đại bám sát thiết kế mới
    public static final int BG_APP = 0xFF0B0F19; // Nền tổng thể tối (xanh đen)
    public static final int BG_CARD = 0xFF111827; // Nền của các hộp nhỏ bên trong (sáng hơn nền chính 1 chút)
    public static final int BORDER_MODERN = 0xFF1F2937; // Viền xám tối phẳng

    public static void drawHudPill(DrawContext ctx, int x, int y, int w, int h, boolean active) {
        int bg = active ? 0xAA0F172A : 0xAA020408;
        int border = active ? 0xFFF59E0B : BORDER_MODERN;
        drawHudPillRounded(ctx, x, y, w, h, bg, border);
    }

    // Bo tròn góc 2px kiểu pixel: cắt 4 góc, vẽ viền bo theo
    private static void drawHudPillRounded(DrawContext ctx, int x, int y, int w, int h, int bg, int border) {
        // Nền
        ctx.fill(x + 2, y,         x + w - 2, y + 1,         bg);
        ctx.fill(x + 1, y + 1,     x + w - 1, y + h - 1,     bg);
        ctx.fill(x + 2, y + h - 1, x + w - 2, y + h,         bg);
        // Viền
        ctx.fill(x + 2,     y,         x + w - 2, y + 1,         border); // trên
        ctx.fill(x + 2,     y + h - 1, x + w - 2, y + h,         border); // dưới
        ctx.fill(x + 1,     y + 1,     x + 2,     y + h - 1,     border); // trái
        ctx.fill(x + w - 2, y + 1,     x + w - 1, y + h - 1,     border); // phải
        ctx.fill(x + 1,     y + 1,     x + 2,     y + 2,         border); // góc trên-trái
        ctx.fill(x + w - 2, y + 1,     x + w - 1, y + 2,         border); // góc trên-phải
        ctx.fill(x + 1,     y + h - 2, x + 2,     y + h - 1,     border); // góc dưới-trái
        ctx.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1,     border); // góc dưới-phải
    }

    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        // Vẽ nền chính trơn
        ctx.fill(x, y, x + w, y + h, BG_APP);
        // Vẽ viền ngoài cùng phẳng thay vì gradient
        drawSolidBorder(ctx, x, y, w, h, BORDER_MODERN);
    }

    // Giữ lại tên hàm này để các file cũ không báo lỗi,
    // nhưng hiển thị dưới dạng viền phẳng hiện đại.
    public static void drawGradientBorder(DrawContext ctx, int x, int y, int w, int h) {
        drawSolidBorder(ctx, x, y, w, h, BORDER_MODERN);
    }

    public static void drawHeader(DrawContext ctx, int x, int y, int w, int hh) {
        // Nền mờ nhẹ cho Header
        ctx.fill(x, y, x + w, y + hh, 0x1AFFFFFF);
        // Đường phân cách mỏng dưới header
        ctx.fill(x, y + hh - 1, x + w, y + hh, BORDER_MODERN);
    }

    public static void drawCard(DrawContext ctx, int x, int y, int w, int h) {
        // Hộp chứa nội dung với viền bao quanh
        ctx.fill(x, y, x + w, y + h, BG_CARD);
        drawSolidBorder(ctx, x, y, w, h, BORDER_MODERN);
    }

    public static void drawSolidBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,         y,         x + w,     y + 1,     c);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     c);
        ctx.fill(x,         y + 1,     x + 1,     y + h - 1, c);
        ctx.fill(x + w - 1, y + 1,     x + w,     y + h - 1, c);
    }

    // Giữ lại hàm divider để không lỗi code, nhưng vẽ kiểu hiện đại (xám mờ dần ở 2 đầu)
    public static void drawDivider(DrawContext ctx, int x, int y, int w) {
        int half = x + w / 2;
        ctx.fillGradient(x,    y, half,  y + 1, 0x001F2937, BORDER_MODERN);
        ctx.fillGradient(half, y, x + w, y + 1, BORDER_MODERN,    0x001F2937);
    }

    // Legacy alias used by old code
    public static void drawInsetPanel(DrawContext ctx, int x, int y, int w, int h) {
        drawCard(ctx, x, y, w, h);
    }

    private DrawHelper() {}
}