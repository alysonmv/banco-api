package com.banco.bancoapi.domain.exception;

/** Raiz das excecoes de regra de negocio do dominio. */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
