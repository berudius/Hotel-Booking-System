package com.demo_resource_service.controllers;

import com.common.dto.demo_resource_service_dto.SpaWorkerDto;
import com.common.dto.demo_resource_service_dto.WorkerScheduleDto;
import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;
import com.demo_resource_service.services.SpaWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spa-workers")
@RequiredArgsConstructor
public class SpaWorkerController {

    private final SpaWorkerService service;

    @GetMapping
    public ResponseEntity<List<SpaWorkerDto>> getAll(
            @PageableDefault(size = 4) Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpaWorkerDto> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<SpaWorkerDto> create(
            @Validated(OnCreate.class) @RequestBody SpaWorkerDto workerDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(workerDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpaWorkerDto> update(
            @PathVariable Integer id, 
            @Validated(OnUpdate.class) @RequestBody SpaWorkerDto detailsDto) {
        return ResponseEntity.ok(service.update(id, detailsDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workerId}/schedules")
    public ResponseEntity<List<WorkerScheduleDto>> getSchedules(@PathVariable Integer workerId) {
        return ResponseEntity.ok(service.getSchedulesByWorkerId(workerId));
    }

    @PostMapping("/{workerId}/schedules")
    public ResponseEntity<WorkerScheduleDto> addSchedule(
            @PathVariable Integer workerId, 
            @Validated(OnCreate.class) @RequestBody WorkerScheduleDto scheduleDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveOrUpdateWeeklySchedule(workerId, scheduleDto));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId) {
        service.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }
}