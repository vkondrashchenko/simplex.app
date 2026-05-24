package com.symplex.core.parser;

import java.util.Arrays;

/**
 * One term in a polynomial: coefficient × x[i₁] × x[i₂] × … (1-based indices).
 * Port of Delphi TSumComponent with all size limits removed.
 */
public record Monomial(double coefficient, int[] varIndices) {

    public Monomial {
        if (varIndices == null) throw new IllegalArgumentException("varIndices must not be null");
    }

    @Override
    public String toString() {
        if (varIndices.length == 0) return String.valueOf(coefficient);
        StringBuilder sb = new StringBuilder();
        sb.append(coefficient);
        for (int idx : varIndices) sb.append("*x").append(idx);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Monomial m)) return false;
        return Double.compare(coefficient, m.coefficient) == 0
                && Arrays.equals(varIndices, m.varIndices);
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(coefficient) + Arrays.hashCode(varIndices);
    }
}
