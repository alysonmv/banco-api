package com.banco.bancoapi.infrastructure.messaging;

import com.banco.bancoapi.application.transfer.TransferEventPublisher;
import com.banco.bancoapi.domain.model.Movement;
import com.banco.bancoapi.infrastructure.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adapter do port {@link TransferEventPublisher}. Publica um evento Spring DENTRO da transacao;
 * a entrega real na fila acontece somente APOS o commit (ver {@link RabbitTransferPublisher}).
 * Captura o correlationId do MDC aqui (ainda na thread da requisicao) para propaga-lo adiante.
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
