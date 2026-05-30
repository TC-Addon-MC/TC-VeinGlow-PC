package com.tcveinminer.client.hud;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.config.ClientConfigManager;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.logic.HudNotifier;
import com.tcveinminer.client.util.DrawHelper;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;

public class VeinMinerHudOverlay {

    public void onHudRender(GuiGraphics ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || minecraft.options == null) return;
        if (!ClientConfigManager.instance.showHud) return;
        if (client.options.hudHidden) return;
        if (client.screen != null) return;

        boolean holding = VeinGlowClient.holdKeyDown;

        // Resolve label chế độ đào hiện tại
        String currentShapeId = ClientConfigManager.instance.currentShape;
        String modeLabel = currentShapeId;
        try {
            ModConfig.MiningShape shape = ModConfig.MiningShape.valueOf(currentShapeId);
            modeLabel = Component.translatable("tc_veinminer.mode." + shape.name()).getString();
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // custom shape hoặc chưa set: hiện raw id
        }

        // Tên phím (giữ nguyên case từ Minecraft, không toUpperCase)
        int actMode = ClientConfigManager.instance.activationMode;
        String keyName = VeinGlowClient.KEY_MINE == null ? "V" : VeinGlowClient.KEY_MINE.getBoundKeyLocalizedText().getString();

        String keyHint = switch (actMode) {
            case 2 -> Component.translatable("hud.tcveinminer.hint.hold_sneak",  keyName).getString();
            case 3 -> Component.translatable("hud.tcveinminer.hint.toggle",      keyName).getString();
            case 4 -> Component.translatable("hud.tcveinminer.hint.toggle_sneak", keyName).getString();
            default -> Component.translatable("hud.tcveinminer.hint.hold",       keyName).getString();
        };

        String statusText;
        if (VeinGlowClient.isMining) {
            statusText = Component.translatable("hud.tcveinminer.mining",
                    String.valueOf(HudNotifier.lastMined),
                    String.valueOf(HudNotifier.lastMax),
                    modeLabel).getString();
        } else if (holding) {
            statusText = Component.translatable("hud.tcveinminer.ready",   keyHint, modeLabel).getString();
        } else {
            statusText = Component.translatable("hud.tcveinminer.waiting", keyHint, modeLabel).getString();
        }

        int textW  = minecraft.font.width(statusText);
        int pillW  = textW + 16;
        int x      = ClientConfigManager.instance.hudPositionX;
        int y      = ClientConfigManager.instance.hudPositionY;

        DrawHelper.drawHudPill(ctx, x, y, pillW, 14, holding);
        int textColor = holding ? ThemeColors.HUD_ON_TEXT : ThemeColors.HUD_OFF_TEXT;
        ctx.drawTextWithShadow(client.textRenderer, statusText, x + 8, y + 3, textColor);

        // Dòng 2: thông báo kết quả đào (hiện trong 2.5s sau khi đào xong)
        if (System.currentTimeMillis() < HudNotifier.notifyAt) {
            long remaining = HudNotifier.notifyAt - System.currentTimeMillis();
            float alpha    = Math.min(1f, remaining / 500f);
            int alphaInt   = (int)(alpha * 255) << 24;

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

            int msgW     = minecraft.font.width(msg);
            int msgPillW = msgW + 16;
            int msgY     = y + 18;

            DrawHelper.drawHudPill(ctx, x, msgY, msgPillW, 14, true);
            ctx.drawTextWithShadow(client.textRenderer, msg, x + 8, msgY + 3,
                    (ThemeColors.TEXT_VALUE & 0x00FFFFFF) | alphaInt);
        }
    }
}
