package com.symplex.core.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PolynomialParserTest {

    // ALPHA.TXT equation from the PDF control example
    private static final String ALPHA_EQ =
            "Y=5.76*x1+7.20*x2+13.36*x3+68.16*x4" +
            "-3.66*x1*x2+32.13*x1*x3+62.8*x1*x4" +
            "+42.80*x2*x3+56.86*x2*x4+58.65*x3*x4";

    @Test
    void parse_alphaEquation_tenMonomials() {
        Polynomial p = PolynomialParser.parse(ALPHA_EQ);
        assertThat(p.monomials()).hasSize(10);
        assertThat(p.maxVarIndex()).isEqualTo(4);
    }

    @Test
    void parse_alphaEquation_linearCoefficients() {
        Polynomial p = PolynomialParser.parse(ALPHA_EQ);
        List<Monomial> m = p.monomials();
        // First four are linear terms: x1, x2, x3, x4
        assertThat(m.get(0).coefficient()).isEqualTo(5.76);
        assertThat(m.get(1).coefficient()).isEqualTo(7.20);
        assertThat(m.get(2).coefficient()).isEqualTo(13.36);
        assertThat(m.get(3).coefficient()).isEqualTo(68.16);
    }

    @Test
    void parse_alphaEquation_interactionCoefficients() {
        Polynomial p = PolynomialParser.parse(ALPHA_EQ);
        List<Monomial> m = p.monomials();
        // 5th term: -3.66*x1*x2
        assertThat(m.get(4).coefficient()).isEqualTo(-3.66);
        assertThat(m.get(4).varIndices()).containsExactly(1, 2);
        // 9th term: 56.86*x2*x4
        assertThat(m.get(8).coefficient()).isEqualTo(56.86);
        assertThat(m.get(8).varIndices()).containsExactly(2, 4);
        // 10th term: 58.65*x3*x4
        assertThat(m.get(9).coefficient()).isEqualTo(58.65);
        assertThat(m.get(9).varIndices()).containsExactly(3, 4);
    }

    @Test
    void parse_leadingMinus() {
        Polynomial p = PolynomialParser.parse("Y=-3.5*x1+2*x2");
        assertThat(p.monomials().get(0).coefficient()).isEqualTo(-3.5);
        assertThat(p.monomials().get(1).coefficient()).isEqualTo(2.0);
    }

    @Test
    void parse_caseInsensitivePrefix() {
        Polynomial p1 = PolynomialParser.parse("y=1*x1");
        Polynomial p2 = PolynomialParser.parse("Y=1*x1");
        Polynomial p3 = PolynomialParser.parse("1*x1");
        assertThat(p1.monomials()).hasSize(1);
        assertThat(p2.monomials()).hasSize(1);
        assertThat(p3.monomials()).hasSize(1);
    }

    @Test
    void parse_multiDigitVarIndices() {
        Polynomial p = PolynomialParser.parse("Y=1.5*x10+2.5*x20*x10");
        assertThat(p.maxVarIndex()).isEqualTo(20);
        assertThat(p.monomials().get(0).varIndices()).containsExactly(10);
        assertThat(p.monomials().get(1).varIndices()).containsExactly(20, 10);
    }

    @Test
    void parse_whitespaceToleranceAndCaseInsensitiveX() {
        Polynomial p = PolynomialParser.parse("Y = 2 * X1 + 3 * x2 * X3");
        assertThat(p.monomials()).hasSize(2);
        assertThat(p.monomials().get(0).coefficient()).isEqualTo(2.0);
        assertThat(p.monomials().get(1).varIndices()).containsExactly(2, 3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Y=", "abc", "Y=x"})
    void parse_invalidInput_throws(String eq) {
        assertThatThrownBy(() -> PolynomialParser.parse(eq))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
