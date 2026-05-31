package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ArcHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int x, int y, HudAnchor anchor, boolean isMining, boolean holding, String modeLabel, String keyHint) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        
        // Arc is always centered around the crosshair, ignoring the configured anchor
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() / 2;
        
        int textW = client.font.width(modeLabel);
        int textColor = holding ? ThemeColors.HUD_ON_TEXT : ThemeColors.HUD_OFF_TEXT;
        ctx.drawString(client.font, modeLabel, cx - textW / 2, cy + 12, textColor, true);
        
        int color = holding ? ThemeColors.EMERALD_BORDER : ThemeColors.TEXT_DIM;
        // Left bracket
        ctx.fill(cx - 10, cy - 8, cx - 8, cy + 8, color);
        ctx.fill(cx - 8, cy - 8, cx - 4, cy - 6, color);
        ctx.fill(cx - 8, cy + 6, cx - 4, cy + 8, color);
        // Right bracket
        ctx.fill(cx + 8, cy - 8, cx + 10, cy + 8, color);
        ctx.fill(cx + 4, cy - 8, cx + 8, cy - 6, color);
        ctx.fill(cx + 4, cy + 6, cx + 8, cy + 8, color);
    }
}
