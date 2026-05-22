// File 5: GeometryGenerator.java
package com.tcveinminer.gui.screens.custom;

import com.tcveinminer.util.ExpressionEvaluator;

/**
 * Converts a classified shape into a RenderMesh within the preview volume.
 *
 * Rendering strategies per ShapeType:
 *
 *   FINITE_VOLUME  → integer-grid voxels, exposed-face culled (only surface blocks).
 *                    This is the standard Minecraft voxel look.
 *
 *   SURFACE        → floating-point sample grid (step=0.5). A cell is added when
 *                    the boolean value changes across any of its three forward edges
 *                    (sign-change detection). Produces a thin shell of point markers.
 *
 *   LINE           → parametric trace along the dominant free axis (or diagonal).
 *                    Adds point markers at step=0.5 along the curve.
 *
 *   INFINITE       → returns empty mesh; caller should show ShapeAnalyzer.getError().
 *   EMPTY          → returns empty mesh.
 */
public class GeometryGenerator {

    /** Half-size of the preview cube. Voxels are sampled in [-R, R] on each axis. */
    private static final int RENDER_RADIUS = 8;

    /** Step size for surface/line floating-point sampling. */
    private static final float SURFACE_STEP = 0.5f;

    public static RenderMesh generate(ShapeAnalyzer analyzer, ExpressionEvaluator evaluator) {
        RenderMesh mesh = new RenderMesh();
        if (!analyzer.isRenderable()) return mesh;  // INFINITE or EMPTY

        switch (analyzer.getType()) {
            case FINITE_VOLUME:
                generateVoxelVolume(mesh, evaluator);
                break;
            case SURFACE:
                generateSurface(mesh, evaluator);
                break;
            case LINE:
                generateLine(mesh, evaluator);
                break;
            // INFINITE / EMPTY already handled above
        }
        return mesh;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FINITE_VOLUME: integer voxel grid, exposed-face culling
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds only the voxels on the outer shell of the volume (at least one of the
     * 6 face-neighbors is outside the shape). Interior voxels are skipped because
     * they would never be visible in a Minecraft-style render anyway.
     */
    private static void generateVoxelVolume(RenderMesh mesh, ExpressionEvaluator evaluator) {
        for (int x = -RENDER_RADIUS; x <= RENDER_RADIUS; x++) {
            for (int y = -RENDER_RADIUS; y <= RENDER_RADIUS; y++) {
                for (int z = -RENDER_RADIUS; z <= RENDER_RADIUS; z++) {
                    if (!evaluator.evalBoolean(x, y, z)) continue;

                    // Keep only exposed (surface) voxels.
                    boolean exposed =
                            !evaluator.evalBoolean(x + 1, y, z) || !evaluator.evalBoolean(x - 1, y, z) ||
                                    !evaluator.evalBoolean(x, y + 1, z) || !evaluator.evalBoolean(x, y - 1, z) ||
                                    !evaluator.evalBoolean(x, y, z + 1) || !evaluator.evalBoolean(x, y, z - 1);
                    if (exposed) mesh.addVoxel(x, y, z);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SURFACE: floating-point sign-change detection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Samples the expression at a fine grid (step = SURFACE_STEP).
     * A cell corner is added to the mesh when the boolean value differs between
     * that corner and any of its three forward neighbors (+x, +y, or +z).
     * This is a simplified marching-cubes edge test that reliably captures thin
     * surfaces like y==1 or x²+z²==4 without requiring the expression to be
     * exactly true at a grid point.
     */
    private static void generateSurface(RenderMesh mesh, ExpressionEvaluator evaluator) {
        float R = RENDER_RADIUS;
        for (float x = -R; x <= R; x += SURFACE_STEP) {
            for (float y = -R; y <= R; y += SURFACE_STEP) {
                for (float z = -R; z <= R; z += SURFACE_STEP) {
                    boolean c  = evaluator.evalBoolean(x,                  y,                  z);
                    boolean px = evaluator.evalBoolean(x + SURFACE_STEP,   y,                  z);
                    boolean py = evaluator.evalBoolean(x,                  y + SURFACE_STEP,   z);
                    boolean pz = evaluator.evalBoolean(x,                  y,                  z + SURFACE_STEP);

                    // Sign change on any edge → this cell straddles the surface.
                    if (c != px || c != py || c != pz) {
                        mesh.addPoint(x, y, z);
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LINE: parametric trace
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * For a LINE (intersection of surfaces), we identify the single free axis
     * (the one not constrained by the equations) and sweep along it.
     *
     * Axis selection priority:
     *   - If only one variable is free (the other two are locked by equations),
     *     sweep along that axis, fixing the constrained vars to their probe values.
     *   - If all three are used, fall back to diagonal sweep (t,t,t) — works for
     *     expressions like x==y && y==z.
     *   - If two are unused (e.g. a degenerate constant line), sweep the Z axis.
     */
    private static void generateLine(RenderMesh mesh, ExpressionEvaluator evaluator) {
        boolean ux = evaluator.usesX(), uy = evaluator.usesY(), uz = evaluator.usesZ();

        if (!ux && !uy) {
            // Free axis: Z (e.g. expression is a constant true in Z direction)
            sweepAxis(mesh, evaluator, 0, 1, 0, 0, 0, 0);
        } else if (!ux && !uz) {
            // Free axis: Y
            sweepAxis(mesh, evaluator, 0, 0, 1, 0, 0, 0);
        } else if (!uy && !uz) {
            // Free axis: X
            sweepAxis(mesh, evaluator, 1, 0, 0, 0, 0, 0);
        } else {
            // All three axes used → try diagonal sweep; add points where true.
            for (float t = -RENDER_RADIUS; t <= RENDER_RADIUS; t += SURFACE_STEP) {
                if (evaluator.evalBoolean(t, t, t)) mesh.addPoint(t, t, t);
            }
        }
    }

    /**
     * Sweeps along the axis defined by (axX, axY, axZ) unit vector.
     * The two fixed coordinates are determined by probing the grid for a true seed
     * in the plane orthogonal to the sweep axis, then held constant.
     *
     * @param axX,axY,axZ  Unit vector of the sweep axis (exactly one should be 1).
     * @param fixX,fixY,fixZ  Initial guess for the fixed coordinates (usually 0).
     */
    private static void sweepAxis(RenderMesh mesh, ExpressionEvaluator evaluator,
                                  int axX, int axY, int axZ,
                                  float fixX, float fixY, float fixZ) {
        // Find a fixed-plane seed where expression could be true
        // (needed for lines like x==3 && z==2, which aren't true at origin)
        outer:
        for (int a = -RENDER_RADIUS; a <= RENDER_RADIUS; a++) {
            for (int b = -RENDER_RADIUS; b <= RENDER_RADIUS; b++) {
                // Map (a,b) to the two non-sweep axes
                float cx = axX == 0 ? (axY == 0 ? fixX : a) : fixX;
                float cy = axY == 0 ? (axX == 0 ? a    : b) : fixY;
                float cz = axZ == 0 ? b : fixZ;
                // Only care about the fixed-coordinate plane (sweep coord = 0)
                if (evaluator.evalBoolean(cx, cy, cz)) {
                    fixX = cx; fixY = cy; fixZ = cz;
                    break outer;
                }
            }
        }

        for (float t = -RENDER_RADIUS; t <= RENDER_RADIUS; t += SURFACE_STEP) {
            float px = fixX + axX * t;
            float py = fixY + axY * t;
            float pz = fixZ + axZ * t;
            if (evaluator.evalBoolean(px, py, pz)) mesh.addPoint(px, py, pz);
        }
    }
}