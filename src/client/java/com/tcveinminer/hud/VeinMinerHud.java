package com.tcveinminer.hud;

import com.tcveinminer.TCVeinMinerClient;
import com.tcveinminer.config.ModConfig;
import com.tcveinminer.gui.Draw;
import com.tcveinminer.gui.TC;
import com.tcveinminer.logic.HudNotifier;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class VeinMinerHud implements HudRenderCallback {

    @Override
    public void onHudRender(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!ModConfig.get().enabled) return; // Nếu mod bị tắt hoàn toàn thì không hiện HUD

        // Hiển thị trạng thái dựa trên việc người chơi có ĐANG ĐÈ PHÍM hay không
        boolean isMiningActive = TCVeinMinerClient.holdKeyDown;
        String status = isMiningActive ? "SẴN SÀNG ĐÀO" : "CHỜ (ĐÈ PHÍM)";

        String txt = "⛏ VeinMiner: " + status;
        int pw = mc.textRenderer.getWidth(txt) + 14;

        // Hiển thị màu khác nhau để dễ nhận biết
        Draw.pill(ctx, 6, 6, pw, 14, isMiningActive);
        ctx.drawTextWithShadow(mc.textRenderer, txt, 13, 9, isMiningActive ? TC.ON_TEXT : TC.TXT_LABEL);
        // Mining progress (set bởi VeinMinerLogic qua HudNotifier)
        if (System.currentTimeMillis() < HudNotifier.notifyAt) {
            String msg = String.format("Đang đào... %d/%d", HudNotifier.lastMined, HudNotifier.lastMax);
            int mw = mc.textRenderer.getWidth(msg) + 14;
            Draw.pill(ctx, 6, 24, mw, 14, true);
            ctx.drawTextWithShadow(mc.textRenderer, msg, 13, 27, TC.TXT_VALUE);
        }
    }
}
