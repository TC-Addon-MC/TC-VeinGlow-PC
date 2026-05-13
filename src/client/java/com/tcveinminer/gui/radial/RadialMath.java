package com.tcveinminer.gui.radial;

public class RadialMath {

    public static float angleStep(int n) {
        return 360f / n;
    }

    public static float midAngle(int index, float angleStep) {
        return -90f + index * angleStep + angleStep / 2f;
    }

    public static float toRad(float deg) {
        return (float) Math.toRadians(deg);
    }

    public static int[] centerPoint(int cx, int cy, float r, float angleDeg) {
        float rad = toRad(angleDeg);
        return new int[] {
                cx + (int)(Math.cos(rad) * r),
                cy + (int)(Math.sin(rad) * r)
        };
    }

    public static int getHoveredIndex(int mx, int my, int cx, int cy, float innerR, float outerR, int count) {
        float dx = mx - cx;
        float dy = my - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < innerR) return -2;
        if (dist > outerR) return -1;

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx)) + 90f;
        if (angle < 0) angle += 360f;

        return (int) (angle / (360f / count)) % count;
    }
}