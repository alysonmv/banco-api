package com.banco.bancoapi.application.transfer;

import com.banco.bancoapi.domain.model.Movement;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resultado do caso de uso de transferencia. E o que o controller serializa para o cliente
 * e tambem o que e persistido (em JSON) no registro de idempotencia, para replay fiel.
 */
public record TransferResult(
        Long movementId,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String status,
        Instant createdAt
) {
    public static TransferResult from(Movement movement) {
        return new TransferResult(
                movement.id(),
                movement.fromAccountId(),
                movement.toAccountId(),
                movement.amount().value(),
                movement.status().name(),
                movement.createdAt()
        );
    }
}
