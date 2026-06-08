package com.banco.bancoapi.application.transfer;

import com.banco.bancoapi.domain.model.Movement;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resultado da transferencia. E o que o controller devolve pro cliente e tambem o que fica
 * salvo (em JSON) na idempotencia, pra conseguir repetir a mesma resposta.
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
