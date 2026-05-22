package com.tcveinminer.gui.widgets;

import com.tcveinminer.gui.CustomButton;
import com.tcveinminer.util.ButtonDrawUtil;
import com.tcveinminer.util.ThemeColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class AmberButton extends CustomButton {
    public AmberButton(int x, int y, int w, int h, Text msg, PressAction p) {
        super(x, y, w, h, msg, p);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mx, int my, float d) {
        ButtonDrawUtil.drawAmber(ctx, getX(), getY(), getWidth(), getHeight(), isHovered(), false);
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        int tw = mc.textRenderer.getWidth(getMessage());
        ctx.drawTextWithShadow(mc.textRenderer, getMessage(), getX() + (getWidth() - tw) / 2, getY() + (getHeight() - 8) / 2, ThemeColors.BG_SCREEN);
    }
}