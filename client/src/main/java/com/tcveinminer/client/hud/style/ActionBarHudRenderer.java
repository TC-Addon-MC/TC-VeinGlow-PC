package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ActionBarHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        if (!holding && !isMining) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() - 60; // Vanilla Action bar area

        String text = isMining ? net.minecraft.network.chat.Component.translatable("hud.tcveinminer.style.mining", modeName).getString() : net.minecraft.network.chat.Component.translatable("hud.tcveinminer.style.mode", modeName).getString();
        int color = isMining ? ThemeColors.GOLD : ThemeColors.HUD_TEXT;
        
        ctx.drawCenteredString(mc.font, text, cx, cy, color);
    }
}
