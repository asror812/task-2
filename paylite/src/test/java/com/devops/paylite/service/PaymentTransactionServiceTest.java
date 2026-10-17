package com.devops.paylite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.devops.paylite.domain.Agent;
import com.devops.paylite.domain.Payment;
import com.devops.paylite.repository.AgentRepository;
import com.devops.paylite.repository.PaymentOutboxEventRepository;
import com.devops.paylite.repository.PaymentRepository;
import com.devops.paylite.service.dto.payment.PaymentCreateRequestDto;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private PaymentOutboxEventRepository outboxEventRepository;

    @InjectMocks
    private PaymentTransactionService service;

    private final PaymentCreateRequestDto request = new PaymentCreateRequestDto("idempotency-1", "check-1");

    @Test
    void returnsReplayWhenConstraintViolationWasCausedByConcurrentIdempotentRequest() {
        Payment existing = Payment.builder()
            .idempotencyKey("idempotency-1")
            .checkId("check-1")
            .agent(Agent.builder().login("agent-1").build())
            .build();
        when(paymentRepository.findByIdempotencyKey("idempotency-1")).thenReturn(Optional.of(existing));

        Payment result = service.resolveCreationConstraintViolation(
            request,
            "agent-1",
            new DataIntegrityViolationException("duplicate idempotency key")
        );

        assertThat(result).isSameAs(existing);
    }

    @Test
    void reportsCheckConflictWhenConstraintViolationWasCausedByReusedCheck() {
        DataIntegrityViolationException cause = new DataIntegrityViolationException("duplicate check ID");
        when(paymentRepository.findByIdempotencyKey("idempotency-1")).thenReturn(Optional.empty());
        when(paymentRepository.findByCheckId("check-1")).thenReturn(Optional.of(Payment.builder().build()));

        assertThatThrownBy(() -> service.resolveCreationConstraintViolation(request, "agent-1", cause))
            .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(exception.getReason()).isEqualTo("Check ID already used for another payment");
                assertThat(exception.getCause()).isSameAs(cause);
            });
    }

    @Test
    void rethrowsUnrelatedDatabaseConstraintViolation() {
        DataIntegrityViolationException cause = new DataIntegrityViolationException("foreign key violation");
        when(paymentRepository.findByIdempotencyKey("idempotency-1")).thenReturn(Optional.empty());
        when(paymentRepository.findByCheckId("check-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveCreationConstraintViolation(request, "agent-1", cause)).isSameAs(cause);
    }
}
