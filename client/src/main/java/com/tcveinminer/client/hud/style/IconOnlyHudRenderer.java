package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class IconOnlyHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int x, int y, HudAnchor anchor, boolean isMining, boolean holding, String modeLabel, String keyHint) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        int color = holding ? ThemeColors.EMERALD_BORDER : ThemeColors.TEXT_DIM;
        int drawX = x;
        if (anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.MIDDLE_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT) {
            drawX -= 12;
        } else if (anchor == HudAnchor.TOP_CENTER) {
            drawX -= 6;
        }
        
        ctx.fill(drawX, y, drawX + 12, y + 12, color);
        // Inner square
        int inner = holding ? ThemeColors.APP_BG : ThemeColors.BG_PANEL_INSET;
        ctx.fill(drawX + 2, y + 2, drawX + 10, y + 10, inner);
        
        // A little line to indicate something inside
        ctx.fill(drawX + 4, y + 5, drawX + 8, y + 7, color);
    }
}
