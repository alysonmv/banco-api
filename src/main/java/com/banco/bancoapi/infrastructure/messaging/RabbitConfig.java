package com.banco.bancoapi.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Value("${banco.messaging.exchange}")
    private String exchange;

    @Value("${banco.messaging.queue}")
    private String queue;

    @Value("${banco.messaging.routing-key}")
    private String routingKey;

    @Bean
    TopicExchange transfersExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    Queue notificationsQueue() {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    Binding notificationsBinding(Queue notificationsQueue, TopicExchange transfersExchange) {
        return BindingBuilder.bind(notificationsQueue).to(transfersExchange).with(routingKey);
    }

    /** Mensagens em JSON (o Boot aplica no RabbitTemplate e nos listeners). */
    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
