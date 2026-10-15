package com.devops.paylite.config;

import com.devops.paylite.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentReconciliationJob.class);

    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "${payment.reconciliation-delay-ms:60000}")
    public void reconcile() {
        try {
            paymentService.reconcilePendingPayments();
        } catch (RuntimeException exception) {
            LOG.error("Payment reconciliation failed", exception);
        }
    }
}
