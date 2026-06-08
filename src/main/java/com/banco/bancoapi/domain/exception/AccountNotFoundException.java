package com.banco.bancoapi.domain.exception;

public class AccountNotFoundException extends DomainException {
    private final Long accountId;

    public AccountNotFoundException(Long accountId) {
        super("Conta nao encontrada: " + accountId);
        this.accountId = accountId;
    }

    public Long accountId() {
        return accountId;
    }
}
