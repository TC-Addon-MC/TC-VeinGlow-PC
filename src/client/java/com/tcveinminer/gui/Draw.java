package com.tcveinminer.gui;

import net.minecraft.client.gui.DrawContext;

public final class Draw {

    /** Panel: nền đen + viền gradient 2px. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x+2, y+2, x+w-2, y+h-2, TC.BG);
        border(ctx, x, y, w, h);
    }

    /** Viền gradient 2px: tím → xanh dương → xanh lá. */
    public static void border(DrawContext ctx, int x, int y, int w, int h) {
        int m = x + w/2;
        ctx.fillGradient(x, y,     m,   y+2,   TC.B_START, TC.B_MID);
        ctx.fillGradient(m, y,   x+w,   y+2,   TC.B_MID,   TC.B_END);
        ctx.fillGradient(x, y+h-2, m,   y+h,   TC.B_START, TC.B_MID);
        ctx.fillGradient(m, y+h-2,x+w,  y+h,   TC.B_MID,   TC.B_END);
        ctx.fill(x,     y+2, x+2,   y+h-2, TC.B_START);
        ctx.fill(x+w-2, y+2, x+w,   y+h-2, TC.B_END);
    }

    /** Header nền + divider gradient bên dưới. */
    public static void header(DrawContext ctx, int x, int y, int w, int hh) {
        ctx.fill(x+2, y+2, x+w-2, y+hh, TC.BG_HEADER);
        int m = x + w/2;
        ctx.fillGradient(x+2, y+hh, m,     y+hh+1, TC.B_START, TC.B_MID);
        ctx.fillGradient(m,   y+hh, x+w-2, y+hh+1, TC.B_MID,   TC.B_END);
    }

    /** Viền 1px đồng màu. */
    public static void rect(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,     y,     x+w,   y+1,   c);
        ctx.fill(x,     y+h-1, x+w,   y+h,   c);
        ctx.fill(x,     y+1,   x+1,   y+h-1, c);
        ctx.fill(x+w-1, y+1,   x+w,   y+h-1, c);
    }

    /** Nút thường. */
    public static void btn(DrawContext ctx, int x, int y, int w, int h, boolean hov) {
        ctx.fill(x, y, x+w, y+h, hov ? TC.BTN_BG_HOV : TC.BTN_BG);
        rect(ctx, x, y, w, h, hov ? TC.BTN_BD_HOV : TC.BTN_BORDER);
    }

    /** Card toggle với dot indicator. */
    public static void toggle(DrawContext ctx, int x, int y, int w, int h, boolean on) {
        if (on) {
            ctx.fillGradient(x, y, x+w, y+h, TC.ON_BG_A, TC.ON_BG_B);
            rect(ctx, x, y, w, h, TC.ON_BORDER);
            ctx.fill(x+5, y+h/2-3, x+11, y+h/2+3, TC.ON_BORDER);
        } else {
            ctx.fill(x, y, x+w, y+h, TC.OFF_BG);
            rect(ctx, x, y, w, h, TC.OFF_BORDER);
            ctx.fill(x+5, y+h/2-3, x+11, y+h/2+3, TC.OFF_BORDER);
        }
    }

    /** HUD pill. */
    public static void pill(DrawContext ctx, int x, int y, int w, int h, boolean on) {
        ctx.fill(x, y, x+w, y+h, on ? TC.HUD_ON_BG : TC.HUD_OFF_BG);
        rect(ctx, x, y, w, h, on ? TC.HUD_ON_BD : TC.HUD_OFF_BD);
    }

    /** Danger button đỏ. */
    public static void danger(DrawContext ctx, int x, int y, int w, int h, boolean hov) {
        ctx.fill(x, y, x+w, y+h, hov ? 0xFF3A0A0A : 0xFF2A0A0A);
        rect(ctx, x, y, w, h, 0xFF8B2737);
    }

    private Draw() {}
}
