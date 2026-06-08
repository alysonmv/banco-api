package com.banco.bancoapi.domain.exception;

import com.banco.bancoapi.domain.model.Money;

public class InsufficientBalanceException extends DomainException {
    public InsufficientBalanceException(Long accountId, Money balance, Money requested) {
        super("Saldo insuficiente na conta " + accountId + ": saldo=" + balance + ", solicitado=" + requested);
    }
}
