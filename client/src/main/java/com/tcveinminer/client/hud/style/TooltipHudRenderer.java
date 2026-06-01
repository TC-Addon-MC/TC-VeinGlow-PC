package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class TooltipHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        if (!holding && !isMining) return;
        Minecraft mc = Minecraft.getInstance();
        
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() / 2;
        
        int tx = cx + 15;
        int ty = cy + 15;
        
        String title = net.minecraft.network.chat.Component.translatable("hud.tcveinminer.style.title").getString();
        String status = isMining ? net.minecraft.network.chat.Component.translatable("hud.tcveinminer.style.active").getString() : net.minecraft.network.chat.Component.translatable("hud.tcveinminer.style.ready_short").getString();
        
        int w = Math.max(mc.font.width(modeName), mc.font.width(status)) + 12;
        int h = 30;

        ctx.fill(tx, ty, tx + w, ty + h, 0xDD111111);
        ctx.fill(tx, ty, tx + w, ty + 1, ThemeColors.BORDER_DIM);
        ctx.fill(tx, ty + h - 1, tx + w, ty + h, ThemeColors.BORDER_DIM);
        ctx.fill(tx, ty, tx + 1, ty + h, ThemeColors.BORDER_DIM);
        ctx.fill(tx + w - 1, ty, tx + w, ty + h, ThemeColors.BORDER_DIM);

        ctx.drawString(mc.font, modeName, tx + 6, ty + 5, ThemeColors.GOLD, false);
        ctx.drawString(mc.font, status, tx + 6, ty + 17, isMining ? ThemeColors.SUCCESS : ThemeColors.HUD_TEXT_DIM, false);
    }
}
