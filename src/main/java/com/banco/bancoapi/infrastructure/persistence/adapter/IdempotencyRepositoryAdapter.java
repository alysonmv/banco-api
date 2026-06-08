package com.banco.bancoapi.infrastructure.persistence.adapter;

import com.banco.bancoapi.domain.model.IdempotencyRecord;
import com.banco.bancoapi.domain.port.IdempotencyRepository;
import com.banco.bancoapi.infrastructure.persistence.entity.IdempotencyEntity;
import com.banco.bancoapi.infrastructure.persistence.jpa.IdempotencyJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IdempotencyRepositoryAdapter implements IdempotencyRepository {

    private final IdempotencyJpaRepository jpa;

    public IdempotencyRepositoryAdapter(IdempotencyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(String idempotencyKey) {
        return jpa.findById(idempotencyKey).map(this::toDomain);
    }

    @Override
    public boolean tryClaim(String idempotencyKey, String requestHash) {
        return jpa.insertClaim(idempotencyKey, requestHash) > 0;
    }

    @Override
    public void complete(String idempotencyKey, Long movementId, int responseStatus, String responseBody) {
        jpa.complete(idempotencyKey, movementId, responseStatus, responseBody);
    }

    private IdempotencyRecord toDomain(IdempotencyEntity e) {
        return new IdempotencyRecord(
                e.getIdempotencyKey(),
                e.getRequestHash(),
                e.getMovementId(),
                e.getResponseStatus() == null ? 0 : e.getResponseStatus(),
                e.getResponseBody(),
                e.getCreatedAt()
        );
    }
}
