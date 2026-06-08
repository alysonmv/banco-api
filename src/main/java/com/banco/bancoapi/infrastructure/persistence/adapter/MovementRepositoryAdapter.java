package com.banco.bancoapi.infrastructure.persistence.adapter;

import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.domain.model.PagedResult;
import com.banco.bancoapi.domain.port.MovementRepository;
import com.banco.bancoapi.infrastructure.persistence.entity.MovementEntity;
import com.banco.bancoapi.infrastructure.persistence.jpa.MovementJpaRepository;
import com.banco.bancoapi.infrastructure.persistence.mapper.MovementMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MovementRepositoryAdapter implements MovementRepository {

    private final MovementJpaRepository jpa;

    public MovementRepositoryAdapter(MovementJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Movement save(Movement movement) {
        MovementEntity entity = new MovementEntity(
                movement.fromAccountId(),
                movement.toAccountId(),
                movement.amount().value(),
                movement.status(),
                movement.idempotencyKey()
        );
        return MovementMapper.toDomain(jpa.save(entity));
    }

    @Override
    public PagedResult<Movement> findByAccountId(Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MovementEntity> result = jpa.findByAccount(accountId, pageable);
        List<Movement> content = result.getContent().stream()
                .map(MovementMapper::toDomain)
                .toList();
        return new PagedResult<>(content, page, size, result.getTotalElements(), result.getTotalPages());
    }
}
