package com.banco.bancoapi.domain.exception;

/** Replay de uma Idempotency-Key ja usada, porem com payload divergente do original. */
public class IdempotencyConflictException extends DomainException {
    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency-Key '" + idempotencyKey + "' ja foi usada com um payload diferente");
    }
}
