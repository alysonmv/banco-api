package com.banco.bancoapi.domain.port;

import com.banco.bancoapi.domain.model.IdempotencyRecord;

import java.util.Optional;

/** Port de persistencia das chaves de idempotencia (estrategia claim-first). */
public interface IdempotencyRepository {

    Optional<IdempotencyRecord> findByKey(String idempotencyKey);

    /**
     * Tenta reservar (claim) a chave inserindo a linha com o hash do request.
     * Retorna {@code true} se reservou; {@code false} se a chave ja existia (replay).
     * A unicidade da PK serializa requests concorrentes com a mesma chave.
     */
    boolean tryClaim(String idempotencyKey, String requestHash);

    /** Completa um claim com o resultado da transferencia (movimentacao + resposta serializada). */
    void complete(String idempotencyKey, Long movementId, int responseStatus, String responseBody);
}
