package com.demo_hotel_service.controllers;

import java.util.List;

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

import com.common.dto.demo_hotel_service_dto.RoomUnitDto;
import com.common.dto.demo_hotel_service_dto.ServiceUnitDto;
import com.common.security.AuthPrincipal;
import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;
import com.demo_hotel_service.data.models.hotel_offerings.rooms.RoomUnit;
import com.demo_hotel_service.services.ServiceUnitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RoomController {
    private final ServiceUnitService roomService;

    @GetMapping("/api/rooms")
    public ResponseEntity<List<ServiceUnitDto>> getRooms(@AuthenticationPrincipal AuthPrincipal principal, @RequestParam Integer page, @RequestParam Integer size){
        return ResponseEntity.ok(roomService.getServiceUnits(principal, RoomUnit.class, PageRequest.of(page, size)));
    }

    @PostMapping("/api/rooms")
    public ResponseEntity<ServiceUnitDto> addRoom(
            @Validated(OnCreate.class) @RequestPart("room") RoomUnitDto room,
            @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.addServiceUnit(room, imageFiles));
    }

    @PutMapping("/api/rooms")
    public ResponseEntity<ServiceUnitDto> updateRoom(
            @Validated(OnUpdate.class) @RequestPart("room") RoomUnitDto room,
            @RequestPart(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {
        return ResponseEntity.ok(roomService.updateServiceUnit(room, imageFiles));
    }

    @DeleteMapping("/api/rooms/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.deleteServiceUnit(id));
    }
}
