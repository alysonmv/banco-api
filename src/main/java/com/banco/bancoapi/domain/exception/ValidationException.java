package com.banco.bancoapi.domain.exception;

/** Entrada invalida no dominio (vira 400). */
public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(message);
    }
}
