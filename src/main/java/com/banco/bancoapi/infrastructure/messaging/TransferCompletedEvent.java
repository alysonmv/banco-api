package com.banco.bancoapi.infrastructure.messaging;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento de transferencia concluida. Publicado internamente (Spring) pelo adapter e,
 * APOS o commit, enviado para a fila RabbitMQ. Carrega o correlationId para que a thread
 * do consumer possa reconstruir o contexto de log (o MDC nao se propaga sozinho).
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
