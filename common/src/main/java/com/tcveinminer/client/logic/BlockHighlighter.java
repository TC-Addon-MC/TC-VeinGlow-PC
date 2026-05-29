package com.tcveinminer.client.logic;

import com.tcveinminer.TCVeinMinerClient;
import com.tcveinminer.client.config.ClientConfig;
import com.tcveinminer.client.config.ClientConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

public class BlockHighlighter {

    // ── Render Layer Cache ────────────────────────────────────────────────────
    // Dùng QUADS + POSITION_COLOR thay vì LINES + lineWidth.
    // Lý do: GL lineWidth không consistent trên modern drivers — thickness thay đổi
    // theo orientation, distance, FOV và góc nhìn. Quad geometry tự tính billboard
    // nên luôn consistent ở mọi angle/distance/FOV.
    private static RenderLayer cachedSolidLayer;
    private static RenderLayer cachedXrayLayer;

    public static boolean allowContinuous = false;
    public static BlockPos lookedAtBlock = null;
    public static boolean allowHighlight = false;
    public static Set<BlockPos> highlightBlocks = new HashSet<>();
    public static String highlightStyle = "FACE";

    /**
     * Khởi tạo RenderLayer một lần duy nhất.
     * Không cần rebuild theo thickness vì thickness bây giờ được xử lý
     * ở geometry level (tính toán trong CPU, không phụ thuộc GL state).
     */
    private static void ensureRenderLayers() {
        if (cachedSolidLayer != null)
            return;

        cachedSolidLayer = RenderLayer.of(
                "tc_veinminer_solid",
                VertexFormats.POSITION_COLOR,
                VertexFormat.DrawMode.QUADS,
                256, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.COLOR_PROGRAM)
                        .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .target(RenderPhase.MAIN_TARGET)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .build(false));

