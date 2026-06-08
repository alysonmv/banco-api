package com.banco.bancoapi.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalizaParaDuasCasasComHalfEven() {
        assertThat(Money.of("10.005").value()).isEqualByComparingTo("10.00"); // HALF_EVEN
        assertThat(Money.of("10.015").value()).isEqualByComparingTo("10.02");
        assertThat(Money.of("1").value().scale()).isEqualTo(2);
    }

    @Test
    void somaESubtrai() {
        assertThat(Money.of("100.00").add(Money.of("0.55"))).isEqualTo(Money.of("100.55"));
        assertThat(Money.of("100.00").subtract(Money.of("0.55"))).isEqualTo(Money.of("99.45"));
    }

    @Test
    void comparacoes() {
        assertThat(Money.of("10.00").isGreaterThan(Money.of("9.99"))).isTrue();
        assertThat(Money.of("10.00").isLessThan(Money.of("10.01"))).isTrue();
        assertThat(Money.of("0.00").isPositive()).isFalse();
        assertThat(Money.of("0.01").isPositive()).isTrue();
        assertThat(Money.of("-0.01").isNegative()).isTrue();
    }

    @Test
    void igualdadePorValor() {
        assertThat(Money.of("10.0")).isEqualTo(Money.of("10.00"));
        assertThat(Money.of(new BigDecimal("10"))).isEqualTo(Money.of("10.00"));
    }
}
