package com.demo_resource_service.services;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.common.dto.demo_resource_service_dto.AllocationResourceDto;
import com.common.dto.demo_resource_service_dto.WorkerDataDto;
import com.common.enums.Gender;
import com.demo_resource_service.data.models.AllocationResource;
import com.common.enums.AllocationStatus;
import com.demo_resource_service.data.models.PhysicalServiceUnit;
import com.demo_resource_service.data.models.SpaWorker;
import com.demo_resource_service.exceptions.ResourceUnavailableException;
import com.demo_resource_service.exceptions.ScheduleConflictException;
import com.demo_resource_service.repositories.AllocationResourceRepository;
import com.demo_resource_service.repositories.PhysicalServiceUnitRepository;
import com.demo_resource_service.repositories.SpaWorkerRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AllocationService {

    private final PhysicalServiceUnitRepository physicalUnitRepository;
    private final AllocationResourceRepository allocationRepository;
    private final SpaWorkerRepository spaWorkerRepository;

    @Transactional(readOnly = true)
    public List<AllocationResourceDto> getAllocationsForRange(LocalDateTime start, LocalDateTime end) {
        return allocationRepository.findAllocationsInRange(start, end).stream()
                .map(a -> {
                    AllocationResourceDto dto = new AllocationResourceDto();
                    dto.setId(a.getId());
                    dto.setPhysicalServiceUnitId(
                            a.getPhysicalServiceUnit() != null ? a.getPhysicalServiceUnit().getId() : null);
                    dto.setAssignedWorkerIds(a.getAssignedWorkerIds());
                    dto.setStatus(a.getStatus());
                    dto.setGeneralBookingId(a.getGeneralBookingId());
                    dto.setServiceUnitId(a.getServiceUnitId());
                    dto.setStart(a.getStart());
                    dto.setEnd(a.getEnd());
                    dto.setTechnicalEnd(a.getTechnicalEnd());
                    return dto;
                })
                .toList();
    }

    @Transactional
    public List<AllocationResourceDto> allocate(List<AllocationResourceDto> requests) {
        List<AllocationResource> newAllocations = new ArrayList<>();

        Set<Long> usedPhysicalUnitIds = new HashSet<>(Set.of(-1L));
        Set<Integer> usedWorkerIds = new HashSet<>(Set.of(-1));

        for (AllocationResourceDto request : requests) {

            PhysicalServiceUnit targetUnit;

            if (request.getPhysicalServiceUnitId() != null) {
                targetUnit = physicalUnitRepository.findById(request.getPhysicalServiceUnitId())
                        .orElseThrow(() -> new ResourceUnavailableException(
                                "Вказане адміністратором приміщення не знайдено"));

                boolean hasOverlap = allocationRepository.existsOverlappingAllocation(
                        targetUnit.getId(),
                        -1L,
                        request.getStart(),
                        request.getEnd().plusMinutes(targetUnit.getCleaningTimeInMinutes()));

                if (hasOverlap) {
                    throw new ScheduleConflictException(
                            "Приміщення " + targetUnit.getPremisesNumber() + " вже зайняте в обраний час.");
                }
            } else {
                List<PhysicalServiceUnit> availableUnits = physicalUnitRepository.findAvailableUnits(
                        request.getServiceUnitId(),
                        request.getClientCount(),
                        request.getStart(),
                        request.getEnd(),
                        usedPhysicalUnitIds,
                        PageRequest.of(0, 1));

                if (availableUnits.isEmpty()) {
                    throw new ResourceUnavailableException(
                            "Немає вільних приміщень для ServiceUnitId: " + request.getServiceUnitId());
                }
                targetUnit = availableUnits.get(0);
            }

            usedPhysicalUnitIds.add(targetUnit.getId());

            LocalDateTime technicalEnd = request.getEnd().plusMinutes(targetUnit.getCleaningTimeInMinutes());

            Set<Integer> assignedWorkerIds = new HashSet<>();
            List<WorkerDataDto> assignedWorkerDtos = new ArrayList<>();

            if (request.getRequiresWorker()) {
                int requiredWorkersCount = request.getClientCount();

                if (request.getAssignedWorkerIds() != null && !request.getAssignedWorkerIds().isEmpty()) {
                    for (Integer workerId : request.getAssignedWorkerIds()) {
                        SpaWorker worker = spaWorkerRepository.findById(workerId)
                                .orElseThrow(() -> new ResourceUnavailableException(
                                        "Майстра з ID " + workerId + " не знайдено"));

                        assignedWorkerIds.add(worker.getId());
                        assignedWorkerDtos.add(new WorkerDataDto(worker.getId(), worker.getFirstName(),
                                worker.getLastName(), worker.getGender()));
                        usedWorkerIds.add(worker.getId());
                    }
                } else {
                    Gender specificGender = request.getPreferedGender();

                    DayOfWeek dayOfWeek = request.getStart().getDayOfWeek();
                    LocalTime startLocalTime = request.getStart().toLocalTime();
                    LocalTime endLocalTime = technicalEnd.toLocalTime();

                    for (int i = 0; i < requiredWorkersCount; i++) {
                        List<SpaWorker> availableWorkers = spaWorkerRepository.findAvailableWorkers(
                                request.getServiceUnitId(),
                                specificGender, 
                                dayOfWeek,
                                startLocalTime,
                                endLocalTime,
                                request.getStart(),
                                technicalEnd,
                                usedWorkerIds,
                                PageRequest.of(0, 1));

                        if (availableWorkers.isEmpty()) {
                            LocalTime breakEndTime = spaWorkerRepository.findOverlappingBreakEndTime(
                                    request.getServiceUnitId(), specificGender, dayOfWeek, startLocalTime,
                                    endLocalTime);

                            if (breakEndTime != null) {
                                String formattedTime = String.format("%02d:%02d", breakEndTime.getHour(),
                                        breakEndTime.getMinute() + 5);
                                throw new ScheduleConflictException(
                                        "Час збігається з перервою персоналу. Оберіть час після " + formattedTime);
                            }

                            String genderMsg = specificGender != null ? " (" + specificGender + ")" : "";
                            throw new ResourceUnavailableException(
                                    "Недостатньо вільних майстрів" + genderMsg + " на обраний час.");
                        }

                        SpaWorker foundWorker = availableWorkers.get(0);
                        usedWorkerIds.add(foundWorker.getId());
                        assignedWorkerIds.add(foundWorker.getId());
                        assignedWorkerDtos.add(new WorkerDataDto(foundWorker.getId(), foundWorker.getFirstName(),
                                foundWorker.getLastName(), foundWorker.getGender()));
                    }
                }
            }

            AllocationResource allocation = new AllocationResource();
            allocation.setPhysicalServiceUnit(targetUnit);
            allocation.setGeneralBookingId(request.getGeneralBookingId());
            allocation.setBookingUnitId(request.getBookingUnitId());
            allocation.setServiceUnitId(request.getServiceUnitId());
            allocation.setStart(request.getStart());
            allocation.setEnd(request.getEnd());
            allocation.setTechnicalEnd(technicalEnd);
            allocation.setClientCount(request.getClientCount());
            allocation.setPreferedGender(request.getPreferedGender());
            allocation.setAssignedWorkerIds(assignedWorkerIds);
            allocation.setStatus(AllocationStatus.ACTIVE);

            newAllocations.add(allocation);

            request.setTechnicalEnd(technicalEnd);
            request.setPhysicalServiceUnitId(targetUnit.getId());
            request.setAssignedWorkerIds(assignedWorkerIds);
            request.setAssignedWorkers(assignedWorkerDtos);
        }

        allocationRepository.saveAll(newAllocations);
        return requests;
    }

    @Transactional
    public void updateAllocationDates(Long bookingUnitId, LocalDateTime newStart, LocalDateTime newEnd) {
        AllocationResource allocation = allocationRepository
                .findByBookingUnitIdAndStatus(bookingUnitId, AllocationStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Активну алокацію не знайдено для BookingUnitId: " + bookingUnitId));

        PhysicalServiceUnit physicalUnit = allocation.getPhysicalServiceUnit();
        LocalDateTime technicalNewEndTime = newEnd.plusMinutes(physicalUnit.getCleaningTimeInMinutes());

        boolean hasRoomOverlap = allocationRepository.existsOverlappingAllocation(
                physicalUnit.getId(),
                allocation.getId(),
                newStart,
                technicalNewEndTime);

        if (hasRoomOverlap) {
            throw new IllegalStateException("Неможливо змінити час: приміщення №" + physicalUnit.getPremisesNumber()
                    + " вже зайняте іншим клієнтом в цей період.");
        }

        Set<Integer> assignedWorkers = allocation.getAssignedWorkerIds();
        if (assignedWorkers != null && !assignedWorkers.isEmpty()) {
            
            DayOfWeek dayOfWeek = newStart.getDayOfWeek();
            LocalTime startLocalTime = newStart.toLocalTime();
            LocalTime endLocalTime = technicalNewEndTime.toLocalTime();

            boolean workersAvailable = spaWorkerRepository.areSpecificWorkersAvailableForNewTime(
                    assignedWorkers,
                    (long) assignedWorkers.size(),
                    allocation.getId(),
                    dayOfWeek,
                    startLocalTime,
                    endLocalTime,
                    newStart,
                    technicalNewEndTime
            );

            if (!workersAvailable) {
                throw new IllegalStateException("Неможливо змінити час: призначені майстри не працюють у новий час, або вже зайняті іншим клієнтом.");
            }

            boolean breaksOverlap = spaWorkerRepository.hasOverlappingBreaksForSpecificWorkers(
                    assignedWorkers,
                    dayOfWeek,
                    startLocalTime,
                    endLocalTime
            );

            if (breaksOverlap) {
                throw new IllegalStateException("Неможливо змінити час: новий час збігається з перервою призначеного майстра.");
            }
        }

        allocation.setStart(newStart);
        allocation.setEnd(newEnd);
        allocation.setTechnicalEnd(technicalNewEndTime);
        allocationRepository.save(allocation);
    }
    
    @Transactional
    public void updateAllocationEndTime(Long bookingUnitId, LocalDateTime newEndTime) {
        AllocationResource allocation = allocationRepository.findByBookingUnitId(bookingUnitId)
                .orElseThrow(() -> new EntityNotFoundException("Алокацію не знайдено для BookingUnitId: " + bookingUnitId));

        PhysicalServiceUnit physicalUnit = allocation.getPhysicalServiceUnit();
        LocalDateTime technicalNewEndTime = newEndTime.plusMinutes(physicalUnit.getCleaningTimeInMinutes());

        boolean hasOverlap = allocationRepository.existsOverlappingAllocation(
                physicalUnit.getId(),
                allocation.getId(),
                allocation.getStart(),
                technicalNewEndTime);

        if (hasOverlap) {
            throw new ScheduleConflictException(
                    "Неможливо змінити час: існує перетин з наступним бронюванням цієї кімнати.");
        }

        allocation.setEnd(newEndTime);
        allocation.setTechnicalEnd(technicalNewEndTime);
        allocationRepository.save(allocation);
    }

    @Transactional
    public void updateStatusByGeneralBooking(Long generalBookingId, AllocationStatus newStatus) {
        List<AllocationResource> allocations = allocationRepository.findByGeneralBookingIdAndStatus(generalBookingId,
                AllocationStatus.ACTIVE);

        if (!allocations.isEmpty()) {
            allocations.forEach(alloc -> alloc.setStatus(newStatus));
            allocationRepository.saveAll(allocations);
        }
    }

    @Transactional
    public void updateStatusByBookingUnit(Long bookingUnitId, AllocationStatus newStatus) {
        allocationRepository.findByBookingUnitIdAndStatus(bookingUnitId, AllocationStatus.ACTIVE)
                .ifPresent(allocation -> {
                    allocation.setStatus(newStatus);
                    allocationRepository.save(allocation);
                });
    }
}