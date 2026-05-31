package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SideBadgeHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int x, int y, HudAnchor anchor, boolean isMining, boolean holding, String modeLabel, String keyHint) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        int textW = client.font.width(modeLabel);
        int drawX = x;
        boolean isRight = anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.MIDDLE_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT;
        if (isRight) {
            drawX -= textW + 8;
        } else if (anchor == HudAnchor.TOP_CENTER) {
            drawX -= textW / 2;
        }

        int color = holding ? ThemeColors.EMERALD_BORDER : ThemeColors.TEXT_DIM;
        int textColor = holding ? ThemeColors.HUD_ON_TEXT : ThemeColors.TEXT_VALUE;
        
        if (isRight) {
            ctx.fill(drawX + textW + 4, y - 2, drawX + textW + 6, y + 10, color);
            ctx.drawString(client.font, modeLabel, drawX, y, textColor, true);
        } else {
            ctx.fill(drawX, y - 2, drawX + 2, y + 10, color);
            ctx.drawString(client.font, modeLabel, drawX + 6, y, textColor, true);
        }
    }
}
