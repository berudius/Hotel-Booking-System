package com.demo_hotel_service.controllers;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.common.dto.demo_hotel_service_dto.SpaUnitDto;
import com.common.security.AuthPrincipal;
import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;
import com.common.dto.demo_hotel_service_dto.ServiceUnitDto;
import com.common.dto.demo_hotel_service_dto.ServiceUnitShortDto;
import com.demo_hotel_service.data.models.hotel_offerings.spa.SpaUnit;
import com.demo_hotel_service.services.ServiceUnitService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class SpaController {
    private final ServiceUnitService spaService;

    @GetMapping("/api/spas")
    public ResponseEntity<List<ServiceUnitDto>> getSpas(@AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(spaService.getServiceUnits(principal, SpaUnit.class, PageRequest.of(page, size)));
    }

    @PostMapping("/api/spas")
    public ResponseEntity<ServiceUnitDto> addSpa(
            @Validated(OnCreate.class) @RequestPart("spa") SpaUnitDto spa,
            @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spaService.addServiceUnit(spa, imageFiles));
    }

    @PutMapping("/api/spas")
    public ResponseEntity<ServiceUnitDto> updateSpa(
            @Validated(OnUpdate.class) @RequestPart("spa") SpaUnitDto spa,
            @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {
        return ResponseEntity.ok(spaService.updateServiceUnit(spa, imageFiles));
    }

    @DeleteMapping("/api/spas/{id}")
    public ResponseEntity<?> deleteSpa(@PathVariable Long id) {
        return ResponseEntity.ok(spaService.deleteServiceUnit(id));
    }

    @GetMapping("/api/service-units/search")
    public ResponseEntity<List<ServiceUnitShortDto>> searchServiceUnits(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "") String category) {
        return ResponseEntity.ok(spaService.searchShortServiceUnits(q, category, 15));
    }

    @GetMapping("/api/service-units/short-by-ids")
    public ResponseEntity<List<ServiceUnitShortDto>> getServiceUnitsShortByIds(
            @RequestParam Set<Long> ids) {
        return ResponseEntity.ok(spaService.getShortSpaUnitsByIds(ids)); // Ваш метод з сервісу
    }
}