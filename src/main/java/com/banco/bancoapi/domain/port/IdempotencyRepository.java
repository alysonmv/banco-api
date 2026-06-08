package com.banco.bancoapi.domain.port;

import com.banco.bancoapi.domain.model.IdempotencyRecord;

import java.util.Optional;

/** Persistencia das chaves de idempotencia (claim primeiro, completa depois). */
public interface IdempotencyRepository {

    Optional<IdempotencyRecord> findByKey(String idempotencyKey);

    /**
     * Tenta reservar a chave inserindo a linha com o hash do request.
     * true se reservou, false se a chave ja existia (replay). A PK unica serializa
     * requests concorrentes com a mesma chave.
     */
    boolean tryClaim(String idempotencyKey, String requestHash);

    /** Fecha o claim com o resultado da transferencia (movimentacao + resposta em JSON). */
    void complete(String idempotencyKey, Long movementId, int responseStatus, String responseBody);
}
