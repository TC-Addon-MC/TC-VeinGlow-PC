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
        Minecraft.getInstance().setScreen(new com.tcveinminer.client.gui.screens.RadialMenuScreen(null));
    }

    public static void onHudRenderRaw(Object drawContext, Object tickCounter) {
        new com.tcveinminer.client.hud.VeinMinerHudOverlay().onHudRender(
                (GuiGraphics) drawContext,
                (DeltaTracker) tickCounter);
    }

    public static boolean onDrawOutlineRaw(Object matrices, Object camera, Object consumers) {
        return BlockHighlighter.onDrawOutline(
                (PoseStack) matrices,
                (Camera) camera,
                (MultiBufferSource) consumers);
    }

    public static void onDrawFluidHighlightRaw(Object matrices, Object camera, Object consumers) {
        BlockHighlighter.onDrawFluidHighlight(
                (PoseStack) matrices,
                (Camera) camera,
                (MultiBufferSource) consumers);
    }

    private ClientApi() {
    }
}