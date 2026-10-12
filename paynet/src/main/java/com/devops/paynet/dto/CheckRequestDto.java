package com.devops.paynet.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public class CheckRequestDto {
    @NotEmpty(message = "Fields cannot be empty")
    private Map<String, Object> fields;

    public CheckRequestDto() {
    }

    public CheckRequestDto(Map<String, Object> fields) {
        this.fields = fields;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }
}
