package com.banco.bancoapi.domain.exception;

/** Violacao de regra de validacao de entrada no dominio (mapeada para 400). */
public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(message);
    }
}
