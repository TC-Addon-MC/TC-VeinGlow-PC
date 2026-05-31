package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ProgressBarHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        if (!holding && !isMining) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() - 22; 

        int w = 60;
        int h = 4;
        int x1 = cx + 100;
        int y1 = cy;

        ctx.fill(x1, y1, x1 + w, y1 + h, 0xFF112244); // Dark blue background
        
        if (isMining) {
            float time = (System.currentTimeMillis() % 1000L) / 1000f; // pulse or fill
            int bw = (int) (w * time);
            ctx.fill(x1, y1, x1 + bw, y1 + h, 0xFFFFAA00); // Gold
            ctx.fill(x1 + bw - 2, y1, x1 + bw, y1 + h, 0xFF00AAFF); // Blue tip
        }

        ctx.drawString(mc.font, modeName, x1 + w/2 - mc.font.width(modeName)/2, y1 - 10, isMining ? 0xFFFFAA00 : 0xFF00AAFF, true);
    }
}
