package com.symplex.core.geometry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BarycentricMapperTest {

    private static final int X0 = BarycentricMapper.X0;
    private static final int Y0 = BarycentricMapper.Y0;
    private static final int W  = BarycentricMapper.W;
    private static final int H  = BarycentricMapper.H;

    // Triangle vertices in pixel coordinates
    private static final int V1X = X0;          // bottom-left
    private static final int V1Y = Y0 + H;
    private static final int V2X = X0 + W;      // bottom-right
    private static final int V2Y = Y0 + H;
    private static final int V3X = X0 + W / 2;  // top
    private static final int V3Y = Y0;

    @Test
    void weights_bottomLeft_vertex_lambdaOneIsOne() {
        double[] w = BarycentricMapper.weights(V1X, V1Y);
        assertThat(w).isNotNull();
        assertThat(w[0]).isCloseTo(1.0, within(1e-12));
        assertThat(w[1]).isCloseTo(0.0, within(1e-12));
        assertThat(w[2]).isCloseTo(0.0, within(1e-12));
    }

    @Test
    void weights_bottomRight_vertex_lambdaTwoIsOne() {
        double[] w = BarycentricMapper.weights(V2X, V2Y);
        assertThat(w).isNotNull();
        assertThat(w[0]).isCloseTo(0.0, within(1e-12));
        assertThat(w[1]).isCloseTo(1.0, within(1e-12));
        assertThat(w[2]).isCloseTo(0.0, within(1e-12));
    }

    @Test
    void weights_nearTopVertex_lambdaThreeDominant() {
        // V3X = X0 + W/2.0 = 228.5 — not an integer pixel, so we test just inside the apex.
        // Pixel (228, 51) is one pixel below and one left of the exact apex.
        double[] w = BarycentricMapper.weights(228, 51);
        assertThat(w).isNotNull();
        assertThat(w[0] + w[1] + w[2]).isCloseTo(1.0, within(1e-12));
        assertThat(w[2]).isGreaterThan(0.99);  // λ3 ≈ 1 near apex
    }

    @Test
    void weights_centroid_equalThirds() {
        int cx = (V1X + V2X + V3X) / 3;
        int cy = (V1Y + V2Y + V3Y) / 3;
        double[] w = BarycentricMapper.weights(cx, cy);
        assertThat(w).isNotNull();
        assertThat(w[0] + w[1] + w[2]).isCloseTo(1.0, within(1e-12));
        assertThat(w[0]).isCloseTo(1.0 / 3, within(0.01)); // pixel rounding tolerance
        assertThat(w[1]).isCloseTo(1.0 / 3, within(0.01));
        assertThat(w[2]).isCloseTo(1.0 / 3, within(0.01));
    }

    @ParameterizedTest
    @CsvSource({
        // interior pixels — various spots
        "228, 220",
        "150, 300",
        "350, 280",
        "228, 51",   // near top
        "31, 392",   // near bottom-left
        "426, 392",  // near bottom-right
    })
    void weights_interiorPixel_sumIsOne(int px, int py) {
        double[] w = BarycentricMapper.weights(px, py);
        assertThat(w).isNotNull();
        assertThat(w[0] + w[1] + w[2]).isCloseTo(1.0, within(1e-12));
        assertThat(w[0]).isGreaterThanOrEqualTo(0.0);
        assertThat(w[1]).isGreaterThanOrEqualTo(0.0);
        assertThat(w[2]).isGreaterThanOrEqualTo(0.0);
    }

    @ParameterizedTest
    @CsvSource({
        // corners of the canvas, well outside the triangle
        "0, 0",
        "456, 0",
        "0, 456",
        "456, 456",
        // explicitly outside
        "29, 200",   // left of triangle
        "428, 200",  // right of triangle
    })
    void weights_outsideTriangle_returnsNull(int px, int py) {
        assertThat(BarycentricMapper.weights(px, py)).isNull();
        assertThat(BarycentricMapper.inside(px, py)).isFalse();
    }
}
