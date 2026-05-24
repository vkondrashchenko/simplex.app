package com.symplex.core.renderer;

import com.symplex.core.geometry.BarycentricMapper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Renders a {@link SimplexGrid} to a PNG image.
 *
 * <p>Port of Delphi {@code BuildImage} + {@code GetFunction}'s drawing code, extended
 * to support all PDF render modes (Region, Isoline-single, Isoline-range).
 *
 * <p>Canvas: 457×457 px, dark-grey background, equilateral triangle in white,
 * triangle interior in yellow, matched pixels coloured per mode.
 */
public final class SimplexRenderer {

    // Colours (match original Delphi palette)
    private static final Color BG       = new Color(50, 50, 50);
    private static final Color TRIANGLE = Color.WHITE;
    private static final Color INTERIOR = Color.YELLOW;
    private static final Color REGION_HIT = Color.RED;

    // Isoline palette — 12 distinct hues cycling for ISOLINE_RANGE
    private static final Color[] ISO_PALETTE = {
        Color.RED, new Color(255, 128, 0), Color.YELLOW,
        new Color(0, 200, 0), Color.CYAN, new Color(0, 128, 255),
        new Color(160, 0, 255), Color.MAGENTA, new Color(255, 64, 64),
        new Color(64, 255, 128), new Color(255, 200, 0), new Color(0, 200, 255)
    };

    private SimplexRenderer() {}

    /**
     * Renders {@code grid} using {@code params} and returns the PNG as a byte array.
     *
     * @param grid         sweep results
     * @param params       render configuration
     * @param freeVarNames labels for the three simplex axes (e.g. ["x2", "x3", "x4"])
     * @param ll           simplex total weight (displayed at vertices)
     */
    public static byte[] renderPng(
            SimplexGrid grid,
            RenderParams params,
            String[] freeVarNames,
            double ll
    ) {
        BufferedImage img = render(grid, params, freeVarNames, ll);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "PNG", baos);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode PNG", e);
        }
        return baos.toByteArray();
    }

    /** Renders to a {@link BufferedImage} — exposed for unit tests. */
    public static BufferedImage render(
            SimplexGrid grid,
            RenderParams params,
            String[] freeVarNames,
            double ll
    ) {
        final int SIZE = BarycentricMapper.CANVAS_SIZE;
        final int X0 = BarycentricMapper.X0;
        final int Y0 = BarycentricMapper.Y0;
        final int W  = BarycentricMapper.W;
        final int H  = BarycentricMapper.H;

        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- Background ---
        g.setColor(BG);
        g.fillRect(0, 0, SIZE, SIZE);

        // --- Triangle outline ---
        int[] vx = {X0 + W / 2, X0,     X0 + W};
        int[] vy = {Y0,         Y0 + H, Y0 + H};
        g.setColor(TRIANGLE);
        g.drawPolygon(vx, vy, 3);

        // --- Compute isoline target list ---
        List<Double> isoTargets = new ArrayList<>();
        List<Color>  isoColors  = new ArrayList<>();
        if (params.mode() == RenderMode.ISOLINE_RANGE) {
            int colorIdx = 0;
            for (double v = params.isoFrom();
                 v <= params.isoTo() + 1e-12;
                 v += params.isoStep()) {
                isoTargets.add(v);
                isoColors.add(ISO_PALETTE[colorIdx % ISO_PALETTE.length]);
                colorIdx++;
            }
        }

        // --- Paint pixels ---
        float[][] values = grid.values();
        int step = params.pixelStep();
        for (int i = 0; i <= W; i += step) {
            for (int j = 0; j <= H; j += step) {
                if (Float.isNaN(values[i][j])) continue;
                double y = values[i][j];
                int px = X0 + i;
                int py = Y0 + j;

                Color c = switch (params.mode()) {
                    case REGION -> (y >= params.regionMin() && y <= params.regionMax())
                            ? REGION_HIT : INTERIOR;
                    case ISOLINE_SINGLE -> matchesIsoline(y, params.isoValue(), params.precision())
                            ? REGION_HIT : INTERIOR;
                    case ISOLINE_RANGE -> {
                        Color hit = INTERIOR;
                        for (int k = 0; k < isoTargets.size(); k++) {
                            if (matchesIsoline(y, isoTargets.get(k), params.precision())) {
                                hit = isoColors.get(k);
                                break;
                            }
                        }
                        yield hit;
                    }
                };

                // Fill a pixelStep×pixelStep block for coarse sweeps
                g.setColor(c);
                g.fillRect(px, py, step, step);
            }
        }

        // --- Vertex labels (free variable names + LL value) ---
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        String llStr = String.format("=%.3g", ll);
        drawVertexLabel(g, freeVarNames[2] + llStr, X0 + W / 2, Y0 - 20,         Anchor.TOP);
        drawVertexLabel(g, freeVarNames[0] + llStr, X0,         Y0 + H + 14,     Anchor.LEFT);
        drawVertexLabel(g, freeVarNames[1] + llStr, X0 + W,     Y0 + H + 14,     Anchor.RIGHT);

        g.dispose();
        return img;
    }

    // --- helpers ---

    /**
     * True when Y matches target at the given decimal precision.
     * Matches if {@code round(Y * 10^p) == round(target * 10^p)}.
     */
    static boolean matchesIsoline(double y, double target, double precision) {
        double scale = Math.pow(10, precision);
        return Math.round(y * scale) == Math.round(target * scale);
    }

    private enum Anchor { LEFT, RIGHT, TOP }

    private static void drawVertexLabel(Graphics2D g, String text, int x, int y, Anchor anchor) {
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        int drawX = switch (anchor) {
            case LEFT  -> x;
            case RIGHT -> x - w;
            case TOP   -> x - w / 2;
        };
        g.drawString(text, drawX, y + fm.getAscent());
    }
}
