package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.gui.widgets.RGBSlider;
import com.tcveinminer.gui.widgets.ThicknessSlider;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ColorTab implements MenuTab {
    private static final int[][] PRESET_RGB = {
            {16, 185, 129}, {216, 161, 91}, {59, 130, 246}, {239, 68, 68},
            {168, 85, 247}, {249, 115, 22}, {203, 213, 225}, {6, 182, 212}
    };

    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int sw = 18; // Thu nhỏ ô màu
        int gap = 4;
        int startY = cy + 20;

        // 1. Vẽ 8 ô màu chọn nhanh (4x2 grid)
        for (int i = 0; i < 8; i++) {
            final int ri = PRESET_RGB[i][0], gi = PRESET_RGB[i][1], bi = PRESET_RGB[i][2];
            int bx = cx + (i % 4) * (sw + gap);
            int by = startY + (i / 4) * (sw + gap);

            screen.addUIElement(new CustomButton(bx, by, sw, sw, Text.empty(), btn -> {
                screen.getState().colorR = ri;
                screen.getState().colorG = gi;
                screen.getState().colorB = bi;
                screen.getState().colorRainbow = false;
                screen.getState().colorDisabled = false;
                screen.rebuildMenu();
            }) {
                @Override
                public void renderWidget(DrawContext ctx, int mx, int my, float delta) {
                    int sc = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
                    ctx.fill(getX(), getY(), getX() + width, getY() + height, DrawHelper.BG_CARD);
                    boolean isSelected = (!screen.getState().colorRainbow && !screen.getState().colorDisabled
                            && screen.getState().colorR == ri && screen.getState().colorG == gi && screen.getState().colorB == bi);
                    int border = isSelected ? ThemeColors.GOLD : (isHovered() ? 0xFFFFFFFF : DrawHelper.BORDER_MODERN);
                    DrawHelper.drawSolidBorder(ctx, getX(), getY(), width, height, border);
                    ctx.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, sc);
                }
            });
        }

        // 2. Nút chức năng (Cầu vồng / Tắt màu) - Thu nhỏ chiều rộng
        int optX = cx + (4 * (sw + gap)) + 10;
        int optW = 60;
        screen.addUIElement(new CustomButton(optX, startY, optW, 18, Text.translatable("gui.tcveinminer.color.rainbow"), btn -> {
            screen.getState().colorRainbow = true;
            screen.getState().colorDisabled = false;
            screen.rebuildMenu();
        }));
        screen.addUIElement(new CustomButton(optX, startY + 22, optW, 18, Text.translatable("gui.tcveinminer.color.disable"), btn -> {
            screen.getState().colorDisabled = true;
            screen.getState().colorRainbow = false;
            screen.rebuildMenu();
        }));

        // 3. Khởi tạo thanh trượt RGB - Chỉnh lại tọa độ Y để không bị tràn
        boolean dis = screen.getState().colorRainbow || screen.getState().colorDisabled;
        int slidersY = cy + 85;
        int sliderH = 14;
        screen.addUIElement(new RGBSlider(cx + 40, slidersY, cw - 55, sliderH, 'R', screen.getState(), screen.getState().colorR / 255.0, dis));
        screen.addUIElement(new RGBSlider(cx + 40, slidersY + 20, cw - 55, sliderH, 'G', screen.getState(), screen.getState().colorG / 255.0, dis));
        screen.addUIElement(new RGBSlider(cx + 40, slidersY + 40, cw - 55, sliderH, 'B', screen.getState(), screen.getState().colorB / 255.0, dis));
        screen.addUIElement(new ThicknessSlider(cx + 40, slidersY + 60, cw - 55, sliderH, screen.getState(), screen.getState().outlineThickness));
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("gui.tcveinminer.color.preset_title").getString(), cx, cy + 6, 0xFFA0AEC0);

        int slidersY = cy + 85;
        ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("gui.tcveinminer.color.rgb_title").getString(), cx, slidersY - 12, 0xFFA0AEC0);

        ctx.drawTextWithShadow(screen.getTextRenderer(), "R:", cx, slidersY + 3, ThemeColors.REDSTONE_TEXT);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "G:", cx, slidersY + 23, ThemeColors.EMERALD_TEXT);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "B:", cx, slidersY + 43, 0xFF93C5FD);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "W:", cx, slidersY + 63, 0xFFFFFFFF);

        String valR = String.valueOf(screen.getState().colorR);
        String valG = String.valueOf(screen.getState().colorG);
        String valB = String.valueOf(screen.getState().colorB);
        String valW = String.format("%.1f", screen.getState().outlineThickness);
        ctx.drawTextWithShadow(screen.getTextRenderer(), valR, cx + 25 - screen.getTextRenderer().getWidth(valR), slidersY + 3, 0xFFFFFFFF);
        ctx.drawTextWithShadow(screen.getTextRenderer(), valG, cx + 25 - screen.getTextRenderer().getWidth(valG), slidersY + 23, 0xFFFFFFFF);
        ctx.drawTextWithShadow(screen.getTextRenderer(), valB, cx + 25 - screen.getTextRenderer().getWidth(valB), slidersY + 43, 0xFFFFFFFF);
        ctx.drawTextWithShadow(screen.getTextRenderer(), valW, cx + 25 - screen.getTextRenderer().getWidth(valW), slidersY + 63, 0xFFFFFFFF);

        // ==========================================
        // KHUNG XEM TRƯỚC (PREVIEW) - ĐÃ THU NHỎ
        // ==========================================
        int bpSize = 44; // Thu nhỏ từ 56
        int bpX = cx + cw - bpSize - 8;
        int bpY = cy + 18;

        ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("gui.tcveinminer.color.preview_title").getString(), bpX - 4, bpY - 12, 0xFFA0AEC0);
        DrawHelper.drawCard(ctx, bpX, bpY, bpSize, bpSize);

        if (screen.getState().colorDisabled) {
            int tw = screen.getTextRenderer().getWidth("OFF");
            ctx.drawTextWithShadow(screen.getTextRenderer(), "OFF", bpX + (bpSize - tw)/2, bpY + (bpSize - 8)/2, 0xFF6B7280);
        }
        else if (screen.getState().colorRainbow) {
            long time = System.currentTimeMillis();
            int r = (int) ((Math.sin(time / 500.0) + 1) * 127.5);
            int g = (int) ((Math.sin(time / 500.0 + 2.0) + 1) * 127.5);
            int b = (int) ((Math.sin(time / 500.0 + 4.0) + 1) * 127.5);
            ctx.fill(bpX + 2, bpY + 2, bpX + bpSize - 2, bpY + bpSize - 2, 0xFF000000 | (r << 16) | (g << 8) | b);
        }
        else {
            int pc = 0xFF000000 | (screen.getState().colorR << 16) | (screen.getState().colorG << 8) | screen.getState().colorB;
            ctx.fill(bpX + 2, bpY + 2, bpX + bpSize - 2, bpY + bpSize - 2, pc);
        }
    }
}