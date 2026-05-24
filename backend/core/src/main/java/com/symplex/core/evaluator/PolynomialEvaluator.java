package com.symplex.core.evaluator;

import com.symplex.core.parser.Monomial;
import com.symplex.core.parser.Polynomial;

/**
 * Evaluates a {@link Polynomial} at a given variable vector.
 *
 * <p>Port of Delphi {@code Alpha.CalcFunction} — direct translation with the
 * 1-based → 0-based index conversion preserved.
 */
public final class PolynomialEvaluator {

    private PolynomialEvaluator() {}

    /**
     * Computes {@code Σᵢ cᵢ · Πⱼ x[kᵢⱼ − 1]} where {@code kᵢⱼ} are 1-based variable indices.
     *
     * @param polynomial parsed polynomial
     * @param x          0-indexed variable vector; must have length ≥ {@code polynomial.maxVarIndex()}
     * @return evaluated scalar value
     * @throws ArrayIndexOutOfBoundsException if {@code x} is too short for the polynomial's indices
     */
    public static double evaluate(Polynomial polynomial, double[] x) {
        double sum = 0.0;
        for (Monomial m : polynomial.monomials()) {
            double product = m.coefficient();
            for (int idx : m.varIndices()) {
                product *= x[idx - 1];   // 1-based idx → 0-based array position
            }
            sum += product;
        }
        return sum;
    }
}
