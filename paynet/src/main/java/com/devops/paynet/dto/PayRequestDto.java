package com.devops.paynet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;

public class PayRequestDto {

    @NotBlank(message = "Check ID is required")
    private String checkId;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "Fields are required")
    private Map<String, Object> fields;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    public PayRequestDto() {}

    public PayRequestDto(String checkId, String idempotencyKey, Map<String, Object> fields, Long amount) {
        this.checkId = checkId;
        this.idempotencyKey = idempotencyKey;
        this.fields = fields;
        this.amount = amount;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}
