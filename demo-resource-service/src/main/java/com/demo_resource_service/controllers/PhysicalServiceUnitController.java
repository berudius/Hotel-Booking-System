package com.demo_resource_service.controllers;

import com.common.dto.demo_resource_service_dto.PhysicalServiceUnitDto;
import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;
import com.demo_resource_service.services.PhysicalServiceUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/physical-units")
@RequiredArgsConstructor
public class PhysicalServiceUnitController {

    private final PhysicalServiceUnitService service;

    @GetMapping
    public ResponseEntity<List<PhysicalServiceUnitDto>> getAll(
            @PageableDefault(size = 4) Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PhysicalServiceUnitDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<PhysicalServiceUnitDto> create(
            @Validated(OnCreate.class) @RequestBody PhysicalServiceUnitDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PhysicalServiceUnitDto> update(
            @PathVariable Long id, 
            @Validated(OnUpdate.class) @RequestBody PhysicalServiceUnitDto details) {
        return ResponseEntity.ok(service.update(id, details));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}