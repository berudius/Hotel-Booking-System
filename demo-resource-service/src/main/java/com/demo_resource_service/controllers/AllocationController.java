package com.demo_resource_service.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.common.dto.demo_resource_service_dto.AllocationResourceDto;
import com.common.enums.AllocationStatus;
import com.demo_resource_service.services.AllocationService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@Validated
public class AllocationController {

    private final AllocationService allocationService;

    @PostMapping("/api/allocations/allocate")
    public ResponseEntity<List<AllocationResourceDto>> allocateResources(
            @Valid @RequestBody List<AllocationResourceDto> requests) {
        List<AllocationResourceDto> allocated = allocationService.allocate(requests);
        return ResponseEntity.ok(allocated);
    }

    @PutMapping("/api/allocations/booking-unit/{bookingUnitId}/dates")
    public ResponseEntity<Void> updateAllocationDates(
            @PathVariable Long bookingUnitId,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, fallbackPatterns = {
                    "dd/MM/yyyy, HH:mm", "dd.MM.yyyy HH:mm", "yyyy-MM-dd'T'HH:mm:ss" }) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, fallbackPatterns = {
                    "dd/MM/yyyy, HH:mm", "dd.MM.yyyy HH:mm", "yyyy-MM-dd'T'HH:mm:ss" }) LocalDateTime end) {

        allocationService.updateAllocationDates(bookingUnitId, start, end);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/allocations/booking-unit/{bookingUnitId}/end-time")
    public ResponseEntity<?> updateAllocationEndTime(
        @PathVariable 
            Long bookingUnitId,
        @RequestParam("end") 
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, fallbackPatterns = {
            "dd/MM/yyyy, HH:mm", "dd.MM.yyyy HH:mm", "yyyy-MM-dd'T'HH:mm:ss" 
        })
            LocalDateTime end
        ){
        allocationService.updateAllocationEndTime(null, null);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/api/allocations/general-booking/{generalBookingId}/status")
    public ResponseEntity<Void> updateStatusByGeneralBooking(
            @PathVariable Long generalBookingId,
            @RequestParam("status") AllocationStatus status) {
        allocationService.updateStatusByGeneralBooking(generalBookingId, status);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/allocations/booking-unit/{bookingUnitId}/status")
    public ResponseEntity<Void> updateStatusByBookingUnit(
            @PathVariable Long bookingUnitId,
            @RequestParam("status") AllocationStatus status) {
        allocationService.updateStatusByBookingUnit(bookingUnitId, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/allocations/calendar")
    public ResponseEntity<List<AllocationResourceDto>> getCalendarAllocations(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, fallbackPatterns = {
                    "dd/MM/yyyy, HH:mm", "dd.MM.yyyy HH:mm" }) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME, fallbackPatterns = {
                    "dd/MM/yyyy, HH:mm", "dd.MM.yyyy HH:mm" }) LocalDateTime end) {

        List<AllocationResourceDto> allocations = allocationService.getAllocationsForRange(start, end);
        return ResponseEntity.ok(allocations);
    }
}