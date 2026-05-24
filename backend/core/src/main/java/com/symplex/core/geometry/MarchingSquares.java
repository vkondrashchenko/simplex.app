package com.symplex.core.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Marching-squares contour extraction over a sparse {@code float[][]} scalar grid.
 *
 * <p>The grid is produced by {@link com.symplex.core.renderer.SimplexGrid#sweep} with a
 * {@code pixelStep} stride; cells outside the triangle carry {@code Float.NaN} and are
 * skipped, so the triangle boundary acts as a natural clip.
 *
 * <p>Ambiguous cases 5 and 10 are resolved by consistently splitting into two segments
 * (no saddle-point lookup needed for visual quality at this scale).
 */
public final class MarchingSquares {

    // Edge indices within one marching-squares cell
    private static final int LEFT = 0, BOTTOM = 1, RIGHT = 2, TOP = 3;

    /**
     * Lookup table: for each 4-bit case index {@code (TL<<3)|(TR<<2)|(BR<<1)|(BL<<0)},
     * a flat list of edge-index pairs. Each consecutive pair forms one line segment.
     * Ambiguous cases (5, 10) carry four entries = two segments.
     */
    private static final int[][] EDGE_TABLE = {
        {},                 // 0: 0000 — no crossing
        {LEFT, BOTTOM},     // 1: 0001 BL above
        {BOTTOM, RIGHT},    // 2: 0010 BR above
        {LEFT, RIGHT},      // 3: 0011 BL+BR above
        {RIGHT, TOP},       // 4: 0100 TR above
        {LEFT, BOTTOM, RIGHT, TOP},  // 5: 0101 BL+TR (ambiguous → two segments)
        {BOTTOM, TOP},      // 6: 0110 BR+TR above
        {LEFT, TOP},        // 7: 0111 BL+BR+TR above (TL below)
        {TOP, LEFT},        // 8: 1000 TL above
        {TOP, BOTTOM},      // 9: 1001 BL+TL above
        {LEFT, TOP, BOTTOM, RIGHT},  // 10: 1010 BR+TL (ambiguous → two segments)
        {TOP, RIGHT},       // 11: 1011 BL+BR+TL above (TR below)
        {LEFT, RIGHT},      // 12: 1100 TR+TL above
        {BOTTOM, RIGHT},    // 13: 1101 BL+TR+TL above (BR below)
        {LEFT, BOTTOM},     // 14: 1110 BR+TR+TL above (BL below)
        {},                 // 15: 1111 — no crossing
    };

    private MarchingSquares() {}

    /**
     * Extracts contour line segments from {@code grid} at {@code threshold}.
     *
     * <p>Iterates over every {@code step × step} cell. Cells where any corner is
     * {@code Float.NaN} are skipped so the triangle boundary clips naturally.
     *
     * @param grid      2-D scalar field {@code grid[col][row]}; {@code Float.NaN} = outside domain
     * @param threshold isosurface value to trace
     * @param step      the sampling stride used when the grid was populated (≥1)
     * @return list of segments; each entry is {@code [x1, y1, x2, y2]} in grid-pixel coordinates
     *         (i.e. column/row indices into {@code grid}), ready to receive the X0/Y0 SVG offset
     */
    public static List<double[]> extractSegments(float[][] grid, double threshold, int step) {
        int maxCol = grid.length - 1;
        int maxRow = grid[0].length - 1;
        List<double[]> segments = new ArrayList<>();

        for (int i = 0; i + step <= maxCol; i += step) {
            for (int j = 0; j + step <= maxRow; j += step) {
                float tl = grid[i][j];
                float tr = grid[i + step][j];
                float bl = grid[i][j + step];
                float br = grid[i + step][j + step];

                if (Float.isNaN(tl) || Float.isNaN(tr) || Float.isNaN(bl) || Float.isNaN(br)) continue;

                int caseIndex = ((tl > threshold) ? 8 : 0)
                              | ((tr > threshold) ? 4 : 0)
                              | ((br > threshold) ? 2 : 0)
                              | ((bl > threshold) ? 1 : 0);

                int[] edges = EDGE_TABLE[caseIndex];
                for (int k = 0; k < edges.length; k += 2) {
                    double[] p1 = edgePoint(i, j, step, edges[k],     tl, tr, bl, br, threshold);
                    double[] p2 = edgePoint(i, j, step, edges[k + 1], tl, tr, bl, br, threshold);
                    segments.add(new double[]{p1[0], p1[1], p2[0], p2[1]});
                }
            }
        }
        return segments;
    }

    /**
     * Linearly interpolates the crossing point on one edge of a {@code step × step} cell
     * whose top-left corner is at grid position {@code (i, j)}.
     */
    private static double[] edgePoint(int i, int j, int step, int edge,
                                       float tl, float tr, float bl, float br,
                                       double threshold) {
        return switch (edge) {
            case LEFT -> {
                // TL (i, j) → BL (i, j+step)
                double t = (tl == bl) ? 0.5 : (threshold - tl) / (bl - tl);
                yield new double[]{i, j + t * step};
            }
            case BOTTOM -> {
                // BL (i, j+step) → BR (i+step, j+step)
                double t = (bl == br) ? 0.5 : (threshold - bl) / (br - bl);
                yield new double[]{i + t * step, j + step};
            }
            case RIGHT -> {
                // TR (i+step, j) → BR (i+step, j+step)
                double t = (tr == br) ? 0.5 : (threshold - tr) / (br - tr);
                yield new double[]{i + step, j + t * step};
            }
            case TOP -> {
                // TL (i, j) → TR (i+step, j)
                double t = (tl == tr) ? 0.5 : (threshold - tl) / (tr - tl);
                yield new double[]{i + t * step, j};
            }
            default -> throw new IllegalArgumentException("Unknown edge index: " + edge);
        };
    }
}
