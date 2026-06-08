package com.banco.bancoapi.domain.exception;

/** Transferencia invalida: valor <= 0 ou origem igual ao destino. */
public class InvalidTransferException extends DomainException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
