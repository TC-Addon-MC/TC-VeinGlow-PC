package com.tcveinminer.hud;

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
        if (!ModConfig.get().showHud) return;
        if (mc.options.hudHidden || mc.currentScreen != null) return;

        boolean on  = ModConfig.get().enabled;
        String  txt = "⛏ VeinMiner: " + (on ? "BẬT" : "TẮT");
        int     pw  = mc.textRenderer.getWidth(txt) + 14;

        Draw.pill(ctx, 6, 6, pw, 14, on);
        ctx.drawTextWithShadow(mc.textRenderer, txt, 13, 9, on ? TC.ON_TEXT : TC.OFF_TEXT);

        // Mining progress (set bởi VeinMinerLogic qua HudNotifier)
        if (System.currentTimeMillis() < HudNotifier.notifyAt) {
            String msg = String.format("Đang đào... %d/%d", HudNotifier.lastMined, HudNotifier.lastMax);
            int mw = mc.textRenderer.getWidth(msg) + 14;
            Draw.pill(ctx, 6, 24, mw, 14, true);
            ctx.drawTextWithShadow(mc.textRenderer, msg, 13, 27, TC.TXT_VALUE);
        }
    }
}
