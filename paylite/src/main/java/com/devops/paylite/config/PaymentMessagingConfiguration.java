package com.devops.paylite.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentMessagingConfiguration {

    public static final String EXCHANGE = "payment.exchange";
    public static final String PROCESS_QUEUE = "payment.process";
    public static final String PROCESS_ROUTING_KEY = "payment.process";
    public static final String RETRY_10S_QUEUE = "payment.retry.10s";
    public static final String RETRY_30S_QUEUE = "payment.retry.30s";
    public static final String RETRY_2M_QUEUE = "payment.retry.2m";
    public static final String DEAD_LETTER_QUEUE = "payment.dlq";

    @Bean
    DirectExchange paymentExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue paymentProcessQueue() {
        return QueueBuilder.durable(PROCESS_QUEUE)
            .deadLetterExchange(EXCHANGE)
            .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
            .build();
    }

    @Bean
    Binding paymentProcessBinding(DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentProcessQueue())
            .to(paymentExchange)
            .with(PROCESS_ROUTING_KEY);
    }

    @Bean
    Queue paymentRetry10sQueue() {
        return retryQueue(RETRY_10S_QUEUE, 10_000);
    }

    @Bean
    Queue paymentRetry30sQueue() {
        return retryQueue(RETRY_30S_QUEUE, 30_000);
    }

    @Bean
    Queue paymentRetry2mQueue() {
        return retryQueue(RETRY_2M_QUEUE, 120_000);
    }

    @Bean
    Declarables paymentRetryBindings(DirectExchange paymentExchange) {
        return new Declarables(
            BindingBuilder.bind(paymentRetry10sQueue()).to(paymentExchange).with(RETRY_10S_QUEUE),
            BindingBuilder.bind(paymentRetry30sQueue()).to(paymentExchange).with(RETRY_30S_QUEUE),
            BindingBuilder.bind(paymentRetry2mQueue()).to(paymentExchange).with(RETRY_2M_QUEUE)
        );
    }

    @Bean
    Queue paymentDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding paymentDeadLetterBinding(DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentDeadLetterQueue()).to(paymentExchange).with(DEAD_LETTER_QUEUE);
    }

    @Bean
    MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        MessageConverter rabbitMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(4);
        factory.setPrefetchCount(10);
        return factory;
    }

    private Queue retryQueue(String name, int delayMs) {
        return QueueBuilder.durable(name)
            .ttl(delayMs)
            .deadLetterExchange(EXCHANGE)
            .deadLetterRoutingKey(PROCESS_ROUTING_KEY)
            .build();
    }
}
