package com.tcveinminer.client.gui.screens.tabs;

import com.tcveinminer.client.gui.CustomButton;
import com.tcveinminer.client.gui.screens.MainMenuScreen;
import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class GeneralTab implements MenuTab {
    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        String[] modes = {
                Text.translatable("gui.tcveinminer.activation.hold_short").getString(),
                Text.translatable("gui.tcveinminer.activation.hold_sneak_short").getString(),
                Text.translatable("gui.tcveinminer.activation.toggle_short").getString(),
                Text.translatable("gui.tcveinminer.activation.toggle_sneak_short").getString()
        };

        int itemW = (cw - 8) / 2;
        for (int i = 0; i < 4; i++) {
            final int m = i + 1;
            int rx = cx + (i % 2) * (itemW + 8);
            int ry = cy + 18 + (i / 2) * 24;
            CustomButton btn = new CustomButton(rx, ry, itemW, 20, Text.literal(modes[i]), b -> {
                screen.getState().activationMode = m;
                screen.rebuildMenu();
            });
            btn.setSelectedInstant(screen.getState().activationMode == m);
            screen.addUIElement(btn);
        }
        // dY phải khớp chính xác với render(): card cao 66 (cy..cy+66), toggle rows bắt đầu tại cy+70
        int dY = cy + 70;
        CustomButton btnHud = new CustomButton(cx, dY, cw, 20, Text.empty(), btn -> {
            screen.getState().showHud = !screen.getState().showHud;
            screen.rebuildMenu();
        });
        btnHud.setSelectedInstant(screen.getState().showHud);
        screen.addUIElement(btnHud);

        CustomButton btnOutline = new CustomButton(cx, dY + 24, cw, 20, Text.empty(), btn -> {
            screen.getState().showOutline = !screen.getState().showOutline;
            screen.rebuildMenu();
        });
        btnOutline.setSelectedInstant(screen.getState().showOutline);
        screen.addUIElement(btnOutline);

        // Removed require_tool button
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        DrawHelper.drawCard(ctx, cx, cy, cw, 64);
        ctx.drawTextWithShadow(screen.getTextRenderer(), Text.translatable("gui.tcveinminer.general.activation_mode").getString(), cx + 8, cy + 5, ThemeColors.TEXT_LABEL);

        int dY = cy + 70;
        drawCheckRow(ctx, screen, cx, dY, cw, Text.translatable("gui.tcveinminer.general.floating_hud").getString(), screen.getState().showHud);
        drawCheckRow(ctx, screen, cx, dY + 24, cw, Text.translatable("gui.tcveinminer.general.outline").getString(), screen.getState().showOutline);
        // Removed require_tool rendering
    }

    private void drawCheckRow(DrawContext ctx, MainMenuScreen screen, int rx, int ry, int rw, String label, boolean checked) {
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
