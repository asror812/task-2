package com.devops.paynet.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CheckResponseDto {
    private boolean valid;
    private String checkId;
    private Long recipientId;
    private String recipientName;
    private String paymentReference;
    private List<String> errors;

    public CheckResponseDto(boolean valid, String recipientName, Long recipientId, List<String> errors) {
        this.valid = valid;
        this.recipientName = recipientName;
        this.recipientId = recipientId;
        this.errors = errors;
    }

}
