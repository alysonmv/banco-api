package com.banco.bancoapi.domain.model;

import java.time.Instant;

/**
 * Operacao idempotente que ja rodou: guarda o hash do request original (pra pegar replay
 * com payload diferente) e a resposta serializada (pra devolver sem rodar de novo).
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