        cachedXrayLayer = RenderLayer.of(
                "tc_veinminer_xray",
                VertexFormats.POSITION_COLOR,
                VertexFormat.DrawMode.QUADS,
                256, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.COLOR_PROGRAM)
                        .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .target(RenderPhase.MAIN_TARGET)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .build(false));
    }

    public static void register() {
        WorldRenderEvents.BLOCK_OUTLINE.register(BlockHighlighter::onDrawOutline);
        WorldRenderEvents.LAST.register(BlockHighlighter::onDrawFluidHighlight);
    }

    private static void onDrawFluidHighlight(WorldRenderContext context) {
        if (!TCVeinMinerClient.holdKeyDown) return;
        if (!ClientConfigManager.instance.showOutline) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        if (client.player.getMainHandStack().getItem() != net.minecraft.item.Items.BUCKET) return;
        if (lookedAtBlock == null) return;

        var fluidState = client.world.getFluidState(lookedAtBlock);
        if (fluidState.isEmpty() || !fluidState.isStill()) return;

        Set<BlockPos> toHighlight = new HashSet<>();
        if (allowHighlight && !highlightBlocks.isEmpty()) {
            toHighlight.addAll(highlightBlocks);
        } else {
            toHighlight.add(lookedAtBlock);
        }

        drawOutlines(context, client, toHighlight, !allowHighlight);
    }

    private static boolean onDrawOutline(WorldRenderContext context,
            WorldRenderContext.BlockOutlineContext outlineCtx) {
        if (!TCVeinMinerClient.holdKeyDown || lookedAtBlock == null) {
            return true;
        }
        if (!ClientConfigManager.instance.showOutline)
            return true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
            return true;

        Set<BlockPos> toHighlight = new HashSet<>();
        if (allowHighlight) {
            toHighlight.addAll(highlightBlocks);
        } else {
            toHighlight.add(lookedAtBlock);
        }
        if (toHighlight.isEmpty())
            return true;

        drawOutlines(context, client, toHighlight, !allowHighlight);
        return false;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private static void drawOutlines(WorldRenderContext context, MinecraftClient client,
            Set<BlockPos> blockSet, boolean isTargetInvalid) {
        Map<Long, EdgeData> edgeCount = new HashMap<>();
        for (BlockPos pos : blockSet) {
            var state = client.world.getBlockState(pos);
            var shape = state.getOutlineShape(client.world, pos);
            if (shape.isEmpty()) {
                var fluidState = client.world.getFluidState(pos);
                if (!fluidState.isEmpty()) {
                    shape = fluidState.getShape(client.world, pos);
                }
            }
            if (shape.isEmpty())
                continue;
            Box box = shape.getBoundingBox();
            addAllEdges(edgeCount, pos, box);
        }

        ensureRenderLayers();

        ClientConfig cfg = ClientConfigManager.instance;
        float[] fallbackRgb = isTargetInvalid
                ? new float[] { 0.937f, 0.267f, 0.267f }
                : getFlowColor(0, 0, 0, cfg);
        float r = fallbackRgb[0], g = fallbackRgb[1], b = fallbackRgb[2];
        float alpha = Math.max(0f, Math.min(1f, cfg.outlineAlpha / 255f));
        float alphaXray = alpha * 0.25f;

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        VertexConsumer solid = context.consumers().getBuffer(cachedSolidLayer);
        VertexConsumer xray = context.consumers().getBuffer(cachedXrayLayer);

        // Camera position as Vector3f để tính billboard direction
        float camX = (float) cameraPos.x;
        float camY = (float) cameraPos.y;
        float camZ = (float) cameraPos.z;

        float maxQuadLen = 0.05f;

        for (EdgeData ed : edgeCount.values()) {
            if (ed.count != 1)
                continue;

            // ── Billboard quad cho edge (ax,ay,az) → (bx,by,bz) ──────────────
            float ex = ed.bx - ed.ax;
            float ey = ed.by - ed.ay;
            float ez = ed.bz - ed.az;

            float mx = (ed.ax + ed.bx) * 0.5f;
            float my = (ed.ay + ed.by) * 0.5f;
            float mz = (ed.az + ed.bz) * 0.5f;
            float toX = camX - mx;
            float toY = camY - my;
            float toZ = camZ - mz;

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
            int subDivs = (int) Math.ceil(edgeLen / maxQuadLen);
            if (subDivs < 1)
                subDivs = 1;

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

                // Xray layer (alpha thấp, xuyên tường)
                xray.vertex(mat, p0x, p0y, p0z).color(rgbA[0], rgbA[1], rgbA[2], alphaXray);
                xray.vertex(mat, p1x, p1y, p1z).color(rgbA[0], rgbA[1], rgbA[2], alphaXray);
                xray.vertex(mat, p2x, p2y, p2z).color(rgbB[0], rgbB[1], rgbB[2], alphaXray);
                xray.vertex(mat, p3x, p3y, p3z).color(rgbB[0], rgbB[1], rgbB[2], alphaXray);

                // Solid layer
                solid.vertex(mat, p0x, p0y, p0z).color(rgbA[0], rgbA[1], rgbA[2], alpha);
                solid.vertex(mat, p1x, p1y, p1z).color(rgbA[0], rgbA[1], rgbA[2], alpha);
                solid.vertex(mat, p2x, p2y, p2z).color(rgbB[0], rgbB[1], rgbB[2], alpha);
                solid.vertex(mat, p3x, p3y, p3z).color(rgbB[0], rgbB[1], rgbB[2], alpha);
            }
        }

        // Invalid target: vẽ thêm dấu X trên mặt trên của block
        if (isTargetInvalid) {
            for (BlockPos pos : blockSet) {
                var shape = client.world.getBlockState(pos).getOutlineShape(client.world, pos);
                if (shape.isEmpty())
                    continue;
                Box box = shape.getBoundingBox();
                float x0 = (float) (pos.getX() + box.minX), x1 = (float) (pos.getX() + box.maxX);
                float y1 = (float) (pos.getY() + box.maxY);
                float z0 = (float) (pos.getZ() + box.minZ), z1 = (float) (pos.getZ() + box.maxZ);

                float diagHalfWidth = getDynamicHalfWidth(cfg.outlineThickness, (x0 + x1) * 0.5f, y1, (z0 + z1) * 0.5f,
                        camX, camY, camZ);

                // Dấu chéo trên mặt trên — vẫn dùng billboard quad logic
                drawFlatEdgeQuad(mat, solid, xray, x0, y1, z0, x1, y1, z1,
                        camX, camY, camZ, diagHalfWidth, r, g, b, alpha, alphaXray);
            }
        }

        matrices.pop();
    }

    private static float getDynamicHalfWidth(float thickness, float mx, float my, float mz, float camX, float camY,
            float camZ) {
        float dx = camX - mx;
        float dy = camY - my;
        float dz = camZ - mz;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return dist * thickness * 0.0006f;
    }

    /**
     * Helper riêng cho edge nằm trên mặt phẳng ngang (y cố định).
     * Bởi vì edge nằm ngang, billboard sẽ offset theo trục Y để luôn nhìn thấy.
     */
    private static void drawFlatEdgeQuad(Matrix4f mat,
            VertexConsumer solid, VertexConsumer xray,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float camX, float camY, float camZ,
            float halfWidth,
            float r, float g, float b,
            float alpha, float alphaXray) {
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

        xray.vertex(mat, ax + ox, ay + oy, az + oz).color(r, g, b, alphaXray);
        xray.vertex(mat, ax - ox, ay - oy, az - oz).color(r, g, b, alphaXray);
        xray.vertex(mat, bx - ox, by - oy, bz - oz).color(r, g, b, alphaXray);
        xray.vertex(mat, bx + ox, by + oy, bz + oz).color(r, g, b, alphaXray);

        solid.vertex(mat, ax + ox, ay + oy, az + oz).color(r, g, b, alpha);
        solid.vertex(mat, ax - ox, ay - oy, az - oz).color(r, g, b, alpha);
        solid.vertex(mat, bx - ox, by - oy, bz - oz).color(r, g, b, alpha);
        solid.vertex(mat, bx + ox, by + oy, bz + oz).color(r, g, b, alpha);
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
            int[] rgb = com.tcveinminer.util.ColorManager.fromHex(cfg.colorList.get(0));
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

        int[] rgb1 = com.tcveinminer.util.ColorManager.fromHex(cfg.colorList.get(index1));
        int[] rgb2 = com.tcveinminer.util.ColorManager.fromHex(cfg.colorList.get(index2));

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

    private static void addAllEdges(Map<Long, EdgeData> edgeCount, BlockPos pos, Box box) {
        int bx2 = pos.getX() * 2, by2 = pos.getY() * 2, bz2 = pos.getZ() * 2;
        int x0 = bx2 + (int) (box.minX * 2), x1 = bx2 + (int) (box.maxX * 2);
        int y0 = by2 + (int) (box.minY * 2), y1 = by2 + (int) (box.maxY * 2);
        int z0 = bz2 + (int) (box.minZ * 2), z1 = bz2 + (int) (box.maxZ * 2);
        addEdge(edgeCount, pos, box, x0, y1, z0, x1, y1, z0);
        addEdge(edgeCount, pos, box, x0, y1, z1, x1, y1, z1);
        addEdge(edgeCount, pos, box, x0, y0, z0, x1, y0, z0);
        addEdge(edgeCount, pos, box, x0, y0, z1, x1, y0, z1);
        addEdge(edgeCount, pos, box, x0, y0, z0, x0, y1, z0);
        addEdge(edgeCount, pos, box, x1, y0, z0, x1, y1, z0);
        addEdge(edgeCount, pos, box, x0, y0, z1, x0, y1, z1);
        addEdge(edgeCount, pos, box, x1, y0, z1, x1, y1, z1);
        addEdge(edgeCount, pos, box, x0, y1, z0, x0, y1, z1);
        addEdge(edgeCount, pos, box, x1, y1, z0, x1, y1, z1);
        addEdge(edgeCount, pos, box, x0, y0, z0, x0, y0, z1);
        addEdge(edgeCount, pos, box, x1, y0, z0, x1, y0, z1);
    }

    private static void addEdge(Map<Long, EdgeData> map, BlockPos pos, Box box,
            int ax2, int ay2, int az2, int bx2, int by2, int bz2) {
        long key = edgeKey(ax2, ay2, az2, bx2, by2, bz2);
        EdgeData ed = map.get(key);
        if (ed == null) {
            float ax = ax2 / 2f, ay = ay2 / 2f, az = az2 / 2f;
            float bx = bx2 / 2f, by = by2 / 2f, bz = bz2 / 2f;
            // Normal vector không còn cần thiết cho quad rendering,
            // nhưng giữ lại EdgeData structure để tránh break thêm code.
            map.put(key, new EdgeData(ax, ay, az, bx, by, bz));
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
