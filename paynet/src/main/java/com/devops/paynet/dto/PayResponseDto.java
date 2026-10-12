package com.devops.paynet.dto;

import java.util.List;

public class PayResponseDto {

    private boolean paid;
    private String paymentId;
    private Long recipientId;
    private String recipientName;
    private List<String> errors;

    public PayResponseDto() {}

    public PayResponseDto(boolean paid, String paymentId, Long recipientId, String recipientName, List<String> errors) {
        this.paid = paid;
        this.paymentId = paymentId;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.errors = errors;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
