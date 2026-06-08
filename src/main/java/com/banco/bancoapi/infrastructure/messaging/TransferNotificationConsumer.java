package com.banco.bancoapi.infrastructure.messaging;

import com.banco.bancoapi.infrastructure.web.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumer da fila de notificacoes. Simula o envio da notificacao com um log estruturado.
 * Restaura o correlationId no MDC a partir do header da mensagem — o MDC nao se propaga
 * automaticamente para a thread do consumer.
 */
@Component
public class TransferNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferNotificationConsumer.class);

    @RabbitListener(
            queues = "${banco.messaging.queue}",
            autoStartup = "${banco.messaging.consumer-enabled:true}")
    public void onTransferCompleted(
            @Payload TransferCompletedEvent event,
            @Header(name = RabbitConfig.CORRELATION_HEADER, required = false) String correlationId) {

        boolean hasCorrelation = correlationId != null && !correlationId.isBlank();
        if (hasCorrelation) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        }
        try {
            log.info("NOTIFICACAO: transferencia concluida movementId={} from={} to={} amount={} em {}",
                    event.movementId(), event.fromAccountId(), event.toAccountId(),
                    event.amount(), event.createdAt());
        } finally {
            if (hasCorrelation) {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            }
        }
    }
}
