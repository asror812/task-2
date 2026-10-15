package com.devops.paylite.domain.enumeration;

/**
 * The PaymentStatus enumeration.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    PAID,
    FAILED,
    REVERSED,
    REFUNDED,
    RECONCILIATION_REQUIRED,
}
