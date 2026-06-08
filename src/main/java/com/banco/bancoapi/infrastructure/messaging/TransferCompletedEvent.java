package com.banco.bancoapi.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de transferencia concluida. O adapter publica internamente (Spring) e, depois do
 * commit, vai pra fila do RabbitMQ. Leva o correlationId junto pro consumer conseguir remontar
 * o contexto de log (o MDC nao se propaga sozinho).
 */
public record TransferCompletedEvent(
        Long movementId,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        Instant createdAt,
        String correlationId
) {
}
