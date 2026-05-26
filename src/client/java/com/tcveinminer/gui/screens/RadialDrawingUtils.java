package com.tcveinminer.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

public class RadialDrawingUtils {

    private static final int SEGMENTS = 64;

    public static void fillArc(BufferBuilder buf, Matrix4f mat, float innerR, float outerR, float startRad, float endRad, int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        // Normalize delta to positive angle in (0, 2PI]
        float delta = endRad - startRad;
        while (delta <= 0f) delta += (float)(2 * Math.PI);
        if (delta <= 1e-6f) return; // nothing to draw

        int segs = Math.max(4, (int) (SEGMENTS * (delta) / (2 * Math.PI)));
        float step = delta / segs;

        for (int i = 0; i < segs; i++) {
            float a0 = startRad + i * step;
            float a1 = a0 + step;
            float cos0 = (float) Math.cos(a0), sin0 = (float) Math.sin(a0);
            float cos1 = (float) Math.cos(a1), sin1 = (float) Math.sin(a1);

            try {
                buf.vertex(mat, cos0 * innerR, sin0 * innerR, 0).color(r, g, b, a);
                buf.vertex(mat, cos0 * outerR, sin0 * outerR, 0).color(r, g, b, a);
                buf.vertex(mat, cos1 * outerR, sin1 * outerR, 0).color(r, g, b, a);

                buf.vertex(mat, cos0 * innerR, sin0 * innerR, 0).color(r, g, b, a);
                buf.vertex(mat, cos1 * outerR, sin1 * outerR, 0).color(r, g, b, a);
                buf.vertex(mat, cos1 * innerR, sin1 * innerR, 0).color(r, g, b, a);
            } catch (RuntimeException ex) {
                throw new RuntimeException("RadialDrawingUtils.fillArc: BufferBuilder likely not begun or wrong vertex format. Call Tessellator.getInstance().begin(...) before drawing.", ex);
            }
        }
    }

    public static void fillCircle(BufferBuilder buf, Matrix4f mat, float radius, int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        float step = (float) (2 * Math.PI / SEGMENTS);
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = i * step, a1 = a0 + step;
            buf.vertex(mat, 0, 0, 0).color(r, g, b, a);
            buf.vertex(mat, (float) Math.cos(a0) * radius, (float) Math.sin(a0) * radius, 0).color(r, g, b, a);
            buf.vertex(mat, (float) Math.cos(a1) * radius, (float) Math.sin(a1) * radius, 0).color(r, g, b, a);
        }
    }

    public static void strokeArc(BufferBuilder buf, Matrix4f mat, float innerR, float outerR, float startRad, float endRad, float w, int color) {
        strokeRing(buf, mat, outerR - w, outerR, startRad, endRad, color);
        strokeRing(buf, mat, innerR, innerR + w, startRad, endRad, color);
        strokeLine(buf, mat, innerR, outerR, startRad, w, color);
        strokeLine(buf, mat, innerR, outerR, endRad, w, color);
    }

    public static void strokeRing(BufferBuilder buf, Matrix4f mat, float r1, float r2, float startRad, float endRad, int color) {
        // Normalize delta angle and compute segments from positive delta
        float delta = endRad - startRad;
        while (delta <= 0f) delta += (float)(2 * Math.PI);
        if (delta <= 1e-6f) return;

        int segs = Math.max(4, (int) (SEGMENTS * (delta) / (2 * Math.PI)));
        float step = delta / segs;
        float a = (color >> 24 & 0xFF) / 255f, r = (color >> 16 & 0xFF) / 255f,
                g = (color >> 8 & 0xFF) / 255f, b = (color & 0xFF) / 255f;

        for (int i = 0; i < segs; i++) {
            float a0 = startRad + i * step, a1 = a0 + step;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

            try {
                buf.vertex(mat, c0 * r1, s0 * r1, 0).color(r, g, b, a);
                buf.vertex(mat, c0 * r2, s0 * r2, 0).color(r, g, b, a);
                buf.vertex(mat, c1 * r2, s1 * r2, 0).color(r, g, b, a);

                buf.vertex(mat, c0 * r1, s0 * r1, 0).color(r, g, b, a);
                buf.vertex(mat, c1 * r2, s1 * r2, 0).color(r, g, b, a);
                buf.vertex(mat, c1 * r1, s1 * r1, 0).color(r, g, b, a);
            } catch (RuntimeException ex) {
                throw new RuntimeException("RadialDrawingUtils.strokeRing: BufferBuilder likely not begun or wrong vertex format.", ex);
            }
        }
    }

    public static void strokeLine(BufferBuilder buf, Matrix4f mat, float r1, float r2, float angle, float w, int color) {
        float a = (color >> 24 & 0xFF) / 255f, r = (color >> 16 & 0xFF) / 255f,
                g = (color >> 8 & 0xFF) / 255f, b = (color & 0xFF) / 255f;
        float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
        float px = -sin * (w / 2), py = cos * (w / 2);

        float x0 = cos * r1, y0 = sin * r1;
        float x1 = cos * r2, y1 = sin * r2;

        buf.vertex(mat, x0 - px, y0 - py, 0).color(r, g, b, a);
        buf.vertex(mat, x0 + px, y0 + py, 0).color(r, g, b, a);
        buf.vertex(mat, x1 + px, y1 + py, 0).color(r, g, b, a);

        buf.vertex(mat, x0 - px, y0 - py, 0).color(r, g, b, a);
        buf.vertex(mat, x1 + px, y1 + py, 0).color(r, g, b, a);
        buf.vertex(mat, x1 - px, y1 - py, 0).color(r, g, b, a);
    }
}