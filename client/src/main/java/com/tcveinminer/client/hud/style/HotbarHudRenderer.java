package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class HotbarHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        if (!holding && !isMining) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() - 22; // Just above hotbar

        int lx = cx + 95; // To the right of the hotbar
        
        String text = isMining ? modeName + " Active" : modeName;
        int color = isMining ? ThemeColors.GOLD : ThemeColors.HUD_TEXT;
        
        ctx.drawString(mc.font, text, lx, cy, color, true);
    }
}
