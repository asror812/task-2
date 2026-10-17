package com.devops.paylite.service;

import com.devops.paylite.config.PaymentMessagingConfiguration;
import com.devops.paylite.domain.PaymentOutboxEvent;
import com.devops.paylite.repository.PaymentOutboxEventRepository;
import com.devops.paylite.service.dto.payment.PaymentProcessMessage;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentOutboxPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentOutboxPublisher.class);

    private final PaymentOutboxEventRepository outboxEventRepository;
    private final PaymentMessagePublisher messagePublisher;

    @Value("${payment.outbox-batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${payment.outbox-publish-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        int safeBatchSize = Math.max(1, batchSize);

        for (PaymentOutboxEvent event : outboxEventRepository.findUnpublishedForUpdate(PageRequest.of(0, safeBatchSize))) {
            if (!publish(event)) {
                break;
            }
        }
    }

    private boolean publish(PaymentOutboxEvent event) {
        event.setPublishAttempts(event.getPublishAttempts() + 1);

        try {
            messagePublisher.publish(
                PaymentMessagingConfiguration.PROCESS_ROUTING_KEY,
                new PaymentProcessMessage(event.getPaymentId())
            );

            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            return true;
        } catch (Exception exception) {
            event.setLastError(limit(exception.getMessage()));
            LOG.warn("Could not publish payment outbox event id={}", event.getId(), exception);
            return false;
        }
    }

    private String limit(String error) {
        if (error == null || error.isBlank()) {
            return "Unknown RabbitMQ publishing error";
        }
        return error.length() <= 1000 ? error : error.substring(0, 1000);
    }
}
