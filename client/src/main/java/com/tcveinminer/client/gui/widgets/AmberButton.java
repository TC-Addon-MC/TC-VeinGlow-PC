package com.tcveinminer.client.gui.widgets;

import com.tcveinminer.client.gui.CustomButton;
import com.tcveinminer.client.util.ButtonDrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class AmberButton extends CustomButton {
    public AmberButton(int x, int y, int w, int h, Component msg, OnPress p) {
        super(x, y, w, h, msg, p);
    }

    @Override
    protected void renderWidget(GuiGraphics ctx, int mx, int my, float d) {
        boolean hov = isHovered();
        ButtonDrawUtil.drawAmber(ctx, getX(), getY(), getWidth(), getHeight(), hov, false);

        Minecraft mc = Minecraft.getInstance();
        int tw = mc.font.width(getMessage());
        int textColor = hov ? 0xFFFFFFFF : 0xFFFFE8A0;
        ctx.drawString(mc.font, getMessage(),
                getX() + (getWidth() - tw) / 2,
                getY() + (getHeight() - 8) / 2,
                textColor);
    }
}