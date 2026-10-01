package com.demo_hotel_service.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.common.dto.demo_hotel_service_dto.BookingUnitDto;
import com.common.dto.demo_hotel_service_dto.GeneralBookingDto;
import com.common.dto.demo_resource_service_dto.AllocationResourceDto;
import com.common.enums.BookingStatus;
import com.common.security.AuthPrincipal;
import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;
import com.demo_hotel_service.services.BookingUnitService;
import com.demo_hotel_service.services.GeneralBookingService;


import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final GeneralBookingService gBookingService;
    private final BookingUnitService bookingUnitService;

    @GetMapping("/api/bookings")
public ResponseEntity<List<GeneralBookingDto>> getMyBookings(
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "12") Integer size,
        @RequestParam(defaultValue = "true") boolean showAll,
        @AuthenticationPrincipal AuthPrincipal userDetails) {
    return ResponseEntity.ok(gBookingService.getGeneralBookingsOfUser(userDetails, showAll, PageRequest.of(page, size)));
}

    @GetMapping("/api/admin/bookings")
    public ResponseEntity<List<GeneralBookingDto>> getAllBookings(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(defaultValue = "true") boolean showAll
        ) {
        return ResponseEntity.ok(gBookingService.getGeneralBookingsForAdmin(showAll, PageRequest.of(page, size)));
    }

    @GetMapping("/api/bookings/{id}")
    public ResponseEntity<GeneralBookingDto> getGeneralBookingById(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(gBookingService.getById(id));
    }

    @PostMapping("/api/bookings")
    public ResponseEntity<GeneralBookingDto > addGeneralBooking(@Validated(OnCreate.class) @RequestBody GeneralBookingDto dto, @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(gBookingService.addGeneralBooking(dto, principal));
    }

    @PutMapping("/api/bookings/{id}")
    public ResponseEntity<GeneralBookingDto> updateGeneralBooking(
            @PathVariable Long id,
            @Validated(OnUpdate.class) @RequestBody GeneralBookingDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(gBookingService.updateGeneralBooking(dto));
    }

    @GetMapping("/api/bookings/{generalBookingId}/units")
    public ResponseEntity<List<BookingUnitDto>> getBookingUnits(
            @PathVariable Long generalBookingId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "12") Integer size) {
        return ResponseEntity.ok(bookingUnitService.getBookingUnits(generalBookingId, PageRequest.of(page, size)));
    }

    @GetMapping("/api/bookings/units/{unitId}")
    public ResponseEntity<BookingUnitDto> getBookingUnitById(@PathVariable @NonNull Long unitId) {
        return ResponseEntity.ok(bookingUnitService.getBookingUnitById(unitId));
    }

    // Для внутрішнього виклику з User Service після реєстрації
    @PutMapping("/api/internal/bookings/claim")
    public ResponseEntity<Void> claimOrphanBookings(
            @RequestParam String phoneNumber,
            @RequestParam Long userId) {
        gBookingService.claimOrphanBookings(phoneNumber, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/bookings/claim-all")
    public ResponseEntity<?> claimAllMyBookings(@AuthenticationPrincipal AuthPrincipal userDetails) {
        
        if (userDetails == null || userDetails.getPhoneNumber() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{error: Номер телефону не знайдено у профілі.}");
        }
        
        // Викликаємо метод для масового оновлення
        gBookingService.claimOrphanBookings(userDetails.getPhoneNumber(), userDetails.getId());
        
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/api/bookings/{id}/status")
    @PreAuthorize("@securityCheck.isGeneralBookingOwner(#id, principal) or hasRole('ADMIN')")
    public ResponseEntity<GeneralBookingDto> updateGeneralBookingStatus(
            @PathVariable Long id,
            @RequestParam BookingStatus status,
            @RequestBody(required = false) List<AllocationResourceDto> manualAllocations,
            @AuthenticationPrincipal AuthPrincipal principal) {
        
        GeneralBookingDto updated = gBookingService.updateGeneralBookingStatus(id, status, manualAllocations, principal);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/api/bookings/{id}/units")
    @PreAuthorize("@securityCheck.isGeneralBookingOwner(#id, principal) or hasRole('ADMIN')")
    public ResponseEntity<List<BookingUnitDto>> addUnitsToExistingBooking(
            @PathVariable Long id,
            @RequestBody List<BookingUnitDto> newUnits,
            @AuthenticationPrincipal AuthPrincipal principal) {
        
        List<BookingUnitDto> savedUnits = bookingUnitService.addBookingUnits(id, newUnits);
        return ResponseEntity.ok(savedUnits);
    }


    @PatchMapping("/api/bookings/units/{unitId}/status")
    public ResponseEntity<BookingUnitDto> updateBookingUnitStatus(
            @PathVariable Long unitId,
            @RequestParam BookingStatus status,
            @AuthenticationPrincipal AuthPrincipal principal) {

        BookingUnitDto updated = bookingUnitService.updateBookingUnitStatusOnly(unitId, status, principal);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/api/bookings/units/{unitId}/dates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingUnitDto> updateBookingUnitDates(
            @PathVariable Long unitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        BookingUnitDto updated = bookingUnitService.updateBookingUnitDates(unitId, start, end);
        return ResponseEntity.ok(updated);
    }
}
