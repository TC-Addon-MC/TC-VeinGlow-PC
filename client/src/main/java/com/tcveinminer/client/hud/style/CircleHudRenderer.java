package com.tcveinminer.client.hud.style;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tcveinminer.client.util.ThemeColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class CircleHudRenderer implements IHudRenderer {
    @Override
    public void render(GuiGraphics ctx, int customX, int customY, HudAnchor anchor, boolean isMining, boolean holding, String modeName, String keyHint) {
        if (!holding && !isMining) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = ctx.guiWidth() / 2;
        int cy = ctx.guiHeight() / 2;

        PoseStack pose = ctx.pose();
        pose.pushPose();
        Matrix4f mat = pose.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float radius = 12f;
        float thickness = 1.5f;
        int color = isMining ? ThemeColors.GOLD : ThemeColors.HUD_TEXT;

        int segments = 32;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * Math.PI * 2 / segments);
            float angle2 = (float) ((i + 1) * Math.PI * 2 / segments);

            float cos1 = (float) Math.cos(angle1), sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2), sin2 = (float) Math.sin(angle2);

            float r1 = radius - thickness, r2 = radius + thickness;

            int a = (color >> 24) & 0xFF;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            buf.addVertex(mat, cx + cos1 * r1, cy + sin1 * r1, 0).setColor(r, g, b, a);
            buf.addVertex(mat, cx + cos1 * r2, cy + sin1 * r2, 0).setColor(r, g, b, a);
            buf.addVertex(mat, cx + cos2 * r2, cy + sin2 * r2, 0).setColor(r, g, b, a);

            buf.addVertex(mat, cx + cos1 * r1, cy + sin1 * r1, 0).setColor(r, g, b, a);
            buf.addVertex(mat, cx + cos2 * r2, cy + sin2 * r2, 0).setColor(r, g, b, a);
            buf.addVertex(mat, cx + cos2 * r1, cy + sin2 * r1, 0).setColor(r, g, b, a);
        }

        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.disableBlend();
        pose.popPose();

        if (isMining) {
            ctx.drawCenteredString(mc.font, modeName, cx, cy + 18, ThemeColors.GOLD);
        }
    }
}
