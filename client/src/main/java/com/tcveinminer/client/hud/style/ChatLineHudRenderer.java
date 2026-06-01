package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ChatLineHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        boolean hasToast = System.currentTimeMillis() < com.tcveinminer.logic.HudNotifier.notifyAt;
        boolean isDone = hasToast && !com.tcveinminer.logic.HudNotifier.lastCancelled;
        boolean isCancelled = hasToast && com.tcveinminer.logic.HudNotifier.lastCancelled;

        if (!holding && !isMining && !hasToast) return;
        Minecraft mc = Minecraft.getInstance();
        
        int cy = ctx.guiHeight() - 48; // Higher to fit 2 lines

        String line1 = "> " + modeName;
        int color1 = ThemeColors.HUD_TEXT_DIM;
        
        if (isMining) {
            color1 = 0xFFFFAA00; // Gold
        } else if (isDone) {
            color1 = 0xFF55FF55; // Green
        } else if (isCancelled) {
            color1 = 0xFFFF5555; // Red
        } else if (holding) {
            color1 = 0xFF55FFFF; // Aqua
        }
        
        int x = 4;
        ctx.drawString(mc.font, line1, x, cy, color1, true);

        if (isCancelled) {
            return; // Don't show text if cancelled
        }

        String line2 = "";
        int color2 = 0xFFAAAAAA;
        if (isMining) {
            line2 = net.minecraft.network.chat.Component.translatable("hud.tcveinminer.style.blocks_progress", com.tcveinminer.logic.HudNotifier.lastMined, com.tcveinminer.logic.HudNotifier.lastMax).getString();
            color2 = 0xFFFFAA00;
        } else if (isDone) {
            line2 = net.minecraft.network.chat.Component.translatable("hud.tcveinminer.style.blocks_progress", com.tcveinminer.logic.HudNotifier.lastMined, com.tcveinminer.logic.HudNotifier.lastMax).getString();
            color2 = 0xFF55FF55;
        } else if (holding) {
            line2 = keyHint; 
        }

        if (!line2.isEmpty()) {
            ctx.drawString(mc.font, line2, x + 10, cy + 12, color2, true);
        }
    }
}
