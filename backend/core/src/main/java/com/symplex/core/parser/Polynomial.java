package com.symplex.core.parser;

import java.util.List;

/**
 * Parsed polynomial: ordered list of monomials and the highest variable index referenced.
 */
public record Polynomial(List<Monomial> monomials, int maxVarIndex) {

    public Polynomial {
        monomials = List.copyOf(monomials);
        if (maxVarIndex < 0) throw new IllegalArgumentException("maxVarIndex must be >= 0");
    }

    public int size() { return monomials.size(); }
}
