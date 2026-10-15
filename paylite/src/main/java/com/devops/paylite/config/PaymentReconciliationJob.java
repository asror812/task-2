package com.devops.paylite.config;

import com.devops.paylite.service.PaymentTransactionService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentReconciliationJob.class);

    private final PaymentTransactionService paymentTransactionService;

    @Value("${payment.reconciliation-stale-after-ms:900000}")
    private long staleAfterMs;

    @Scheduled(fixedDelayString = "${payment.reconciliation-delay-ms:900000}")
    public void reconcile() {
        try {
            int recovered = paymentTransactionService.enqueueStalePaymentsForRecovery(
                Instant.now().minusMillis(staleAfterMs)
            );
            if (recovered > 0) {
                LOG.warn("Queued {} stale payments for safety reconciliation", recovered);
            }
        } catch (RuntimeException exception) {
            LOG.error("Payment reconciliation failed", exception);
        }
    }
}
