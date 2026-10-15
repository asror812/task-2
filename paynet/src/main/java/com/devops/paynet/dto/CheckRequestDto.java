package com.devops.paynet.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

public class CheckRequestDto {
    @NotEmpty(message = "Fields cannot be empty")
    private Map<String, Object> fields;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amount;

    public CheckRequestDto() {
    }

    public CheckRequestDto(Map<String, Object> fields, Long amount) {
        this.fields = fields;
        this.amount = amount;
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
