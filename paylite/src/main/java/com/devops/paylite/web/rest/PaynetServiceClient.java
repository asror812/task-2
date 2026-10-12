package com.devops.paylite.web.rest;

import java.util.List;
import com.devops.paylite.service.dto.load.PaynetCategoryLoadDto;
import com.devops.paylite.service.dto.load.PaynetRecipientLoadDto;
import com.devops.paylite.service.dto.load.PaynetRecipientFieldLoadDto;
import com.devops.paylite.service.dto.paynet.PaynetCheckRequestDto;
import com.devops.paylite.service.dto.paynet.PaynetCheckResponseDto;
import com.devops.paylite.service.dto.paynet.PaynetPayRequestDto;
import com.devops.paylite.service.dto.paynet.PaynetPayResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "paynet")
public interface PaynetServiceClient {

    @GetMapping("/api/recipients")
    List<PaynetRecipientLoadDto> getRecipientPage(
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );

    @GetMapping("/api/recipient-fields")
    List<PaynetRecipientFieldLoadDto> getRecipientFields(
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );

    @PostMapping("/api/recipients/{id}/check")
    PaynetCheckResponseDto check(
        @PathVariable("id") Long id,
        @RequestBody PaynetCheckRequestDto checkCreateDto);

    @PostMapping("/api/recipients/{id}/pay")
    PaynetPayResponseDto pay(
        @PathVariable("id") Long id,
        @RequestBody PaynetPayRequestDto payRequestDto);

    @GetMapping("/api/categories")
    List<PaynetCategoryLoadDto> getCategoryPage(
        @RequestParam("page") int page,
        @RequestParam("size") int size
    );
}
