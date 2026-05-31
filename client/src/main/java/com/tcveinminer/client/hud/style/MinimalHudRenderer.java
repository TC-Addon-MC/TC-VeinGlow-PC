package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import com.tcveinminer.logic.HudNotifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class MinimalHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int x, int y, HudAnchor anchor, boolean isMining, boolean holding, String modeLabel, String keyHint) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        String text = modeLabel;
        if (isMining) {
            text += " (" + HudNotifier.lastMined + "/" + HudNotifier.lastMax + ")";
        } else if (!holding) {
            text += " [" + keyHint + "]";
        }

        int textW = client.font.width(text);
        int drawX = x;
        if (anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.MIDDLE_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT) {
            drawX -= textW;
        } else if (anchor == HudAnchor.TOP_CENTER) {
            drawX -= textW / 2;
        }

        int textColor = holding ? ThemeColors.HUD_ON_TEXT : ThemeColors.TEXT_VALUE;
        ctx.drawString(client.font, text, drawX, y, textColor, true);
    }
}
