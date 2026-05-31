package com.tcveinminer.client.hud;

import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import com.tcveinminer.logic.HudNotifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.tcveinminer.client.hud.style.HudAnchor;

public class ToastRenderer {
    public static void renderAttached(GuiGraphics ctx, com.tcveinminer.client.hud.style.HudStyle style) {
        if (!com.tcveinminer.client.config.ClientConfigManager.instance.showToast) return;
        if (style == com.tcveinminer.client.hud.style.HudStyle.CHAT_LINE) return;
        if (System.currentTimeMillis() >= HudNotifier.notifyAt) return;
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        long remaining = HudNotifier.notifyAt - System.currentTimeMillis();
        float alpha = Math.min(1f, remaining / 500f);
        int alphaInt = (int)(alpha * 255) << 24;

        String msg;
        if (HudNotifier.lastCancelled) {
            msg = Component.translatable("hud.tcveinminer.cancelled",
                    String.valueOf(HudNotifier.lastMined),
                    String.valueOf(HudNotifier.lastMax)).getString();
        } else {
            msg = Component.translatable("hud.tcveinminer.done",
                    String.valueOf(HudNotifier.lastMined),
                    String.valueOf(HudNotifier.lastMax)).getString();
        }

        int msgW = client.font.width(msg);
        
        HudAnchor anchor = com.tcveinminer.client.config.ClientConfigManager.instance.toastAnchor;
        int drawX = com.tcveinminer.client.config.ClientConfigManager.instance.toastPositionX;
        int drawY = com.tcveinminer.client.config.ClientConfigManager.instance.toastPositionY;
        int screenW = ctx.guiWidth();
        int screenH = ctx.guiHeight();

        if (anchor != HudAnchor.CUSTOM) {
            switch (anchor) {
                case TOP_LEFT -> { drawX += 30; drawY += 30; }
                case TOP_CENTER -> { drawX += screenW/2; drawY += 30; }
                case TOP_RIGHT -> { drawX += screenW - 30; drawY += 30; }
                case MIDDLE_LEFT -> { drawX += 30; drawY += screenH/2; }
                case MIDDLE_RIGHT -> { drawX += screenW - 30; drawY += screenH/2; }
                case BOTTOM_LEFT -> { drawX += 30; drawY += screenH - 30; }
                case BOTTOM_RIGHT -> { drawX += screenW - 30; drawY += screenH - 30; }
                case BOTTOM_CENTER -> { drawX += screenW/2; drawY += screenH - 40; }
                case CUSTOM -> {}
            }
        }

        if (anchor == HudAnchor.TOP_CENTER || anchor == HudAnchor.BOTTOM_CENTER || anchor == HudAnchor.CUSTOM) {
            drawX -= msgW / 2;
        } else if (anchor == HudAnchor.TOP_RIGHT || anchor == HudAnchor.MIDDLE_RIGHT || anchor == HudAnchor.BOTTOM_RIGHT) {
            drawX -= msgW;
        }

        ctx.drawString(client.font, msg, drawX, drawY, (ThemeColors.TEXT_VALUE & 0x00FFFFFF) | alphaInt, true);
    }
}
