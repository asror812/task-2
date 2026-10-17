package com.devops.paylite.web.rest;

import com.devops.paylite.config.PaymentClientConfig;
import com.devops.paylite.service.dto.load.PaynetCatalogEntryLoadDto;
import com.devops.paylite.service.dto.paynet.PaynetCheckDetailsDto;
import com.devops.paylite.service.dto.paynet.PaynetCheckRequestDto;
import com.devops.paylite.service.dto.paynet.PaynetCheckResponseDto;
import com.devops.paylite.service.dto.paynet.PaynetPayRequestDto;
import com.devops.paylite.service.dto.paynet.PaynetPayResponseDto;
import com.devops.paylite.service.dto.paynet.PaynetPaymentStatusResponseDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "paynet", configuration = PaymentClientConfig.class)
public interface PaynetServiceClient {
    @GetMapping("/api/recipients/catalog")
    List<PaynetCatalogEntryLoadDto> getCatalog();

    @PostMapping("/api/recipients/{id}/check")
    PaynetCheckResponseDto check(@PathVariable("id") Long id, @RequestBody PaynetCheckRequestDto checkCreateDto);

    @GetMapping("/api/recipients/checks/{checkId}")
    PaynetCheckDetailsDto getCheckDetails(@PathVariable("checkId") String checkId);

    @PostMapping("/api/recipients/{id}/pay")
    PaynetPayResponseDto pay(@PathVariable("id") Long id, @RequestBody PaynetPayRequestDto payRequestDto);

    @GetMapping("/api/recipients/{id}/pay/status")
    PaynetPaymentStatusResponseDto getPaymentStatus(@PathVariable("id") Long id, @RequestParam("idempotencyKey") String idempotencyKey);
}
