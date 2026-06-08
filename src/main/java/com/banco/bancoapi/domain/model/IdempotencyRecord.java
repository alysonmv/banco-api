package com.banco.bancoapi.domain.model;

import java.time.Instant;

/**
 * Registro de uma operacao idempotente ja executada: guarda o hash do request original
 * (para detectar replay divergente) e a resposta serializada (para devolver sem reexecutar).
 */
public record IdempotencyRecord(
        String idempotencyKey,
        String requestHash,
        Long movementId,
        int responseStatus,
        String responseBody,
        Instant createdAt
) {
}
