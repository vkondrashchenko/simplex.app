package com.symplex.core.geometry;

/**
 * Maps pixel coordinates inside the simplex triangle to barycentric weights.
 *
 * <p>Replaces the 200-line Delphi {@code Crosslin} unit with the standard
 * analytical barycentric formula — exact, O(1), no line-intersection approximation.
 *
 * <p>Triangle layout (matches the original Delphi canvas constants):
 * <pre>
 *            v3 = (x0 + W/2,  y0)         top    — 3rd free variable
 *           / \
 *          /   \
 *   v1 = (x0, y0+H)  ---  v2 = (x0+W, y0+H)
 *   bottom-left             bottom-right
 *   1st free variable       2nd free variable
 * </pre>
 * Constants: {@code x0=30, y0=50, W=397, H=343}; image canvas is 457×457 px.
 */
public final class BarycentricMapper {

    // --- Simplex geometry constants (must match SimplexRenderer) ---
    public static final int X0 = 30;
    public static final int Y0 = 50;
    public static final int W  = 397;
    public static final int H  = 343;

    // Triangle vertices in pixel space
    private static final double V1X = X0;
    private static final double V1Y = Y0 + H;   // bottom-left
    private static final double V2X = X0 + W;
    private static final double V2Y = Y0 + H;   // bottom-right
    private static final double V3X = X0 + W / 2.0;
    private static final double V3Y = Y0;       // top

    // Pre-computed denominator for barycentric formula (constant for this triangle)
    private static final double DET =
            (V2Y - V3Y) * (V1X - V3X) + (V3X - V2X) * (V1Y - V3Y);

    private BarycentricMapper() {}

    /**
     * Returns barycentric weights {@code [λ1, λ2, λ3]} for pixel {@code (px, py)}.
     * Sum is always 1.0 for points on or inside the triangle.
     * Returns {@code null} if the point is strictly outside the triangle.
     */
    public static double[] weights(int px, int py) {
        double dx = px - V3X;
        double dy = py - V3Y;

        double l1 = ((V2Y - V3Y) * dx + (V3X - V2X) * dy) / DET;
        double l2 = ((V3Y - V1Y) * dx + (V1X - V3X) * dy) / DET;
        double l3 = 1.0 - l1 - l2;

        if (l1 < 0.0 || l2 < 0.0 || l3 < 0.0) return null;  // outside
        return new double[]{l1, l2, l3};
    }

    /** Returns {@code true} if pixel {@code (px, py)} is inside (or on the boundary of) the triangle. */
    public static boolean inside(int px, int py) {
        return weights(px, py) != null;
    }

    /**
     * Width of the image canvas (same as Delphi {@code Scr.Width}).
     */
    public static final int CANVAS_SIZE = 457;
}
