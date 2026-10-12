package com.devops.paynet.dto;

import java.math.BigDecimal;
import java.util.List;

public class CheckResponseDto {
    private boolean valid;
    private String checkId;
    private Long recipientId;
    private String recipientName;
    private List<String> errors;

    public CheckResponseDto() {
    }

    public CheckResponseDto(boolean valid, String recipientName, Long recipientId, List<String> errors) {
        this.valid = valid;
        this.recipientName = recipientName;
        this.recipientId = recipientId;
        this.errors = errors;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
