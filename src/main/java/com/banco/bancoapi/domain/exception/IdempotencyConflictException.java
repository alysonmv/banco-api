package com.banco.bancoapi.domain.exception;

/** Mesma Idempotency-Key usada de novo, mas com payload diferente do original. */
public class IdempotencyConflictException extends DomainException {
    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency-Key '" + idempotencyKey + "' ja foi usada com um payload diferente");
    }
}
