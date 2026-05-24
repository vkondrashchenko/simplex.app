package com.symplex.core.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a simplex-lattice polynomial string into a {@link Polynomial}.
 *
 * <p>Accepted format: {@code Y=a1*x1*x2+a2*x3-a3*x1*x4*x5+...}
 * <ul>
 *   <li>Leading {@code Y=} or {@code y=} (case-insensitive) is stripped.</li>
 *   <li>Whitespace around operators is tolerated.</li>
 *   <li>Coefficients may be negative (leading minus on first term allowed).</li>
 *   <li>Variable indices are multi-digit ({@code x10}, {@code x100} etc.).</li>
 *   <li>Coefficient {@code 1} must be written explicitly: {@code 1*x1}.</li>
 *   <li>No constant term allowed.</li>
 * </ul>
 *
 * <p>Fixes over the original Delphi {@code GetInfo}:
 * case-insensitive variable names, multi-digit indices, leading minus,
 * whitespace tolerance, explicit error messages.
 */
public final class PolynomialParser {

    private PolynomialParser() {}

    // Matches one monomial: optional leading sign, decimal coefficient, then *xN factors.
    // Group 1: sign+coefficient string (e.g. "-3.66" or "5.76")
    // Group 2: the factor chain (e.g. "*x1*x2" or "")
    private static final Pattern TERM = Pattern.compile(
            "([+-]?\\d+(?:\\.\\d+)?)\\s*((?:\\*\\s*[xX]\\d+\\s*)*)"
    );

    // Matches a single *xN factor (capturing the integer N).
    private static final Pattern FACTOR = Pattern.compile("\\*\\s*[xX](\\d+)\\s*");

    /**
     * Parses {@code equation} and returns a {@link Polynomial}.
     *
     * @param equation raw string, e.g. {@code "Y=5.76*x1+7.20*x2-3.66*x1*x2"}
     * @throws IllegalArgumentException on malformed input
     */
    public static Polynomial parse(String equation) {
        if (equation == null || equation.isBlank()) {
            throw new IllegalArgumentException("Equation must not be blank");
        }

        // Strip optional "Y=" or "y=" prefix (any prefix up to and including '=')
        String body = equation.trim();
        int eqIdx = body.indexOf('=');
        if (eqIdx >= 0) body = body.substring(eqIdx + 1).trim();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("Equation body is empty after stripping prefix");
        }

        // Normalise: collapse whitespace around * and operators, but keep sign glued to coefficient.
        // Strategy: tokenise with TERM pattern, walking through the string.
        // Between terms there must be a '+' or '-' separator (absorbed into the next term's sign).
        List<Monomial> monomials = new ArrayList<>();
        int maxVar = 0;

        // Pre-process: ensure every term starts with an explicit +/- so our pattern can match
        // the sign as part of the coefficient group.
        // We do this by inserting '+' if the string doesn't start with a sign.
        String normalised = body.replaceAll("\\s+", "");
        if (!normalised.isEmpty() && normalised.charAt(0) != '+' && normalised.charAt(0) != '-') {
            normalised = "+" + normalised;
        }
        // Now split on '+'/'-' boundaries that precede a digit (the sign is kept with the number).
        // Use TERM regex with positive-lookahead to walk the string.
        Matcher termMatcher = TERM.matcher(normalised);
        int lastEnd = 0;

        while (termMatcher.find()) {
            // Enforce no gaps (would indicate unexpected characters)
            if (termMatcher.start() != lastEnd) {
                String gap = normalised.substring(lastEnd, termMatcher.start());
                if (!gap.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Unexpected characters in equation near position " + lastEnd + ": '" + gap + "'");
                }
            }
            lastEnd = termMatcher.end();

            double coeff;
            try {
                coeff = Double.parseDouble(termMatcher.group(1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid coefficient: '" + termMatcher.group(1) + "'", e);
            }

            String factorChain = termMatcher.group(2);
            List<Integer> indices = new ArrayList<>();
            Matcher facMatcher = FACTOR.matcher(factorChain);
            while (facMatcher.find()) {
                int idx = Integer.parseInt(facMatcher.group(1));
                if (idx < 1) {
                    throw new IllegalArgumentException("Variable index must be >= 1, got: x" + idx);
                }
                indices.add(idx);
                if (idx > maxVar) maxVar = idx;
            }

            monomials.add(new Monomial(coeff, indices.stream().mapToInt(Integer::intValue).toArray()));
        }

        if (lastEnd != normalised.length()) {
            throw new IllegalArgumentException(
                    "Trailing unparsed characters: '" + normalised.substring(lastEnd) + "'");
        }
        if (monomials.isEmpty()) {
            throw new IllegalArgumentException("No monomials found in equation");
        }

        return new Polynomial(monomials, maxVar);
    }
}
