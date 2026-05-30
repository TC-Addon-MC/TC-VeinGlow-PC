package com.tcveinminer.client.logic;

import com.tcveinminer.client.VeinGlowClient;
import com.tcveinminer.client.config.ClientConfig;
import com.tcveinminer.client.config.ClientConfigManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.*;

public class BlockHighlighter {

    public static boolean allowContinuous = false;
    public static BlockPos lookedAtBlock = null;
    public static boolean allowHighlight = false;
    public static Set<BlockPos> highlightBlocks = new HashSet<>();
    public static String highlightStyle = "FACE";

    public static void register() {
        // Registration should happen in loader-specific entrypoints
    }

    public static void onDrawFluidHighlight(PoseStack matrices, Camera camera, MultiBufferSource consumers) {
        if (!VeinGlowClient.holdKeyDown)
            return;
        if (!ClientConfigManager.instance.showOutline)
            return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null)
            return;
        if (client.player.getMainHandItem().getItem() != net.minecraft.world.item.Items.BUCKET)
            return;
        if (lookedAtBlock == null)
            return;

        var fluidState = client.level.getFluidState(lookedAtBlock);
        if (fluidState.isEmpty() || !fluidState.isSource())
            return;

        Set<BlockPos> toHighlight = new HashSet<>();
        if (allowHighlight && !highlightBlocks.isEmpty()) {
            toHighlight.addAll(highlightBlocks);
        } else {
            toHighlight.add(lookedAtBlock);
        }

