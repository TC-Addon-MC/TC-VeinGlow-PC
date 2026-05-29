package com.tcveinminer.client.gui.widgets;

import com.tcveinminer.client.gui.CustomButton;
import com.tcveinminer.client.util.ButtonDrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class AmberButton extends CustomButton {
    public AmberButton(int x, int y, int w, int h, Text msg, PressAction p) {
        super(x, y, w, h, msg, p);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mx, int my, float d) {
        boolean hov = isHovered();
        ButtonDrawUtil.drawAmber(ctx, getX(), getY(), getWidth(), getHeight(), hov, false);

        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        int tw = mc.textRenderer.getWidth(getMessage());
        // Chữ trắng sáng (nền giờ là tối amber, không phải vàng đặc)
        int textColor = hov ? 0xFFFFFFFF : 0xFFFFE8A0;
        ctx.drawTextWithShadow(mc.textRenderer, getMessage(),
                getX() + (getWidth() - tw) / 2,
                getY() + (getHeight() - 8) / 2,
                textColor);
    }
}
