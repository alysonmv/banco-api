package com.banco.bancoapi.infrastructure.persistence.jpa;

import com.banco.bancoapi.infrastructure.persistence.entity.MovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementJpaRepository extends JpaRepository<MovementEntity, Long> {

    /** Movimentacoes onde a conta aparece como origem ou destino. */
    @Query("""
            select m from MovementEntity m
            where m.fromAccountId = :accountId or m.toAccountId = :accountId
            order by m.createdAt desc, m.id desc
            """)
    Page<MovementEntity> findByAccount(@Param("accountId") Long accountId, Pageable pageable);
}
