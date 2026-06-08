package com.banco.bancoapi.infrastructure.persistence.mapper;

import com.banco.bancoapi.domain.model.Money;
import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.infrastructure.persistence.entity.MovementEntity;

public final class MovementMapper {

    private MovementMapper() {
    }

    public static Movement toDomain(MovementEntity entity) {
        return new Movement(
                entity.getId(),
                entity.getFromAccountId(),
                entity.getToAccountId(),
                Money.of(entity.getAmount()),
                entity.getStatus(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt()
        );
    }
}
