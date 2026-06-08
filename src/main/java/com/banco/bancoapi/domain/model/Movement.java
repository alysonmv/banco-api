package com.banco.bancoapi.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Linha do ledger, imutavel. Depois de criada nao muda.
 * O extrato sai daqui; nao e calculado a partir do saldo.
 */
public final class Movement {

    private final Long id;
    private final Long fromAccountId;
    private final Long toAccountId;
    private final Money amount;
    private final MovementStatus status;
    private final String idempotencyKey;
    private final Instant createdAt;

    public Movement(Long id, Long fromAccountId, Long toAccountId, Money amount,
                    MovementStatus status, String idempotencyKey, Instant createdAt) {
        this.id = id;
        this.fromAccountId = Objects.requireNonNull(fromAccountId);
        this.toAccountId = Objects.requireNonNull(toAccountId);
        this.amount = Objects.requireNonNull(amount);
        this.status = Objects.requireNonNull(status);
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    /** Movimentacao concluida, sem id/timestamp ainda (vem na hora de salvar). */
    public static Movement completed(Long fromAccountId, Long toAccountId, Money amount, String idempotencyKey) {
        return new Movement(null, fromAccountId, toAccountId, amount, MovementStatus.COMPLETED, idempotencyKey, null);
    }

    public Long id() {
        return id;
    }

    public Long fromAccountId() {
        return fromAccountId;
    }

    public Long toAccountId() {
        return toAccountId;
    }

    public Money amount() {
        return amount;
    }

    public MovementStatus status() {
        return status;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
