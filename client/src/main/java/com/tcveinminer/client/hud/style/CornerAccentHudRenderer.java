package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class CornerAccentHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        if (!holding && !isMining) return;
        Minecraft mc = Minecraft.getInstance();
        
        int color = isMining ? ThemeColors.GOLD : ThemeColors.BORDER_DEFAULT;
        
        ctx.fill(customX, customY, customX + 4, customY + 16, color);
        ctx.fill(customX, customY, customX + 16, customY + 4, color);

        ctx.drawString(mc.font, modeName, customX + 6, customY + 6, isMining ? ThemeColors.GOLD : ThemeColors.HUD_TEXT, true);
    }
}
