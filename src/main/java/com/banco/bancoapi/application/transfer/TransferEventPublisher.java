package com.banco.bancoapi.application.transfer;

import com.banco.bancoapi.domain.model.Movement;

/**
 * Port para anunciar que uma transferencia foi concluida. A publicacao efetiva na fila
 * acontece APOS o commit (responsabilidade da infraestrutura) e e NAO-CRITICA: se falhar,
 * a transferencia ja esta concluida e NAO deve ser revertida.
 */
public interface TransferEventPublisher {
    void transferCompleted(Movement movement);
}
