package com.banco.bancoapi.domain.exception;

/** Transferencia invalida por regra de negocio: valor <= 0 ou mesma conta de origem/destino. */
public class InvalidTransferException extends DomainException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
