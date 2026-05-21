package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.gui.widgets.RGBSlider;
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
        int sw = 18;
        for (int i = 0; i < 8; i++) {
            final int ri = PRESET_RGB[i][0], gi = PRESET_RGB[i][1], bi = PRESET_RGB[i][2];
            int bx = cx + (i % 4) * (sw + 6), by = cy + 12 + (i / 4) * 22;
            screen.addUIElement(new CustomButton(bx, by, sw, sw, Text.empty(), btn -> {
                screen.getState().colorR = ri; screen.getState().colorG = gi; screen.getState().colorB = bi;
                screen.getState().colorRainbow = false; screen.getState().colorDisabled = false;
                screen.rebuildMenu();
            }));
        }
        int optX = cx + 4 * (sw + 6) + 10;
        screen.addUIElement(new CustomButton(optX, cy + 12, 55, 16, Text.empty(), btn -> { screen.getState().colorRainbow = true; screen.getState().colorDisabled = false; screen.rebuildMenu(); }));
        screen.addUIElement(new CustomButton(optX, cy + 34, 55, 16, Text.empty(), btn -> { screen.getState().colorDisabled = true; screen.getState().colorRainbow = false; screen.rebuildMenu(); }));

        boolean dis = screen.getState().colorRainbow || screen.getState().colorDisabled;
        int slidersY = cy + 64;
        screen.addUIElement(new RGBSlider(cx + 40, slidersY, cw - 45, 12, 'R', screen.getState(), screen.getState().colorR / 255.0, dis));
        screen.addUIElement(new RGBSlider(cx + 40, slidersY + 18, cw - 45, 12, 'G', screen.getState(), screen.getState().colorG / 255.0, dis));
        screen.addUIElement(new RGBSlider(cx + 40, slidersY + 36, cw - 45, 12, 'B', screen.getState(), screen.getState().colorB / 255.0, dis));
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        int sw = 18;
        ctx.drawTextWithShadow(screen.getTextRenderer(), "MÀU SẮC (CHỌN NHANH)", cx, cy, ThemeColors.TEXT_LABEL);
        for (int i = 0; i < 8; i++) {
            int col = i % 4, row = i / 4;
            int bx = cx + col * (sw + 6), by = cy + 12 + row * 22;
            int sc = 0xFF000000 | (PRESET_RGB[i][0] << 16) | (PRESET_RGB[i][1] << 8) | PRESET_RGB[i][2];
            ctx.fill(bx, by, bx + sw, by + sw, ThemeColors.BG_INPUT);
            DrawHelper.drawSolidBorder(ctx, bx, by, sw, sw, ThemeColors.BORDER_DEFAULT);
            ctx.fill(bx + 2, by + 2, bx + sw - 2, by + sw - 2, sc);
        }
        int optX = cx + 4 * (sw + 6) + 10;
        ctx.drawTextWithShadow(screen.getTextRenderer(), "Cầu vồng", optX + 4, cy + 16, ThemeColors.TEXT_LABEL);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "Tắt màu", optX + 4, cy + 38, ThemeColors.TEXT_LABEL);

        int slidersY = cy + 64;
        ctx.drawTextWithShadow(screen.getTextRenderer(), "RGB CHỈNH TAY", cx, slidersY - 10, ThemeColors.TEXT_DIM);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "R: " + screen.getState().colorR, cx, slidersY + 2, ThemeColors.REDSTONE_TEXT);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "G: " + screen.getState().colorG, cx, slidersY + 20, ThemeColors.EMERALD_TEXT);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "B: " + screen.getState().colorB, cx, slidersY + 38, 0xFF93C5FD);

        int pc = screen.getState().colorDisabled ? ThemeColors.BG_INPUT : screen.getState().colorRainbow ? 0xFFEE82EE : 0xFF000000 | (screen.getState().colorR << 16) | (screen.getState().colorG << 8) | screen.getState().colorB;
        int bpX = cx + cw - 32, bpY = cy + 12;
        ctx.fill(bpX, bpY, bpX + 32, bpY + 32, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, bpX, bpY, 32, 32, pc);
        if (!screen.getState().colorDisabled && !screen.getState().colorRainbow)
            ctx.fill(bpX + 2, bpY + 2, bpX + 30, bpY + 30, (pc & 0x00FFFFFF) | 0xFF000000);
    }
}