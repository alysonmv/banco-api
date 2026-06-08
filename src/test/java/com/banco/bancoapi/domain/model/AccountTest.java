package com.banco.bancoapi.domain.model;

import com.banco.bancoapi.domain.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void debitaReduzSaldo() {
        Account account = new Account(1L, "Alice", Money.of("100.00"));
        account.debit(Money.of("30.00"));
        assertThat(account.balance()).isEqualTo(Money.of("70.00"));
    }

    @Test
    void creditaAumentaSaldo() {
        Account account = new Account(1L, "Alice", Money.of("100.00"));
        account.credit(Money.of("30.00"));
        assertThat(account.balance()).isEqualTo(Money.of("130.00"));
    }

    @Test
    void debitoMaiorQueSaldoLancaExcecaoENaoAltera() {
        Account account = new Account(1L, "Alice", Money.of("20.00"));
        assertThatThrownBy(() -> account.debit(Money.of("20.01")))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(account.balance()).isEqualTo(Money.of("20.00"));
    }

    @Test
    void debitoExatoDoSaldoZeraConta() {
        Account account = new Account(1L, "Alice", Money.of("20.00"));
        account.debit(Money.of("20.00"));
        assertThat(account.balance()).isEqualTo(Money.of("0.00"));
    }

    @Test
    void valorNaoPositivoRejeitado() {
        Account account = new Account(1L, "Alice", Money.of("20.00"));
        assertThatThrownBy(() -> account.debit(Money.of("0.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.credit(Money.of("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
