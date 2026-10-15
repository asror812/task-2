package com.devops.paylite.service;

import com.devops.paylite.config.PaymentMessagingConfiguration;
import com.devops.paylite.domain.Payment;
import com.devops.paylite.domain.enumeration.PaymentStatus;
import com.devops.paylite.service.dto.payment.PaymentProcessMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMessageConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentMessageConsumer.class);

    private final PaymentService paymentService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentMessagePublisher messagePublisher;

    @Value("${payment.max-attempts:4}")
    private int maxAttempts;

    @RabbitListener(queues = PaymentMessagingConfiguration.PROCESS_QUEUE)
    public void process(PaymentProcessMessage message) {
        Payment payment = paymentService.processPendingPayment(message.paymentId());

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        if (payment.getAttemptCount() >= maxAttempts) {
            paymentTransactionService.markReconciliationRequired(payment.getId());

            LOG.warn("Payment is being moved to the dead letter queue id={} attempts={}", payment.getId(), payment.getAttemptCount());
            messagePublisher.publish(
                PaymentMessagingConfiguration.DEAD_LETTER_QUEUE,
                message
            );

            LOG.error("Payment requires manual reconciliation id={} attempts={}", payment.getId(), payment.getAttemptCount());
            return;
        }

        messagePublisher.publish(
            retryRoutingKey(payment.getAttemptCount()),
            message
        );
    }

    private String retryRoutingKey(int attemptCount) {
        return switch (attemptCount) {
            case 1 -> PaymentMessagingConfiguration.RETRY_10S_QUEUE;
            case 2 -> PaymentMessagingConfiguration.RETRY_30S_QUEUE;
            default -> PaymentMessagingConfiguration.RETRY_2M_QUEUE;
        };
    }
}
