// File 4: ShapeAnalyzer.java
package com.tcveinminer.gui.screens.custom;

import com.tcveinminer.util.ExpressionEvaluator;

/**
 * Classifies an expression into a geometric ShapeType by probing the evaluator
 * without scanning voxels. Uses axis-aligned far-field probing to detect
 * boundedness, and root-operator analysis for surface/line detection.
 *
 * Classification logic:
 *   == at root                          → SURFACE
 *   && at root with multiple equations  → LINE
 *   inequality, bounded all 6 axes      → FINITE_VOLUME
 *   inequality, unbounded any axis      → INFINITE  (rejected, not rendered)
 *   no true point found anywhere        → EMPTY
 */
public class ShapeAnalyzer {

    // Distance used for far-field "unbounded?" probe. Must be >> RENDER_RADIUS.
    private static final double PROBE_FAR = 1000.0;

    // Sample points used to find any true point when origin is false.
    private static final double[] PROBE_OFFSETS = {-5, -3, -2, -1, 0, 1, 2, 3, 5};

    private final ExpressionEvaluator evaluator;
    private final String rawExpr;
    private ShapeType type = ShapeType.EMPTY;
    private String error = null;

    public ShapeAnalyzer(ExpressionEvaluator evaluator, String rawExpr) {
        this.evaluator = evaluator;
        this.rawExpr = rawExpr;
    }

    public void analyze() {
        if (!evaluator.isValid()) {
            error = evaluator.getError();
            return;
        }

        String rootOp = evaluator.getRootOperator();

        // ── 1. Surface: root operator is == ────────────────────────────────
        if ("==".equals(rootOp)) {
            type = ShapeType.SURFACE;
            return;
        }

        // ── 2. Line: && at root with multiple == sub-conditions ────────────
        if ("&&".equals(rootOp) && evaluator.hasMultipleEquations() && rawExpr.contains("==")) {
            type = ShapeType.LINE;
            return;
        }

        // ── 3. Inequality shapes: check boundedness per axis ────────────────
        // Find a seed point that evaluates to true (needed for direction probing).
        double[] seed = findTruePoint();
        if (seed == null) {
            // No true point exists anywhere in our probe grid → empty set.
            type = ShapeType.EMPTY;
            return;
        }

        // For each of the 6 axis directions, check whether the true region
        // extends to infinity from the seed point.
        boolean infiniteInAnyAxis =
                isUnbounded(seed,  1,  0,  0) ||
                        isUnbounded(seed, -1,  0,  0) ||
                        isUnbounded(seed,  0,  1,  0) ||
                        isUnbounded(seed,  0, -1,  0) ||
                        isUnbounded(seed,  0,  0,  1) ||
                        isUnbounded(seed,  0,  0, -1);

        if (infiniteInAnyAxis) {
            type = ShapeType.INFINITE;
            error = "Biểu thức mô tả hình vô hạn (ví dụ: x >= 0). "
                    + "Hãy thêm ràng buộc để giới hạn vùng (ví dụ: x >= 0 && x <= 4).";
            return;
        }

        type = ShapeType.FINITE_VOLUME;
    }

    /**
     * Probe from a known true seed point in direction (dx,dy,dz).
     * Returns true if the expression is still true at PROBE_FAR distance,
     * meaning the shape extends to infinity in that direction.
     *
     * We probe at multiple distances to avoid accidentally hitting a false
     * island far from the origin (e.g. periodic functions).
     */
    private boolean isUnbounded(double[] seed, double dx, double dy, double dz) {
        // Require true at BOTH medium and far distance → reduces false positives
        // from expressions like sin(x) > 0 that alternate.
        double mid = PROBE_FAR / 2.0;
        boolean atMid = evaluator.evalBoolean(
                seed[0] + dx * mid,
                seed[1] + dy * mid,
                seed[2] + dz * mid);
        boolean atFar = evaluator.evalBoolean(
                seed[0] + dx * PROBE_FAR,
                seed[1] + dy * PROBE_FAR,
                seed[2] + dz * PROBE_FAR);
        return atMid && atFar;
    }

    /**
     * Finds any (x,y,z) point where the expression evaluates to true.
     * Searches a small grid around the origin. Returns null if none found.
     */
    private double[] findTruePoint() {
        for (double x : PROBE_OFFSETS)
            for (double y : PROBE_OFFSETS)
                for (double z : PROBE_OFFSETS)
                    if (evaluator.evalBoolean(x, y, z))
                        return new double[]{x, y, z};
        return null;
    }

    public ShapeType getType() { return type; }

    /** Non-null if analyze() determined the expression is invalid or infinite. */
    public String getError() { return error; }

    /** True when the shape can actually be rendered. */
    public boolean isRenderable() {
        return error == null && type != ShapeType.EMPTY;
    }
}