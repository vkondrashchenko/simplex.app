package com.symplex.lambda.dto;

import com.symplex.core.renderer.RenderMode;

import java.util.Map;

/**
 * Request body for POST /api/render.
 *
 * @param equation   polynomial equation string (e.g. "Y=5.76*x1+7.20*x2")
 * @param fixedVars  1-based variable index → fixed value; omitted variables are free
 * @param mode       render mode (REGION, ISOLINE_SINGLE, ISOLINE_RANGE)
 * @param regionMin  lower bound for REGION mode
 * @param regionMax  upper bound for REGION mode
 * @param isoValue   target value for ISOLINE_SINGLE mode
 * @param isoFrom    range start for ISOLINE_RANGE mode
 * @param isoTo      range end for ISOLINE_RANGE mode
 * @param isoStep    step for ISOLINE_RANGE mode
 * @param precision  decimal digits for isoline matching
 * @param pixelStep  sweep stride (1 = every pixel; max 10)
 */
public record RenderRequest(
        String equation,
        Map<Integer, Double> fixedVars,
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
    public RenderRequest {
        if (fixedVars == null) fixedVars = Map.of();
        if (mode == null) mode = RenderMode.REGION;
        if (pixelStep < 1) pixelStep = 1;
    }
}
