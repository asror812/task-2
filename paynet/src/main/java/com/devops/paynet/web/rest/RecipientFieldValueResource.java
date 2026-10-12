package com.devops.paynet.web.rest;

import com.devops.paynet.service.RecipientFieldValueService;
import com.devops.paynet.dto.RecipientFieldValueCreateDto;
import com.devops.paynet.dto.RecipientFieldValueResponseDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipient-field-values")
public class RecipientFieldValueResource {

    private final RecipientFieldValueService service;

    public RecipientFieldValueResource(RecipientFieldValueService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<RecipientFieldValueResponseDto> create(@Valid @RequestBody RecipientFieldValueCreateDto dto) {
        RecipientFieldValueResponseDto result = service.save(dto);
        return ResponseEntity.created(URI.create("/api/recipient-field-values/" + result.id())).body(result);
    }

    @GetMapping
    public List<RecipientFieldValueResponseDto> getOptions(
        @RequestParam Long recipientFieldId,
        @RequestParam(required = false) Long parentId
    ) {
        return service.findOptions(recipientFieldId, parentId);
    }

    @GetMapping("/{id}")
    public RecipientFieldValueResponseDto getOne(@PathVariable Long id) {
        return service.findOne(id);
    }
}