        drawOutlines(matrices, camera, client, toHighlight, !allowHighlight);
    }

    public static boolean onDrawOutline(PoseStack matrices, Camera camera, MultiBufferSource consumers) {
        if (!VeinGlowClient.holdKeyDown || lookedAtBlock == null) {
            return true;
        }
        if (!ClientConfigManager.instance.showOutline)
            return true;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null)
            return true;

        Set<BlockPos> toHighlight = new HashSet<>();
        if (allowHighlight) {
            toHighlight.addAll(highlightBlocks);
        } else {
            toHighlight.add(lookedAtBlock);
        }
        if (toHighlight.isEmpty())
            return true;

        drawOutlines(matrices, camera, client, toHighlight, !allowHighlight);
        return false;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private static void drawOutlines(PoseStack matrices, Camera camera,
            Minecraft client, Set<BlockPos> blockSet, boolean isTargetInvalid) {
        Map<Long, EdgeData> edgeCount = new HashMap<>();
        for (BlockPos pos : blockSet) {
            BlockState state = client.level.getBlockState(pos);
            VoxelShape shape = state.getShape(client.level, pos);
            if (shape.isEmpty()) {
                var fluidState = client.level.getFluidState(pos);
                if (!fluidState.isEmpty()) {
                    shape = fluidState.getShape(client.level, pos);
                }
            }
            if (shape.isEmpty())
                continue;
            AABB box = shape.bounds();
            addAllEdges(edgeCount, pos, box);
        }

        ClientConfig cfg = ClientConfigManager.instance;
        float[] fallbackRgb = isTargetInvalid
                ? new float[] { 0.937f, 0.267f, 0.267f }
                : getFlowColor(0, 0, 0, cfg);
        float r = fallbackRgb[0], g = fallbackRgb[1], b = fallbackRgb[2];
        float alpha = Math.max(0f, Math.min(1f, cfg.outlineAlpha / 255f));
        float alphaXray = alpha * 0.25f;

        Vec3 cameraPos = camera.getPosition();

        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f mat = matrices.last().pose();

        float camX = (float) cameraPos.x;
        float camY = (float) cameraPos.y;
        float camZ = (float) cameraPos.z;

        float maxQuadLen = 0.05f;

        Tesselator tessellator = Tesselator.getInstance();

        // ── Xray pass (depth ALWAYS, alpha thấp) ──────────────────────────────
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(515); // GL_ALWAYS = 519, GL_LEQUAL = 515 — xray dùng ALWAYS
        RenderSystem.depthFunc(519); // GL_ALWAYS

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder xrayBuf = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        renderEdges(xrayBuf, mat, edgeCount, cfg, isTargetInvalid, fallbackRgb, alphaXray,
                camX, camY, camZ, maxQuadLen);

        if (isTargetInvalid) {
            renderInvalidX(xrayBuf, mat, blockSet, client, cfg, camX, camY, camZ, r, g, b, alphaXray);
        }

        var builtXray = xrayBuf.build();
        if (builtXray != null) {
            BufferUploader.drawWithShader(builtXray);
        }

        // ── Solid pass (depth LEQUAL, alpha đầy đủ) ───────────────────────────
        RenderSystem.depthFunc(515); // GL_LEQUAL

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder solidBuf = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        renderEdges(solidBuf, mat, edgeCount, cfg, isTargetInvalid, fallbackRgb, alpha,
                camX, camY, camZ, maxQuadLen);

        if (isTargetInvalid) {
            renderInvalidX(solidBuf, mat, blockSet, client, cfg, camX, camY, camZ, r, g, b, alpha);
        }

        var builtSolid = solidBuf.build();
        if (builtSolid != null) {
            BufferUploader.drawWithShader(builtSolid);
        }

        // ── Restore GL state ──────────────────────────────────────────────────
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthFunc(515); // restore to LEQUAL

        matrices.popPose();
    }

    private static void renderEdges(BufferBuilder buf, Matrix4f mat,
            Map<Long, EdgeData> edgeCount, ClientConfig cfg,
            boolean isTargetInvalid, float[] fallbackRgb, float alpha,
            float camX, float camY, float camZ, float maxQuadLen) {

        for (EdgeData ed : edgeCount.values()) {
            if (ed.count != 1)
                continue;

            float ex = ed.bx - ed.ax;
            float ey = ed.by - ed.ay;
            float ez = ed.bz - ed.az;

            float mx = (ed.ax + ed.bx) * 0.5f;
            float my = (ed.ay + ed.by) * 0.5f;
            float mz = (ed.az + ed.bz) * 0.5f;
            float toX = camX - mx, toY = camY - my, toZ = camZ - mz;

            float ox = ey * toZ - ez * toY;
            float oy = ez * toX - ex * toZ;
            float oz = ex * toY - ey * toX;

            float len = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
            if (len < 1e-6f)
                continue;

            float edgeHalfWidth = getDynamicHalfWidth(cfg.outlineThickness, mx, my, mz, camX, camY, camZ);
            float invLen = edgeHalfWidth / len;
            ox *= invLen;
            oy *= invLen;
            oz *= invLen;

            float edgeLen = (float) Math.sqrt(ex * ex + ey * ey + ez * ez);
            int subDivs = Math.max(1, (int) Math.ceil(edgeLen / maxQuadLen));

            float stepX = ex / subDivs;
            float stepY = ey / subDivs;
            float stepZ = ez / subDivs;

            for (int i = 0; i < subDivs; i++) {
                float ax = ed.ax + stepX * i;
                float ay = ed.ay + stepY * i;
                float az = ed.az + stepZ * i;
                float bx = ed.ax + stepX * (i + 1);
                float by = ed.ay + stepY * (i + 1);
                float bz = ed.az + stepZ * (i + 1);

                float[] rgbA = isTargetInvalid ? fallbackRgb : getFlowColor(ax, ay, az, cfg);
                float[] rgbB = isTargetInvalid ? fallbackRgb : getFlowColor(bx, by, bz, cfg);

                float p0x = ax + ox, p0y = ay + oy, p0z = az + oz;
                float p1x = ax - ox, p1y = ay - oy, p1z = az - oz;
                float p2x = bx - ox, p2y = by - oy, p2z = bz - oz;
                float p3x = bx + ox, p3y = by + oy, p3z = bz + oz;

                buf.addVertex(mat, p0x, p0y, p0z).setColor(rgbA[0], rgbA[1], rgbA[2], alpha);
                buf.addVertex(mat, p1x, p1y, p1z).setColor(rgbA[0], rgbA[1], rgbA[2], alpha);
                buf.addVertex(mat, p2x, p2y, p2z).setColor(rgbB[0], rgbB[1], rgbB[2], alpha);
                buf.addVertex(mat, p3x, p3y, p3z).setColor(rgbB[0], rgbB[1], rgbB[2], alpha);
            }
        }
    }

    private static void renderInvalidX(BufferBuilder buf, Matrix4f mat,
            Set<BlockPos> blockSet, Minecraft client, ClientConfig cfg,
            float camX, float camY, float camZ,
            float r, float g, float b, float alpha) {
        for (BlockPos pos : blockSet) {
            BlockState state = client.level.getBlockState(pos);
            VoxelShape shape = state.getShape(client.level, pos);
            if (shape.isEmpty())
                continue;
            AABB box = shape.bounds();
            float x0 = (float) (pos.getX() + box.minX), x1 = (float) (pos.getX() + box.maxX);
            float y1f = (float) (pos.getY() + box.maxY);
            float z0 = (float) (pos.getZ() + box.minZ), z1 = (float) (pos.getZ() + box.maxZ);

            float halfWidth = getDynamicHalfWidth(cfg.outlineThickness,
                    (x0 + x1) * 0.5f, y1f, (z0 + z1) * 0.5f, camX, camY, camZ);

            drawFlatEdgeQuad(buf, mat, x0, y1f, z0, x1, y1f, z1,
                    camX, camY, camZ, halfWidth, r, g, b, alpha);
        }
    }

    private static float getDynamicHalfWidth(float thickness, float mx, float my, float mz,
            float camX, float camY, float camZ) {
        float dx = camX - mx, dy = camY - my, dz = camZ - mz;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return dist * thickness * 0.0006f;
    }

    private static void drawFlatEdgeQuad(BufferBuilder buf, Matrix4f mat,
            float ax, float ay, float az, float bx, float by, float bz,
            float camX, float camY, float camZ, float halfWidth,
            float r, float g, float b, float alpha) {
        float ex = bx - ax, ey = by - ay, ez = bz - az;
        float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f, mz = (az + bz) * 0.5f;
        float toX = camX - mx, toY = camY - my, toZ = camZ - mz;
        float ox = ey * toZ - ez * toY;
        float oy = ez * toX - ex * toZ;
        float oz = ex * toY - ey * toX;
        float len = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
        if (len < 1e-6f)
            return;
        float inv = halfWidth / len;
        ox *= inv;
        oy *= inv;
        oz *= inv;

        buf.addVertex(mat, ax + ox, ay + oy, az + oz).setColor(r, g, b, alpha);
        buf.addVertex(mat, ax - ox, ay - oy, az - oz).setColor(r, g, b, alpha);
        buf.addVertex(mat, bx - ox, by - oy, bz - oz).setColor(r, g, b, alpha);
        buf.addVertex(mat, bx + ox, by + oy, bz + oz).setColor(r, g, b, alpha);
    }

    // ── Color Utilities ───────────────────────────────────────────────────────

    private static float[] getFlowColor(float x, float y, float z, ClientConfig cfg) {
        if (cfg.colorDisabled)
            return new float[] { 0.5f, 0.5f, 0.5f };

        if (cfg.colorList == null || cfg.colorList.isEmpty()) {
            if (cfg.colorRainbow) {
                float hue = (System.currentTimeMillis() % 4000) / 4000f;
                return hsvToRgb(hue, 1f, 1f);
            }
            return new float[] { cfg.colorR / 255f, cfg.colorG / 255f, cfg.colorB / 255f };
        }

        int n = cfg.colorList.size();
        if (n == 1) {
            int[] rgb = com.tcveinminer.client.util.ColorManager.fromHex(cfg.colorList.get(0));
            if (rgb != null)
                return new float[] { rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f };
            return new float[] { 1f, 1f, 1f };
        }

        float d = x + y + z;
        float timeOffset = 0;
        if (cfg.enableFlowAnimation) {
            float speed = 1.0f / Math.max(0.1f, cfg.colorTransitionTime);
            timeOffset = (System.currentTimeMillis() % 100000) / 1000f * speed;
        }

        float segmentLen = Math.max(0.1f, cfg.segmentLength);
        float phase = (d / segmentLen) - timeOffset;
        phase = (phase % n + n) % n;

        int index1 = (int) phase;
        int index2 = (index1 + 1) % n;
        float t = phase - index1;
        float smooth = Math.max(0.001f, Math.min(1.0f, cfg.flowSmoothness));

        if (smooth < 1.0f) {
            float mul = 1.0f / smooth;
            t = (t - 0.5f) * mul + 0.5f;
            t = Math.max(0f, Math.min(1f, t));
        }

        int[] rgb1 = com.tcveinminer.client.util.ColorManager.fromHex(cfg.colorList.get(index1));
        int[] rgb2 = com.tcveinminer.client.util.ColorManager.fromHex(cfg.colorList.get(index2));
        if (rgb1 == null || rgb2 == null)
            return new float[] { 1f, 1f, 1f };

        float r = (rgb1[0] + (rgb2[0] - rgb1[0]) * t) / 255f;
        float g = (rgb1[1] + (rgb2[1] - rgb1[1]) * t) / 255f;
        float b = (rgb1[2] + (rgb2[2] - rgb1[2]) * t) / 255f;
        return new float[] { r, g, b };
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        return switch (i % 6) {
            case 0 -> new float[] { v, t, p };
            case 1 -> new float[] { q, v, p };
            case 2 -> new float[] { p, v, t };
            case 3 -> new float[] { p, q, v };
            case 4 -> new float[] { t, p, v };
            default -> new float[] { v, p, q };
        };
    }

    // ── Edge Merging Logic ────────────────────────────────────────────────────

    private static void addAllEdges(Map<Long, EdgeData> edgeCount, BlockPos pos, AABB box) {
        int bx2 = pos.getX() * 2, by2 = pos.getY() * 2, bz2 = pos.getZ() * 2;
        int x0 = bx2 + (int) (box.minX * 2), x1 = bx2 + (int) (box.maxX * 2);
        int y0 = by2 + (int) (box.minY * 2), y1 = by2 + (int) (box.maxY * 2);
        int z0 = bz2 + (int) (box.minZ * 2), z1 = bz2 + (int) (box.maxZ * 2);
        addEdge(edgeCount, x0, y1, z0, x1, y1, z0);
        addEdge(edgeCount, x0, y1, z1, x1, y1, z1);
        addEdge(edgeCount, x0, y0, z0, x1, y0, z0);
        addEdge(edgeCount, x0, y0, z1, x1, y0, z1);
        addEdge(edgeCount, x0, y0, z0, x0, y1, z0);
        addEdge(edgeCount, x1, y0, z0, x1, y1, z0);
        addEdge(edgeCount, x0, y0, z1, x0, y1, z1);
        addEdge(edgeCount, x1, y0, z1, x1, y1, z1);
        addEdge(edgeCount, x0, y1, z0, x0, y1, z1);
        addEdge(edgeCount, x1, y1, z0, x1, y1, z1);
        addEdge(edgeCount, x0, y0, z0, x0, y0, z1);
        addEdge(edgeCount, x1, y0, z0, x1, y0, z1);
    }

    private static void addEdge(Map<Long, EdgeData> map,
            int ax2, int ay2, int az2, int bx2, int by2, int bz2) {
        long key = edgeKey(ax2, ay2, az2, bx2, by2, bz2);
        EdgeData ed = map.get(key);
        if (ed == null) {
            map.put(key, new EdgeData(ax2 / 2f, ay2 / 2f, az2 / 2f, bx2 / 2f, by2 / 2f, bz2 / 2f));
        } else {
            ed.count++;
        }
    }

    private static long edgeKey(int ax2, int ay2, int az2, int bx2, int by2, int bz2) {
        long pa = packPoint(ax2, ay2, az2), pb = packPoint(bx2, by2, bz2);
        if (pa > pb) {
            long t = pa;
            pa = pb;
            pb = t;
        }
        return pa * 1_000_000_007L + pb;
    }

    private static long packPoint(int x2, int y2, int z2) {
        return ((long) (x2 + 4096) * 8193L + (y2 + 4096)) * 8193L + (z2 + 4096);
    }

    private static class EdgeData {
        float ax, ay, az, bx, by, bz;
        int count = 1;

        EdgeData(float ax, float ay, float az, float bx, float by, float bz) {
            this.ax = ax;
            this.ay = ay;
            this.az = az;
            this.bx = bx;
            this.by = by;
            this.bz = bz;
        }
    }
}