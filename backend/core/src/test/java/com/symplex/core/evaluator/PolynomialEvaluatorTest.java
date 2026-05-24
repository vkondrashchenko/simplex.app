package com.symplex.core.evaluator;

import com.symplex.core.parser.Polynomial;
import com.symplex.core.parser.PolynomialParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PolynomialEvaluatorTest {

    // PDF control example: ALPHA.TXT with x1=0, x2=0.3, x3=0.3, x4=0.4 → Y=51.1452
    private static final String ALPHA_EQ =
            "Y=5.76*x1+7.20*x2+13.36*x3+68.16*x4" +
            "-3.66*x1*x2+32.13*x1*x3+62.8*x1*x4" +
            "+42.80*x2*x3+56.86*x2*x4+58.65*x3*x4";

    @Test
    void evaluate_controlExample_matches51point1452() {
        Polynomial p = PolynomialParser.parse(ALPHA_EQ);
        // 0-indexed: x1→[0], x2→[1], x3→[2], x4→[3]
        double[] x = {0.0, 0.3, 0.3, 0.4};
        double y = PolynomialEvaluator.evaluate(p, x);
        assertThat(y).isCloseTo(51.1452, within(1e-9));
    }

    @Test
    void evaluate_linearPoly_returnsWeightedSum() {
        Polynomial p = PolynomialParser.parse("Y=2*x1+3*x2");
        double[] x = {1.0, 1.0};
        assertThat(PolynomialEvaluator.evaluate(p, x)).isCloseTo(5.0, within(1e-12));
    }

    @Test
    void evaluate_interactionTerm_multipliesVariables() {
        Polynomial p = PolynomialParser.parse("Y=4*x1*x2");
        double[] x = {0.5, 0.5};
        assertThat(PolynomialEvaluator.evaluate(p, x)).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void evaluate_zeroCoefficient_contributeNothing() {
        Polynomial p = PolynomialParser.parse("Y=0*x1+5*x2");
        double[] x = {999.0, 2.0};
        assertThat(PolynomialEvaluator.evaluate(p, x)).isCloseTo(10.0, within(1e-12));
    }

    @Test
    void evaluate_atBoundary_x1equals1_restZero() {
        Polynomial p = PolynomialParser.parse(ALPHA_EQ);
        double[] x = {1.0, 0.0, 0.0, 0.0};
        // Only linear x1 term fires: 5.76
        assertThat(PolynomialEvaluator.evaluate(p, x)).isCloseTo(5.76, within(1e-9));
    }
}
