package com.symplex.core.renderer;

import com.symplex.core.geometry.BarycentricMapper;
import com.symplex.core.geometry.MarchingSquares;

import java.util.List;
import java.util.Locale;

/**
 * Renders a {@link SimplexGrid} as an SVG string.
 *
 * <p>Uses {@link MarchingSquares} for isoline modes (exact sub-pixel contour lines via
 * simple threshold crossing) and scan-line rects for REGION mode. Returns a complete
 * {@code <svg>} document with {@code viewBox="0 0 457 457" width="100%" height="100%"}
 * so the browser can scale it to any container size without quality loss.
 *
 * <p>The {@code precision} render parameter controls SVG {@code stroke-width} inversely:
 * {@code strokeWidth = max(0.5, 3.5 - precision)}, mapping the slider range [0, 3] to
 * stroke widths [3.5, 0.5]. Lower precision (coarse) → thick lines; higher precision (fine) → thin lines.
 */
public final class SvgRenderer {

    private static final String BG_COLOR       = "#323232";
    private static final String INTERIOR_COLOR = "#ffff00";
    private static final String REGION_COLOR   = "#ff4444";

    private static final String[] ISO_PALETTE = {
        "#ff0000", "#ff8000", "#ffff00", "#00c800", "#00ffff",
        "#0080ff", "#a000ff", "#ff00ff", "#ff4040", "#40ff80",
        "#ffc800", "#00c8ff",
    };

    private SvgRenderer() {}

    /**
     * Builds an SVG document for the given grid and render parameters.
     *
     * @param grid         sweep result from {@link SimplexGrid#sweep}
     * @param params       render configuration
     * @param freeVarNames labels for the three simplex vertices (e.g. ["x2", "x3", "x4"])
     * @param ll           total simplex weight (displayed in vertex labels)
     * @return complete SVG document as a string
     */
    public static String render(
            SimplexGrid grid,
            RenderParams params,
            String[] freeVarNames,
            double ll
    ) {
        final int SIZE = BarycentricMapper.CANVAS_SIZE;
        final int X0   = BarycentricMapper.X0;
        final int Y0   = BarycentricMapper.Y0;
        final int W    = BarycentricMapper.W;
        final int H    = BarycentricMapper.H;

        // Triangle vertex pixel coordinates
        double v3x = X0 + W / 2.0; double v3y = Y0;       // top
        double v1x = X0;            double v1y = Y0 + H;   // bottom-left
        double v2x = X0 + W;        double v2y = Y0 + H;   // bottom-right

        double strokeWidth = Math.max(0.5, 3.5 - params.precision());
        int    step        = params.pixelStep();

        StringBuilder sb = new StringBuilder(65_536);
        sb.append(String.format(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\" width=\"100%%\" height=\"100%%\">%n",
            SIZE, SIZE));

        // Background
        sb.append(String.format(
            "  <rect width=\"%d\" height=\"%d\" fill=\"%s\"/>%n",
            SIZE, SIZE, BG_COLOR));

        // Triangle: yellow fill + white outline (drawn before isolines so lines appear on top)
        sb.append(String.format(
            "  <polygon points=\"%.1f,%.1f %.1f,%.1f %.1f,%.1f\" fill=\"%s\" fill-opacity=\"0.9\" stroke=\"white\" stroke-width=\"1\"/>%n",
            v3x, v3y, v1x, v1y, v2x, v2y, INTERIOR_COLOR));

        // Mode-specific layer
        float[][] values = grid.values();
        switch (params.mode()) {
            case REGION -> {
                String d = buildRegionPath(values, params, X0, Y0, W, H, step);
                if (!d.isEmpty()) {
                    sb.append(String.format(
                        "  <path d=\"%s\" fill=\"%s\" fill-opacity=\"0.85\" stroke=\"none\"/>%n",
                        d, REGION_COLOR));
                }
            }
            case ISOLINE_SINGLE -> {
                List<double[]> segs = MarchingSquares.extractSegments(values, params.isoValue(), step);
                String d = toPathData(segs, X0, Y0);
                if (!d.isEmpty()) {
                    sb.append(String.format(
                        "  <path d=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"%.2f\" stroke-linecap=\"round\"/>%n",
                        d, ISO_PALETTE[0], strokeWidth));
                }
            }
            case ISOLINE_RANGE -> {
                int colorIdx = 0;
                for (double v = params.isoFrom(); v <= params.isoTo() + 1e-12; v += params.isoStep()) {
                    List<double[]> segs = MarchingSquares.extractSegments(values, v, step);
                    String d = toPathData(segs, X0, Y0);
                    if (!d.isEmpty()) {
                        sb.append(String.format(
                            "  <path d=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"%.2f\" stroke-linecap=\"round\"/>%n",
                            d, ISO_PALETTE[colorIdx % ISO_PALETTE.length], strokeWidth));
                    }
                    colorIdx++;
                }
            }
        }

        // Vertex labels: freeVarNames[2]=top, [0]=bottom-left, [1]=bottom-right
        String llStr = String.format(Locale.US, "=%.3g", ll);
        sb.append(String.format(Locale.US,
            "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"11\" fill=\"white\">%s%s</text>%n",
            v3x, v3y - 8.0, escape(freeVarNames[2]), llStr));
        sb.append(String.format(Locale.US,
            "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"start\" font-family=\"sans-serif\" font-size=\"11\" fill=\"white\">%s%s</text>%n",
            v1x, v1y + 14.0, escape(freeVarNames[0]), llStr));
        sb.append(String.format(Locale.US,
            "  <text x=\"%.1f\" y=\"%.1f\" text-anchor=\"end\" font-family=\"sans-serif\" font-size=\"11\" fill=\"white\">%s%s</text>%n",
            v2x, v2y + 14.0, escape(freeVarNames[1]), llStr));

        sb.append("</svg>");
        return sb.toString();
    }

    /**
     * Builds an SVG path string for REGION mode using horizontal scan-line rectangles.
     * Adjacent matching pixels in the same row are merged into one rect to reduce path size.
     */
    private static String buildRegionPath(float[][] values, RenderParams params,
                                           int X0, int Y0, int W, int H, int step) {
        double min = params.regionMin();
        double max = params.regionMax();
        StringBuilder path = new StringBuilder();

        for (int j = 0; j <= H; j += step) {
            int runStart = -1;
            for (int i = 0; i <= W; i += step) {
                float v = values[i][j];
                boolean hit = !Float.isNaN(v) && v >= min && v <= max;
                if (hit && runStart < 0) {
                    runStart = i;
                } else if (!hit && runStart >= 0) {
                    appendRect(path, X0 + runStart, Y0 + j, X0 + i, Y0 + j + step);
                    runStart = -1;
                }
            }
            if (runStart >= 0) {
                appendRect(path, X0 + runStart, Y0 + j, X0 + W + step, Y0 + j + step);
            }
        }
        return path.toString().trim();
    }

    private static void appendRect(StringBuilder sb, int x1, int y1, int x2, int y2) {
        sb.append(String.format(Locale.US, "M%d,%dH%dV%dH%dZ", x1, y1, x2, y2, x1));
    }

    /** Converts a list of {x1,y1,x2,y2} segments (grid-local) to SVG path data with offset. */
    private static String toPathData(List<double[]> segments, int X0, int Y0) {
        if (segments.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(segments.size() * 30);
        for (double[] seg : segments) {
            sb.append(String.format(Locale.US, "M%.2f,%.2fL%.2f,%.2f",
                X0 + seg[0], Y0 + seg[1], X0 + seg[2], Y0 + seg[3]));
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
