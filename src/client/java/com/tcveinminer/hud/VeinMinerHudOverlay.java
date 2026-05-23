package com.tcveinminer.hud;

import com.tcveinminer.TCVeinMinerClient;
import com.tcveinminer.config.ClientConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.logic.HudNotifier;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class VeinMinerHudOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) return;
        if (!ClientConfigManager.instance.showHud) return;
        if (client.options.hudHidden) return;
        if (client.currentScreen != null) return;

        boolean holding = TCVeinMinerClient.holdKeyDown;

        // Resolve shape hiện tại từ ClientConfig (không dùng ModConfig server-side)
        String currentShapeId = ClientConfigManager.instance.currentShape;
        
        String modeLabel = currentShapeId;
        try {
            ModConfig.MiningShape shape = ModConfig.MiningShape.valueOf(currentShapeId);
            
            modeLabel = shape.label;
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // custom shape hoặc chưa set: hiện raw id
        }

        String statusText = (holding ? "SẴN SÀNG" : "CHỜ [V]") + "  " + modeLabel;
        int textW  = client.textRenderer.getWidth(statusText);
        int pillW  = textW + 16;
        int x      = ClientConfigManager.instance.hudPositionX;
        int y      = ClientConfigManager.instance.hudPositionY;

        DrawHelper.drawHudPill(ctx, x, y, pillW, 14, holding);
        int textColor = holding ? ThemeColors.HUD_ON_TEXT : ThemeColors.HUD_OFF_TEXT;
        ctx.drawTextWithShadow(client.textRenderer, statusText, x + 8, y + 3, textColor);

        // Dòng 2: tiến trình đào (hiện trong 2.5s sau khi đào xong)
        if (System.currentTimeMillis() < HudNotifier.notifyAt) {
            long remaining = HudNotifier.notifyAt - System.currentTimeMillis();
            float alpha = Math.min(1f, remaining / 500f);
            int alphaInt = (int)(alpha * 255) << 24;

            String msg = String.format("Đã đào: %d/%d block", HudNotifier.lastMined, HudNotifier.lastMax);
            int msgW     = client.textRenderer.getWidth(msg);
            int msgPillW = msgW + 16;
            int msgY     = y + 18;

            DrawHelper.drawHudPill(ctx, x, msgY, msgPillW, 14, true);
            ctx.drawTextWithShadow(client.textRenderer, msg, x + 8, msgY + 3,
                    (ThemeColors.TEXT_VALUE & 0x00FFFFFF) | alphaInt);
        }
    }
}
