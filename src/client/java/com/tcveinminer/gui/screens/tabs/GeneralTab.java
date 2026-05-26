package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class GeneralTab implements MenuTab {
    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int itemW = (cw - 8) / 2;
        for (int i = 0; i < 4; i++) {
            final int m = i + 1;
            int rx = cx + (i % 2) * (itemW + 8);
            int ry = cy + 18 + (i / 2) * 24;
            screen.addUIElement(new CustomButton(rx, ry, itemW, 20, Text.empty(), btn -> {
                screen.getState().activationMode = m;
                screen.rebuildMenu();
            }));
        }
        // dY phải khớp chính xác với render(): card cao 66 (cy..cy+66), toggle rows bắt đầu tại cy+70
        int dY = cy + 70;
        screen.addUIElement(new CustomButton(cx, dY, cw, 20, Text.empty(), btn -> {
            screen.getState().showHud = !screen.getState().showHud;
            screen.rebuildMenu();
        }));
        screen.addUIElement(new CustomButton(cx, dY + 24, cw, 20, Text.empty(), btn -> {
            screen.getState().showOutline = !screen.getState().showOutline;
            screen.rebuildMenu();
        }));
        screen.addUIElement(new CustomButton(cx, dY + 48, cw, 20, Text.empty(), btn -> {
            screen.getState().requireCorrectTool = !screen.getState().requireCorrectTool;
            screen.rebuildMenu();
        }));
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        DrawHelper.drawCard(ctx, cx, cy, cw, 64);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "CHẾ ĐỘ KÍCH HOẠT", cx + 8, cy + 5, ThemeColors.TEXT_LABEL);
        String[] modes = {"Đè nút", "Đè + sneak", "Nhấn bật", "Nhấn + sneak"};
        int itemW = (cw - 8) / 2;
        for (int i = 0; i < 4; i++) {
            int rx = cx + (i % 2) * (itemW + 8);
            int ry = cy + 18 + (i / 2) * 24;
            boolean sel = (screen.getState().activationMode == i + 1);
            ctx.fill(rx, ry, rx + itemW, ry + 20, sel ? ThemeColors.GOLD_FILL : ThemeColors.BG_INPUT);
            DrawHelper.drawSolidBorder(ctx, rx, ry, itemW, 20, sel ? ThemeColors.GOLD : ThemeColors.BORDER_DEFAULT);
            ctx.drawTextWithShadow(screen.getTextRenderer(), modes[i], rx + 6, ry + 6, sel ? ThemeColors.GOLD : ThemeColors.TEXT_LABEL);
        }

        int dY = cy + 70;
        drawCheckRow(ctx, screen, cx, dY, cw, "HUD nổi", screen.getState().showHud);
        drawCheckRow(ctx, screen, cx, dY + 24, cw, "Outline", screen.getState().showOutline);
        drawCheckRow(ctx, screen, cx, dY + 48, cw, "Yêu cầu đúng dụng cụ", screen.getState().requireCorrectTool);
    }

    private void drawCheckRow(DrawContext ctx, MainMenuScreen screen, int rx, int ry, int rw, String label, boolean checked) {
        int rowBg = checked ? 0x33D8A15B : ThemeColors.BG_ROW;
        int rowBorder = checked ? ThemeColors.GOLD : ThemeColors.BORDER_DEFAULT;
        ctx.fill(rx, ry, rx + rw, ry + 20, rowBg);
        DrawHelper.drawSolidBorder(ctx, rx, ry, rw, 20, rowBorder);
        int labelColor = checked ? ThemeColors.GOLD : ThemeColors.TEXT_LABEL;
        ctx.drawTextWithShadow(screen.getTextRenderer(), label, rx + 8, ry + 6, labelColor);
        int bx = rx + rw - 18, by = ry + 3;
        ctx.fill(bx, by, bx + 14, by + 14, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, bx, by, 14, 14, checked ? ThemeColors.GOLD : ThemeColors.BORDER_DIM);
        if (checked) {
            ctx.fill(bx + 2, by + 2, bx + 12, by + 12, ThemeColors.GOLD);
            ctx.fill(bx + 4, by + 5, bx + 6, by + 9, 0xFFFFFFFF);
            ctx.fill(bx + 6, by + 7, bx + 10, by + 9, 0xFFFFFFFF);
        }
    }
}