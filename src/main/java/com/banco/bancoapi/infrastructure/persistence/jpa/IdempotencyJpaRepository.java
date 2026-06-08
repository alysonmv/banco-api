package com.banco.bancoapi.infrastructure.persistence.jpa;

import com.banco.bancoapi.infrastructure.persistence.entity.IdempotencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyEntity, String> {

    /**
     * Insere o claim de forma atomica. {@code ON CONFLICT DO NOTHING} faz o INSERT virar no-op
     * (retorna 0) quando a chave ja existe; sob concorrencia, bloqueia em linha nao-commitada
     * ate o outro request commitar. flush antes / clear depois mantem a consistencia do contexto.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO idempotency_records (idempotency_key, request_hash, created_at)
            VALUES (:key, :hash, now())
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertClaim(@Param("key") String key, @Param("hash") String hash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE idempotency_records
            SET movement_id = :movementId, response_status = :status, response_body = :body
            WHERE idempotency_key = :key
            """, nativeQuery = true)
    int complete(@Param("key") String key,
                 @Param("movementId") Long movementId,
                 @Param("status") int status,
                 @Param("body") String body);
}
