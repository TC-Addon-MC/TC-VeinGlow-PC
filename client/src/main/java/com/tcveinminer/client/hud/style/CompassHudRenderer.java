package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class CompassHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        if (!holding && !isMining) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = ctx.guiWidth() / 2;
        int cy = 10;

        int w = 120;
        int h = 14;

        ctx.fill(cx - w/2, cy, cx + w/2, cy + h, ThemeColors.HUD_BG);
        ctx.fill(cx - w/2, cy, cx + w/2, cy + 1, ThemeColors.BORDER_DIM);
        ctx.fill(cx - w/2, cy + h - 1, cx + w/2, cy + h, ThemeColors.BORDER_DIM);

        int color = isMining ? ThemeColors.GOLD : ThemeColors.HUD_TEXT;
        ctx.drawCenteredString(mc.font, modeName, cx, cy + 3, color);
    }
}
