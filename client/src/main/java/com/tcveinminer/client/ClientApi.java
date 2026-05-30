package com.tcveinminer.client;

import com.tcveinminer.client.logic.BlockHighlighter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;

public final class ClientApi {
    public static void openRadialMenuRaw() {
        MinecraftClient.getInstance().setScreen(new com.tcveinminer.client.gui.screens.RadialMenuScreen(null));
    }

    public static void onHudRenderRaw(Object drawContext, Object tickCounter) {
        new com.tcveinminer.client.hud.VeinMinerHudOverlay().onHudRender(
                (GuiGraphics) drawContext,
                (RenderTickCounter) tickCounter);
    }

    public static boolean onDrawOutlineRaw(Object matrices, Object camera, Object consumers) {
        return BlockHighlighter.onDrawOutline(
                (MatrixStack) matrices,
                (Camera) camera,
                (VertexConsumerProvider) consumers);
    }

    public static void onDrawFluidHighlightRaw(Object matrices, Object camera, Object consumers) {
        BlockHighlighter.onDrawFluidHighlight(
                (MatrixStack) matrices,
                (Camera) camera,
                (VertexConsumerProvider) consumers);
    }

    private ClientApi() {
    }
}