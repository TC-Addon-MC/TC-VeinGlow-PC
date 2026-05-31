package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import com.tcveinminer.logic.HudNotifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PillHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int x, int y, HudAnchor anchor, boolean isMining, boolean holding, String modeLabel, String keyHint) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        String statusText;
        if (isMining) {
            statusText = Component.translatable("hud.tcveinminer.mining",
                    String.valueOf(HudNotifier.lastMined),
                    String.valueOf(HudNotifier.lastMax),
                    modeLabel).getString();
        } else if (holding) {
            statusText = Component.translatable("hud.tcveinminer.ready", keyHint, modeLabel).getString();
        } else {
            statusText = Component.translatable("hud.tcveinminer.waiting", keyHint, modeLabel).getString();
        }

        int textW = client.font.width(statusText);
        int pillW = textW + 16;
        
        int drawX = x;
        if (anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.MIDDLE_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT) {
            drawX -= pillW;
        } else if (anchor == HudAnchor.TOP_CENTER) {
            drawX -= pillW / 2;
        }

        DrawHelper.drawHudPill(ctx, drawX, y, pillW, 14, holding);
        int textColor = holding ? ThemeColors.HUD_ON_TEXT : ThemeColors.HUD_OFF_TEXT;
        ctx.drawString(client.font, statusText, drawX + 8, y + 3, textColor);
    }
}
