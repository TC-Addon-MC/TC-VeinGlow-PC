package com.tcveinminer.client.util;

public final class LayoutUtil {

    public static final int SCREEN_W_SM  = 280;
    public static final int SCREEN_W_MD  = 860;
    public static final int SCREEN_W_LG  = 960;
    public static final int SCREEN_H_MD  = 540;

    public static final int HEADER_H     = 40;
    public static final int HEADER_PAD_X = 16;
    public static final int HEADER_PAD_Y = 14;

    public static final int TAB_BAR_H    = 40;
    public static final int TAB_H        = 40;
    public static final int TAB_PAD_X    = 10;

    public static final int CONTENT_PAD_X = 56;
    public static final int CONTENT_PAD_Y = 24;

    public static final int ROW_STEP     = 28;
    public static final int ROW_STEP_SM  = 22;
    public static final int ROW_H        = 20;

    public static final int BTN_H_SM     = 14;
    public static final int BTN_H        = 24;
    public static final int BTN_H_LG     = 28;
    public static final int BTN_W_CLOSE  = 90;
    public static final int BTN_W_HALF   = 145;
    public static final int BTN_H_SAVE   = 32;

    public static final int FOOTER_H     = 60;
    public static final int FOOTER_PAD   = 36;

    public static int contentY(int screenY, boolean hasTabs) {
        int y = screenY + HEADER_H;
        if (hasTabs) y += TAB_BAR_H;
        return y + CONTENT_PAD_Y;
    }

    public static int footerY(int screenY, int screenH) {
        return screenY + screenH - FOOTER_PAD;
    }

    public static int centerX(int panelX, int panelW, int w) {
        return panelX + (panelW - w) / 2;
    }

    private LayoutUtil() {}
}
