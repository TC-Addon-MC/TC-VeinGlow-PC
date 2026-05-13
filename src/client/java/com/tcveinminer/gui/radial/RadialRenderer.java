package com.tcveinminer.gui.radial;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

public class RadialRenderer {

    public static void drawSegment(
            Matrix4f mat,
            VertexConsumer buf,
            int x, int y,
            float inR, float outR,
            float startDeg, float endDeg,
            int segments,
            int color
    ) {
        float sRad = RadialMath.toRad(startDeg);
        float eRad = RadialMath.toRad(endDeg);
        float step = (eRad - sRad) / segments;

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        for (int i = 0; i < segments; i++) {
            float a1 = sRad + i * step;
            float a2 = sRad + (i + 1) * step;

            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float c2 = (float) Math.cos(a2);
            float s2 = (float) Math.sin(a2);

            buf.vertex(mat, x + c1 * inR,  y + s1 * inR,  0.01f).color(r, g, b, a);
            buf.vertex(mat, x + c1 * outR, y + s1 * outR, 0.01f).color(r, g, b, a);
            buf.vertex(mat, x + c2 * outR, y + s2 * outR, 0.01f).color(r, g, b, a);
            buf.vertex(mat, x + c2 * inR,  y + s2 * inR,  0.01f).color(r, g, b, a);
        }
    }
}