package com.banco.bancoapi.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica a notificacao na fila so depois do commit da transferencia (AFTER_COMMIT), nunca
 * dentro da transacao de debito/credito.
 *
 * A notificacao nao e critica: se a publicacao falhar, a transferencia ja foi feita e nao
 * deve ser desfeita, so registramos o erro. Sem outbox tem uma janela em que a notificacao
 * pode se perder.
 */
@Component
public class RabbitTransferPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitTransferPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitTransferPublisher(RabbitTemplate rabbitTemplate,
                                   @Value("${banco.messaging.exchange}") String exchange,
                                   @Value("${banco.messaging.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                if (event.correlationId() != null) {
                    message.getMessageProperties().setHeader(RabbitConfig.CORRELATION_HEADER, event.correlationId());
                }
                return message;
            });
            log.info("Notificacao de transferencia publicada (movementId={})", event.movementId());
        } catch (Exception e) {
            // nao e critico: a transferencia ja foi commitada, so logamos
            log.error("Falha ao publicar notificacao da transferencia movementId={} (nao-critico, "
                    + "transferencia mantida)", event.movementId(), e);
        }
    }
}
