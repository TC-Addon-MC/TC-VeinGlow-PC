package com.tcveinminer.client.hud;

import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import com.tcveinminer.logic.HudNotifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ToastRenderer {
    public static void render(GuiGraphics ctx) {
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
        int msgPillW = msgW + 16;
        
        int x = (ctx.guiWidth() - msgPillW) / 2;
        int y = ctx.guiHeight() - 40;

        DrawHelper.drawHudPill(ctx, x, y, msgPillW, 14, true);
        ctx.drawString(client.font, msg, x + 8, y + 3,
                (ThemeColors.TEXT_VALUE & 0x00FFFFFF) | alphaInt);
    }
}
