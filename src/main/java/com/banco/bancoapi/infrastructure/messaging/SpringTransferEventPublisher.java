package com.banco.bancoapi.infrastructure.messaging;

import com.banco.bancoapi.application.transfer.TransferEventPublisher;
import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.infrastructure.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Implementa o TransferEventPublisher. Publica um evento do Spring dentro da transacao; a
 * fila so recebe de fato depois do commit (ver RabbitTransferPublisher).
 * Pego o correlationId do MDC aqui, ainda na thread da request, pra repassar adiante.
 */
@Component
public class SpringTransferEventPublisher implements TransferEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringTransferEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void transferCompleted(Movement movement) {
        TransferCompletedEvent event = new TransferCompletedEvent(
                movement.id(),
                movement.fromAccountId(),
                movement.toAccountId(),
                movement.amount().value(),
                movement.createdAt(),
                MDC.get(CorrelationIdFilter.MDC_KEY)
        );
        publisher.publishEvent(event);
    }
}
