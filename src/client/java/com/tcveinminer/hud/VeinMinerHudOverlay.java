package com.tcveinminer.hud;

import com.tcveinminer.config.ConfigManager;
import com.tcveinminer.util.DrawHelper;
import com.tcveinminer.util.ThemeColors;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class VeinMinerHudOverlay implements HudRenderCallback {

    // Message khi đang đào: "Đang đào... 23/64", fade sau 2.5s
    private static String miningMessage = null;
    private static long miningMessageExpiry = 0;

    public static void showMiningProgress(int found, int max) {
        miningMessage = String.format("Đang đào... %d/%d", found, max);
        miningMessageExpiry = System.currentTimeMillis() + 2500;
    }

    public static void clearMiningMessage() {
        miningMessage = null;
    }

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!ConfigManager.get().showHud) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) return;
        if (client.currentScreen != null) return; // ẩn khi mở menu

        boolean on = ConfigManager.get().enabled;
        String statusText = "⛏ VeinMiner: " + (on ? "BẬT" : "TẮT");

        int textW = client.textRenderer.getWidth(statusText);
        int pillW = textW + 16;
        int pillH = 14;
        int x = 6, y = 6;

        DrawHelper.drawHudPill(ctx, x, y, pillW, pillH, on);
        int textColor = on ? ThemeColors.HUD_ON_TEXT : ThemeColors.HUD_OFF_TEXT;
        ctx.drawTextWithShadow(client.textRenderer, statusText, x + 8, y + 3, textColor);

        // Mining progress message (fade)
        if (miningMessage != null && System.currentTimeMillis() < miningMessageExpiry) {
            long remaining = miningMessageExpiry - System.currentTimeMillis();
            float alpha = Math.min(1f, remaining / 500f); // fade trong 0.5s cuối
            int alphaInt = (int)(alpha * 255) << 24;
            int msgW = client.textRenderer.getWidth(miningMessage);
            int msgPillW = msgW + 16;
            int msgY = y + pillH + 4;

            DrawHelper.drawHudPill(ctx, x, msgY, msgPillW, pillH, true);
            ctx.drawTextWithShadow(client.textRenderer, miningMessage, x + 8, msgY + 3,
                (ThemeColors.TEXT_VALUE & 0x00FFFFFF) | alphaInt);
        } else {
            miningMessage = null;
        }
    }
}
