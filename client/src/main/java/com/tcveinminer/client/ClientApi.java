package com.tcveinminer.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class ClientApi {
    public static void openRadialMenuRaw() {
        MinecraftClient.getInstance().setScreen(new com.tcveinminer.client.gui.screens.RadialMenuScreen(null));
    }

    public static void onHudRenderRaw(Object drawContext, Object tickCounter) {
        new com.tcveinminer.client.hud.VeinMinerHudOverlay().onHudRender(
                (DrawContext) drawContext,
                (RenderTickCounter) tickCounter);
    }
    
    private ClientApi() {}
}
