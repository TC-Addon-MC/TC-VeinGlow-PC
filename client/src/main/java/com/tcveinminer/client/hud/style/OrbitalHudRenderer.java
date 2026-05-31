package com.tcveinminer.client.hud.style;

import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class OrbitalHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        if (!holding && !isMining) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() / 2;

        long speed = isMining ? 400L : 2000L;
        float time = (System.currentTimeMillis() % speed) / (float)speed; // 0.0 to 1.0
        float angle = time * (float) Math.PI * 2f;
        int radius = 16;
        
        int color1 = ThemeColors.GOLD;
        int color2 = ThemeColors.HUD_TEXT;
        int color3 = isMining ? ThemeColors.EMERALD_BORDER : ThemeColors.BORDER_DEFAULT;

        for (int i = 0; i < 3; i++) {
            float offsetAngle = angle + (i * (float) Math.PI * 2f / 3f);
            int dx = (int) (Math.cos(offsetAngle) * radius);
            int dy = (int) (Math.sin(offsetAngle) * radius);
            int c = (i == 0) ? color1 : (i == 1 ? color2 : color3);
            ctx.fill(cx + dx - 1, cy + dy - 1, cx + dx + 2, cy + dy + 2, c);
        }
    }
}
