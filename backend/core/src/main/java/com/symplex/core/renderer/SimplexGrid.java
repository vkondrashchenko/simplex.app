package com.symplex.core.renderer;

import com.symplex.core.evaluator.PolynomialEvaluator;
import com.symplex.core.geometry.BarycentricMapper;
import com.symplex.core.parser.Polynomial;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Result of evaluating a polynomial over every pixel of the simplex triangle.
 *
 * <p>Port of Delphi {@code TFunc.F} (the pixel-value grid) plus {@code GetFunction}'s
 * sweep logic, but with:
 * <ul>
 *   <li>Analytical barycentric mapping instead of Crosslin line-intersection.</li>
 *   <li>Parallel outer loop via {@code IntStream.range().parallel()} — safe because
 *       each column {@code i} writes only to {@code values[i][]}, no shared mutable state.</li>
 *   <li>Sentinel {@code Float.NaN} replaces the Delphi {@code -1} for "outside triangle".</li>
 * </ul>
 *
 * @param values  {@code values[i][j]} — Y at pixel {@code (x0+i, y0+j)};
 *                {@code Float.NaN} if outside the triangle.
 *                Dimensions: {@code [W+1][H+1]}.
 * @param min     minimum Y value observed inside the triangle
 * @param max     maximum Y value observed inside the triangle
 */
public record SimplexGrid(float[][] values, double min, double max) {

    /**
     * Sweeps the simplex triangle and returns a populated {@link SimplexGrid}.
     *
     * @param polynomial parsed polynomial to evaluate
     * @param xFixed     0-indexed variable array with fixed values pre-set;
     *                   positions for free variables are overwritten per pixel
     * @param freeIdx    0-indexed positions of the three free variables in {@code xFixed}
     *                   (i.e. {@code xFixed[freeIdx[k]]} is set per-pixel)
     * @param ll         {@code 1 - Σ x_fixed} — total weight available for the three free axes
     * @param pixelStep  stride (1 = every pixel)
     */
    public static SimplexGrid sweep(
            Polynomial polynomial,
            double[] xFixed,
            int[] freeIdx,
            double ll,
            int pixelStep
    ) {
        final int W = BarycentricMapper.W;
        final int H = BarycentricMapper.H;
        float[][] values = new float[W + 1][H + 1];
        for (float[] col : values) Arrays.fill(col, Float.NaN);

        // Parallel per-column; each column has its own x[] copy to avoid contention.
        double[] minMax = IntStream.rangeClosed(0, W)
                .filter(i -> i % pixelStep == 0)
                .parallel()
                .mapToObj(i -> {
                    double[] x = xFixed.clone();
                    double localMin = Double.MAX_VALUE;
                    double localMax = -Double.MAX_VALUE;
                    for (int j = 0; j <= H; j += pixelStep) {
                        double[] w = BarycentricMapper.weights(BarycentricMapper.X0 + i, BarycentricMapper.Y0 + j);
                        if (w == null) continue;
                        x[freeIdx[0]] = ll * w[0];
                        x[freeIdx[1]] = ll * w[1];
                        x[freeIdx[2]] = ll * w[2];
                        double y = PolynomialEvaluator.evaluate(polynomial, x);
                        values[i][j] = (float) y;
                        if (y < localMin) localMin = y;
                        if (y > localMax) localMax = y;
                    }
                    return new double[]{localMin, localMax};
                })
                .reduce(
                        new double[]{Double.MAX_VALUE, -Double.MAX_VALUE},
                        (a, b) -> new double[]{Math.min(a[0], b[0]), Math.max(a[1], b[1])}
                );

        return new SimplexGrid(values, minMax[0], minMax[1]);
    }
}
