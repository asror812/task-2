package com.devops.paylite.service;

import com.devops.paylite.config.PaymentMessagingConfiguration;
import com.devops.paylite.service.dto.payment.PaymentProcessMessage;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String routingKey, PaymentProcessMessage message) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());

        rabbitTemplate.convertAndSend(
            PaymentMessagingConfiguration.EXCHANGE,
            routingKey,
            message,
            correlationData
        );

        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                throw new IllegalStateException("RabbitMQ rejected message: " + confirm.getReason());
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for RabbitMQ confirmation", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not confirm RabbitMQ message", exception);
        }
    }
}
