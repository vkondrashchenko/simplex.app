package com.symplex.core.renderer;

/**
 * Rendering parameters passed to {@link SimplexRenderer}.
 *
 * @param mode       how to highlight points
 * @param regionMin  lower bound for {@link RenderMode#REGION}
 * @param regionMax  upper bound for {@link RenderMode#REGION}
 * @param isoValue   target value for {@link RenderMode#ISOLINE_SINGLE}
 * @param isoFrom    range start for {@link RenderMode#ISOLINE_RANGE}
 * @param isoTo      range end   for {@link RenderMode#ISOLINE_RANGE}
 * @param isoStep    range step  for {@link RenderMode#ISOLINE_RANGE}
 * @param precision  SVG stroke-width for isoline rendering; {@code strokeWidth = max(0.5, precision)}
 *                   so 0 → hairline (0.5 px), 1 → default (1 px), 3 → thick (3 px)
 * @param pixelStep  sweep stride in pixels (1 = every pixel; 2 = every other; max 10)
 */
public record RenderParams(
        RenderMode mode,
        double regionMin,
        double regionMax,
        double isoValue,
        double isoFrom,
        double isoTo,
        double isoStep,
        double precision,
        int pixelStep
) {
    public RenderParams {
        if (mode == null) throw new IllegalArgumentException("mode must not be null");
        pixelStep = Math.max(1, Math.min(10, pixelStep));
        if (precision < 0) precision = 0;
    }

    /** Convenience factory for {@link RenderMode#REGION}. */
    public static RenderParams region(double min, double max, int pixelStep) {
        return new RenderParams(RenderMode.REGION, min, max, 0, 0, 0, 0, 0, pixelStep);
    }

    /** Convenience factory for {@link RenderMode#ISOLINE_SINGLE}. */
    public static RenderParams isolineSingle(double value, double precision, int pixelStep) {
        return new RenderParams(RenderMode.ISOLINE_SINGLE, 0, 0, value, 0, 0, 0, precision, pixelStep);
    }

    /** Convenience factory for {@link RenderMode#ISOLINE_RANGE}. */
    public static RenderParams isolineRange(double from, double to, double step, double precision, int pixelStep) {
        return new RenderParams(RenderMode.ISOLINE_RANGE, 0, 0, 0, from, to, step, precision, pixelStep);
    }
}
