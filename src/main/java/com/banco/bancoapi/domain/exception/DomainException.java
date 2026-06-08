package com.banco.bancoapi.domain.exception;

/** Base das excecoes de regra de negocio. */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
