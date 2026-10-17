package com.devops.paylite.service.dto.paynet;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaynetCheckResponseDto {

    private boolean valid;
    private String checkId;
    private Long recipientId;
    private String recipientName;
    private String paymentReference;
    private List<String> errors;
}
