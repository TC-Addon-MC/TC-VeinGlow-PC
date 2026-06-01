package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class CrosshairTagHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        if (!holding && !isMining) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() / 2;

        String text = isMining ? "[" + net.minecraft.network.chat.Component.translatable("hud.tcveinminer.style.active").getString() + "]" : modeName;
        int color = isMining ? ThemeColors.GOLD : ThemeColors.HUD_TEXT_DIM;
        
        ctx.drawString(mc.font, text, cx + 8, cy + 6, color, true);
    }
}
