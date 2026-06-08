package com.banco.bancoapi.application.transfer;

import com.banco.bancoapi.domain.model.Movement;

/**
 * Avisa que a transferencia terminou. A publicacao na fila acontece depois do commit (na
 * infra) e nao e critica: se falhar, a transferencia ja foi feita e nao se desfaz.
 */
public interface TransferEventPublisher {
    void transferCompleted(Movement movement);
}
