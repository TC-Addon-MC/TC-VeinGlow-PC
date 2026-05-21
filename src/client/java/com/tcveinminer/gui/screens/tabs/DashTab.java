package com.tcveinminer.gui.screens.tabs;

import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.screens.MainMenuScreen;
import com.tcveinminer.gui.widgets.MaxBlockSlider;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;

public class DashTab implements MenuTab {
    @Override
    public void init(MainMenuScreen screen, int cx, int cy, int cw, int ch) {
        int sliderY = cy + (ch / 2) + 20;
        screen.addUIElement(new MaxBlockSlider(cx + 8, sliderY, cw - 16, 12, screen.getState()));
    }

    @Override
    public void render(DrawContext ctx, MainMenuScreen screen, int cx, int cy, int cw, int ch, int mouseX, int mouseY, float delta) {
        int cardH = ch / 2 - 4;

        DrawHelper.drawCard(ctx, cx, cy, cw, cardH);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "CHẾ ĐỘ HIỆN TẠI", cx + 8, cy + 6, ThemeColors.TEXT_LABEL);
        DrawHelper.drawCard(ctx, cx + 4, cy + 18, cw - 8, cardH - 24);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "ĐÀO", cx + 10, cy + 22, ThemeColors.TEXT_LABEL);
        String label = screen.getState().hoveredShapeId;
        try { label = ModConfig.MiningShape.valueOf(screen.getState().hoveredShapeId).label; } catch (Exception ignored) {}
        ctx.drawTextWithShadow(screen.getTextRenderer(), label, cx + 10, cy + 34, ThemeColors.GOLD);

        int card2Y = cy + cardH + 6;
        DrawHelper.drawCard(ctx, cx, card2Y, cw, cardH);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "GIỚI HẠN KHỐI", cx + 8, card2Y + 6, ThemeColors.TEXT_LABEL);
        String lim = screen.getState().maxBlocks + "/128";
        ctx.drawTextWithShadow(screen.getTextRenderer(), lim, cx + cw - 8 - screen.getTextRenderer().getWidth(lim), card2Y + 6, ThemeColors.GOLD);

        ctx.fill(cx + 8, card2Y + 20, cx + cw - 8, card2Y + 32, ThemeColors.BG_INPUT);
        DrawHelper.drawSolidBorder(ctx, cx + 8, card2Y + 20, cw - 16, 12, ThemeColors.BORDER_DEFAULT);
        ctx.drawTextWithShadow(screen.getTextRenderer(), "Giới hạn khối tối đa", cx + 8, card2Y + cardH - 14, ThemeColors.TEXT_HINT);
    }
}