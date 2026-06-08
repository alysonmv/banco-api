package com.banco.bancoapi.domain.model;

import com.banco.bancoapi.domain.exception.InsufficientBalanceException;

import java.util.Objects;

/**
 * Conta de banco. Entidade de negocio em Java puro — sem JPA/Spring.
 * As regras de debito/credito vivem aqui; a persistencia e a concorrencia
 * (lock pessimista) sao responsabilidade da infraestrutura.
 */
public class Account {

    private final Long id;
    private final String name;
    private Money balance;

    public Account(Long id, String name, Money balance) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name nao pode ser nulo");
        this.balance = Objects.requireNonNull(balance, "balance nao pode ser nulo");
    }

    /**
     * Debita o valor. Valida saldo suficiente ANTES de alterar — o saldo nunca fica negativo.
     * A garantia sob concorrencia depende de a conta ter sido carregada sob lock pessimista.
     */
    public void debit(Money amount) {
        requirePositive(amount);
        if (balance.isLessThan(amount)) {
            throw new InsufficientBalanceException(id, balance, amount);
        }
        this.balance = balance.subtract(amount);
    }

    public void credit(Money amount) {
        requirePositive(amount);
        this.balance = balance.add(amount);
    }

    private void requirePositive(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("valor de movimentacao deve ser positivo");
        }
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Money balance() {
        return balance;
    }
}
