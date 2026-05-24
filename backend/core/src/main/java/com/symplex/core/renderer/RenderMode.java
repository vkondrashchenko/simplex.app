package com.symplex.core.renderer;

/**
 * How the simplex diagram highlights points of interest.
 *
 * <p>Implements the full feature set described in the application PDF (§3.7),
 * resolving the PDF-vs-code discrepancy in favour of the richer PDF spec.
 */
public enum RenderMode {

    /** Highlight all pixels where Y ∈ [regionMin, regionMax]. */
    REGION,

    /**
     * Highlight pixels where Y matches a single value within {@code precision} decimal digits.
     * Match condition: {@code Math.round(Y * 10^p) == Math.round(target * 10^p)}.
     */
    ISOLINE_SINGLE,

    /**
     * Draw isolines for every value in {@code [isoFrom, isoFrom+isoStep, …, isoTo]},
     * each matched at {@code precision} decimal digits. Distinct colours per isoline.
     */
    ISOLINE_RANGE
}
