package com.tcveinminer.logic;

import com.tcveinminer.TCVeinMinerClient;
import com.tcveinminer.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.*;
import java.util.OptionalDouble;

public class BlockHighlighter {

    // RenderLayer giống getLines() nhưng tắt depth test → vẽ xuyên block
    private static final RenderLayer LINES_NO_DEPTH = RenderLayer.of(
            "tc_veinminer_lines_no_depth",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            256,
            false, false,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1.0)))
                    .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .target(RenderPhase.ITEM_ENTITY_TARGET)
                    .writeMaskState(RenderPhase.COLOR_MASK)        // chỉ write color, không write depth
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)      // luôn pass depth test → xuyên block
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false)
    );

    public static void register() {
        WorldRenderEvents.BLOCK_OUTLINE.register(BlockHighlighter::onDrawOutline);
    }

    private static boolean onDrawOutline(WorldRenderContext context, WorldRenderContext.BlockOutlineContext outlineContext) {
        if (!TCVeinMinerClient.holdKeyDown) return true;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return true;

        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return true;

        BlockPos targetPos = ((BlockHitResult) hit).getBlockPos();
        BlockState targetState = client.world.getBlockState(targetPos);

        List<BlockPos> blocksToMine = VeinMinerLogic.bfs(client.world, targetPos, targetState, ConfigManager.get());
        if (blocksToMine.isEmpty()) return true;

        Set<BlockPos> blockSet = new HashSet<>(blocksToMine);
        blockSet.add(targetPos);

        // --- Đếm số lần mỗi cạnh world xuất hiện ---
        // Cạnh xuất hiện đúng 1 lần → cạnh ngoài → vẽ
        // Cạnh xuất hiện 2 lần     → cạnh nội bộ (2 block share) → ẩn
        Map<Long, EdgeData> edgeCount = new HashMap<>();

        for (BlockPos pos : blockSet) {
            Box box = client.world.getBlockState(pos).getOutlineShape(client.world, pos).getBoundingBox();

            // 12 cạnh, mỗi cạnh = 2 điểm, encode bằng int*2 (nhân 2 để tránh float)
            // Tọa độ world = pos + offset (0 hoặc 1)
            // Encode điểm: (x*2, y*2, z*2) trong không gian *2 để tránh phân số
            int bx2 = pos.getX() * 2;
            int by2 = pos.getY() * 2;
            int bz2 = pos.getZ() * 2;

            // min/max của box theo đơn vị *2
            int x0 = bx2 + (int)(box.minX * 2);
            int x1 = bx2 + (int)(box.maxX * 2);
            int y0 = by2 + (int)(box.minY * 2);
            int y1 = by2 + (int)(box.maxY * 2);
            int z0 = bz2 + (int)(box.minZ * 2);
            int z1 = bz2 + (int)(box.maxZ * 2);

            // 12 cạnh: 4 theo X, 4 theo Y, 4 theo Z
            addEdge(edgeCount, pos, box,  x0,y1,z0, x1,y1,z0);  // top-front   (X)
            addEdge(edgeCount, pos, box,  x0,y1,z1, x1,y1,z1);  // top-back    (X)
            addEdge(edgeCount, pos, box,  x0,y0,z0, x1,y0,z0);  // bot-front   (X)
            addEdge(edgeCount, pos, box,  x0,y0,z1, x1,y0,z1);  // bot-back    (X)

            addEdge(edgeCount, pos, box,  x0,y0,z0, x0,y1,z0);  // front-left  (Y)
            addEdge(edgeCount, pos, box,  x1,y0,z0, x1,y1,z0);  // front-right (Y)
            addEdge(edgeCount, pos, box,  x0,y0,z1, x0,y1,z1);  // back-left   (Y)
            addEdge(edgeCount, pos, box,  x1,y0,z1, x1,y1,z1);  // back-right  (Y)

            addEdge(edgeCount, pos, box,  x0,y1,z0, x0,y1,z1);  // top-left    (Z)
            addEdge(edgeCount, pos, box,  x1,y1,z0, x1,y1,z1);  // top-right   (Z)
            addEdge(edgeCount, pos, box,  x0,y0,z0, x0,y0,z1);  // bot-left    (Z)
            addEdge(edgeCount, pos, box,  x1,y0,z0, x1,y0,z1);  // bot-right   (Z)
        }

        // --- Vẽ chỉ cạnh xuất hiện đúng 1 lần ---
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        // Pass 1: bình thường (có depth test, màu đậm)
        VertexConsumer solid = context.consumers().getBuffer(RenderLayer.getLines());
        // Pass 2: xuyên block (không depth test, màu mờ)
        VertexConsumer xray  = context.consumers().getBuffer(LINES_NO_DEPTH);

        for (EdgeData ed : edgeCount.values()) {
            if (ed.count != 1) continue;
            solid.vertex(mat, ed.ax, ed.ay, ed.az).color(1f, 0.2f, 0.2f, 0.8f).normal(ed.nx, ed.ny, ed.nz);
            solid.vertex(mat, ed.bx, ed.by, ed.bz).color(1f, 0.2f, 0.2f, 0.8f).normal(ed.nx, ed.ny, ed.nz);
            xray.vertex(mat, ed.ax, ed.ay, ed.az).color(1f, 0.2f, 0.2f, 0.25f).normal(ed.nx, ed.ny, ed.nz);
            xray.vertex(mat, ed.bx, ed.by, ed.bz).color(1f, 0.2f, 0.2f, 0.25f).normal(ed.nx, ed.ny, ed.nz);
        }

        matrices.pop();
        return false;
    }

    private static void addEdge(Map<Long, EdgeData> map, BlockPos pos, Box box,
                                int ax2, int ay2, int az2,
                                int bx2, int by2, int bz2) {
        // Canonical key: điểm nhỏ hơn đứng trước
        long key = edgeKey(ax2, ay2, az2, bx2, by2, bz2);

        EdgeData ed = map.get(key);
        if (ed == null) {
            // Tọa độ thực = /2.0f
            float ax = ax2 / 2.0f, ay = ay2 / 2.0f, az = az2 / 2.0f;
            float bx = bx2 / 2.0f, by = by2 / 2.0f, bz = bz2 / 2.0f;

            // Normal xấp xỉ: hướng từ tâm block ra cạnh
            float cx = pos.getX() + (float)(box.minX + box.maxX) / 2f;
            float cy = pos.getY() + (float)(box.minY + box.maxY) / 2f;
            float cz = pos.getZ() + (float)(box.minZ + box.maxZ) / 2f;
            float mx = (ax + bx) / 2f, my = (ay + by) / 2f, mz = (az + bz) / 2f;
            float nx = mx - cx, ny = my - cy, nz = mz - cz;
            float len = (float) Math.sqrt(nx*nx + ny*ny + nz*nz);
            if (len > 0) { nx /= len; ny /= len; nz /= len; }

            map.put(key, new EdgeData(ax, ay, az, bx, by, bz, nx, ny, nz));
        } else {
            ed.count++;
        }
    }

    private static long edgeKey(int ax2, int ay2, int az2, int bx2, int by2, int bz2) {
        // Đảm bảo canonical (điểm nhỏ hơn trước) bằng cách so sánh packed
        long pa = packPoint(ax2, ay2, az2);
        long pb = packPoint(bx2, by2, bz2);
        if (pa > pb) { long tmp = pa; pa = pb; pb = tmp; }
        return pa * 1_000_000_007L + pb;
    }

    private static long packPoint(int x2, int y2, int z2) {
        // x2, y2, z2 nằm trong khoảng [-4096, 4096]*2 → offset 4096 để dương
        return ((long)(x2 + 4096) * 8193L + (y2 + 4096)) * 8193L + (z2 + 4096);
    }

    private static class EdgeData {
        float ax, ay, az, bx, by, bz, nx, ny, nz;
        int count = 1;
        EdgeData(float ax, float ay, float az, float bx, float by, float bz,
                 float nx, float ny, float nz) {
            this.ax = ax; this.ay = ay; this.az = az;
            this.bx = bx; this.by = by; this.bz = bz;
            this.nx = nx; this.ny = ny; this.nz = nz;
        }
    }
}