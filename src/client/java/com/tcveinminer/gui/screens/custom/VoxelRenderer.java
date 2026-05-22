// File 6: VoxelRenderer.java
package com.tcveinminer.gui.screens.custom;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;
import com.tcveinminer.util.ThemeColors;

public class VoxelRenderer {
    private static final int VOXEL_RANGE = 8;

    public static void drawWireframe3D(DrawContext ctx, TextRenderer textRenderer, RenderMesh mesh, String errorMsg, int screenCX, int screenCY, int areaW, float rotX, float rotY) {
        if (mesh == null || mesh.isEmpty()) {
            String msg = errorMsg != null ? "Công thức không hợp lệ" : "Không có khối nào";
            ctx.drawCenteredTextWithShadow(textRenderer, msg, screenCX, screenCY - 4, ThemeColors.TEXT_DIM);
            return;
        }

        float rxRad = (float) Math.toRadians(rotX);
        float ryRad = (float) Math.toRadians(rotY);
        float scale = areaW / 22.0f;

        // Gom tất cả dots để sort theo depth
        List<Dot3D> dots = new ArrayList<>();

        // 1. Voxels (surface voxels) - vẽ dot 2x2
        for (int[] v : mesh.voxels) {
            float[] r = rotate(v[0], v[1], v[2], rxRad, ryRad);
            float[] p = project(r[0], r[1], r[2], screenCX, screenCY, scale);
            int color = lerpColor(ThemeColors.EMERALD, ThemeColors.GOLD, getTY(v[1]));
            color = darken(color, getLightFactor(r[2]));
            dots.add(new Dot3D(p[0], p[1], r[2], color, 2));
        }

        // 2. Surface points (==) - vẽ dot 1x1
        for (float[] q : mesh.points) {
            float[] r = rotate(q[0], q[1], q[2], rxRad, ryRad);
            float[] p = project(r[0], r[1], r[2], screenCX, screenCY, scale);
            int color = lerpColor(ThemeColors.EMERALD, ThemeColors.GOLD, getTY((int) q[1]));
            color = darken(color, getLightFactor(r[2]));
            dots.add(new Dot3D(p[0], p[1], r[2], color, 1));
        }

        // 3. Line segments - vẽ dot dọc đường
        for (int[] l : mesh.lines) {
            int steps = VOXEL_RANGE * 4;
            for (int i = 0; i <= steps; i++) {
                float t = (float) i / steps;
                float lx = l[0] + t * (l[3] - l[0]);
                float ly = l[1] + t * (l[4] - l[1]);
                float lz = l[2] + t * (l[5] - l[2]);
                float[] r = rotate(lx, ly, lz, rxRad, ryRad);
                float[] p = project(r[0], r[1], r[2], screenCX, screenCY, scale);
                dots.add(new Dot3D(p[0], p[1], r[2], ThemeColors.EMERALD, 1));
            }
        }

        // Sort back-to-front để dot gần hơn đè lên dot xa hơn
        dots.sort((a, b) -> Float.compare(b.depth, a.depth));

        for (Dot3D d : dots) {
            ctx.fill((int) d.x, (int) d.y, (int) d.x + d.size, (int) d.y + d.size, d.color);
        }
    }

    private static float getTY(int y) {
        return Math.max(0, Math.min(1, (y + VOXEL_RANGE) / (float)(VOXEL_RANGE * 2)));
    }

    private static float getLightFactor(float z) {
        float tZ = Math.max(0, Math.min(1, (z + VOXEL_RANGE) / (float)(VOXEL_RANGE * 2)));
        return 0.35f + (0.65f * (1.0f - tZ));
    }

    private static float[] rotate(float x, float y, float z, float rxRad, float ryRad) {
        float cosY = (float) Math.cos(ryRad), sinY = (float) Math.sin(ryRad);
        float x1 = x * cosY + z * sinY;
        float z1 = -x * sinY + z * cosY;
        float cosX = (float) Math.cos(rxRad), sinX = (float) Math.sin(rxRad);
        float y2 = y * cosX - z1 * sinX;
        float z2 = y * sinX + z1 * cosX;
        return new float[]{x1, y2, z2};
    }

    private static float[] project(float x, float y, float z, int cx, int cy, float scale) {
        float fov = 18.0f;
        float d = fov / (fov + z);
        return new float[]{cx + x * scale * d, cy - y * scale * d, z};
    }

    private static int lerpColor(int a, int b, float t) {
        int aa = (a >> 24) & 0xFF, ra = (a >> 16) & 0xFF, ga = (a >> 8) & 0xFF, ba = a & 0xFF;
        int ab = (b >> 24) & 0xFF, rb = (b >> 16) & 0xFF, gb = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(aa + (ab - aa) * t) << 24) | ((int)(ra + (rb - ra) * t) << 16)
                | ((int)(ga + (gb - ga) * t) << 8)  | (int)(ba + (bb - ba) * t);
    }

    private static int darken(int color, float factor) {
        return ((color >> 24) & 0xFF) << 24
                | (int)(((color >> 16) & 0xFF) * factor) << 16
                | (int)(((color >>  8) & 0xFF) * factor) << 8
                | (int)((color & 0xFF) * factor);
    }

    private static class Dot3D {
        float x, y, depth; int color, size;
        Dot3D(float x, float y, float depth, int color, int size) {
            this.x = x; this.y = y; this.depth = depth; this.color = color; this.size = size;
        }
    }
}