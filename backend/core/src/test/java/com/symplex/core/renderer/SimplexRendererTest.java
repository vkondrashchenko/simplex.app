package com.symplex.core.renderer;

import com.symplex.core.parser.Polynomial;
import com.symplex.core.parser.PolynomialParser;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class SimplexRendererTest {

    private static final String ALPHA_EQ =
            "Y=5.76*x1+7.20*x2+13.36*x3+68.16*x4" +
            "-3.66*x1*x2+32.13*x1*x3+62.8*x1*x4" +
            "+42.80*x2*x3+56.86*x2*x4+58.65*x3*x4";

    /** Builds a grid with x1=0 fixed, free vars x2,x3,x4 — the PDF control scenario. */
    private SimplexGrid buildControlGrid(int pixelStep) {
        Polynomial poly = PolynomialParser.parse(ALPHA_EQ);
        double[] xFixed = {0.0, 0.0, 0.0, 0.0};  // x1=0, rest overwritten per pixel
        int[] freeIdx = {1, 2, 3};                 // 0-based: x2,x3,x4
        double ll = 1.0;                            // 1 - x1_fixed = 1 - 0 = 1
        return SimplexGrid.sweep(poly, xFixed, freeIdx, ll, pixelStep);
    }

    @Test
    void render_producesCorrectImageSize() {
        SimplexGrid grid = buildControlGrid(4);
        RenderParams params = RenderParams.region(0, 100, 4);
        String[] names = {"x2", "x3", "x4"};
        BufferedImage img = SimplexRenderer.render(grid, params, names, 1.0);
        assertThat(img.getWidth()).isEqualTo(457);
        assertThat(img.getHeight()).isEqualTo(457);
    }

    @Test
    void render_gridHasReasonableMinMax() {
        SimplexGrid grid = buildControlGrid(4);
        assertThat(grid.min()).isLessThan(grid.max());
        // PDF shows isolines from 25 to 65 — min/max should bracket that
        assertThat(grid.min()).isLessThan(30.0);
        assertThat(grid.max()).isGreaterThan(60.0);
    }

    @Test
    void matchesIsoline_exactAtPrecision() {
        // 51.1 matches 51.1 at any precision
        assertThat(SimplexRenderer.matchesIsoline(51.1, 51.1, 1)).isTrue();
        // 51.14 rounds to 51.1 at precision=1 (511 == 511) — so it MATCHES
        assertThat(SimplexRenderer.matchesIsoline(51.14, 51.1, 1)).isTrue();
        // 51.14 rounds to 51.14 at precision=2 (5114 != 5110) — does NOT match
        assertThat(SimplexRenderer.matchesIsoline(51.14, 51.1, 2)).isFalse();
        // 25.04 rounds to 25.0 at precision=1 (250 == 250) — matches
        assertThat(SimplexRenderer.matchesIsoline(25.04, 25.0, 1)).isTrue();
        // 25.16 rounds to 25.2 at precision=1 (252 != 250) — does NOT match
        assertThat(SimplexRenderer.matchesIsoline(25.16, 25.0, 1)).isFalse();
    }

    @Test
    void render_pngBytes_nonEmpty() {
        SimplexGrid grid = buildControlGrid(5);
        RenderParams params = RenderParams.isolineRange(25, 65, 5, 1, 5);
        byte[] png = SimplexRenderer.renderPng(grid, params, new String[]{"x2", "x3", "x4"}, 1.0);
        assertThat(png).isNotEmpty();
        // PNG magic bytes
        assertThat(png[0] & 0xFF).isEqualTo(0x89);
        assertThat(png[1] & 0xFF).isEqualTo(0x50); // 'P'
        assertThat(png[2] & 0xFF).isEqualTo(0x4E); // 'N'
        assertThat(png[3] & 0xFF).isEqualTo(0x47); // 'G'
    }
}
