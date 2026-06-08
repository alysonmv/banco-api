package com.banco.bancoapi.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica a notificacao na fila SOMENTE apos o commit da transferencia
 * ({@link TransactionPhase#AFTER_COMMIT}). Nunca dentro da transacao de debito/credito.
 *
 * <p><b>Prioridade explicita:</b> a notificacao e NAO-CRITICA. Se a publicacao falhar, a
 * transferencia JA foi concluida e NAO deve ser revertida — apenas registramos o erro em log.
 * (Limitacao conhecida sem outbox: ha uma janela em que a notificacao pode ser perdida.)
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
            // NAO-CRITICO: a transferencia ja foi concluida e commitada. So logamos.
            log.error("Falha ao publicar notificacao da transferencia movementId={} (nao-critico, "
                    + "transferencia mantida)", event.movementId(), e);
        }
    }
}
