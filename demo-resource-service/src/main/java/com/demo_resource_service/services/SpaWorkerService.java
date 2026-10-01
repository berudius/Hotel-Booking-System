package com.demo_resource_service.services;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.common.dto.demo_resource_service_dto.WorkerScheduleDto;
import com.common.dto.demo_resource_service_dto.ScheduleBreakDto;
import com.demo_resource_service.data.models.SpaWorker;
import com.common.dto.demo_resource_service_dto.SpaWorkerDto; 
import com.demo_resource_service.data.models.WorkerSchedule;
import com.demo_resource_service.exceptions.ResourceInUseException;
import com.demo_resource_service.exceptions.ScheduleConflictException;
import com.common.enums.AllocationStatus;
import com.demo_resource_service.data.models.AllocationResource;
import com.demo_resource_service.data.models.ScheduleBreak;
import com.demo_resource_service.repositories.AllocationResourceRepository;
import com.demo_resource_service.repositories.SpaWorkerRepository;
import com.demo_resource_service.repositories.WorkerScheduleRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpaWorkerService {
    private final SpaWorkerRepository repository;
    private final AllocationResourceRepository allocationRepository;
    private final WorkerScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public List<SpaWorkerDto> getAll(Pageable pageable) {
        return repository.findAll(pageable)
                .getContent()
                .stream()
                .map(this::mapWorkerToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpaWorkerDto getById(Integer id) {
        return mapWorkerToDto(getEntityById(id));
    }

    private SpaWorker getEntityById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Працівника не знайдено: " + id));
    }

    @Transactional
    public SpaWorkerDto create(SpaWorkerDto dto) {
        SpaWorker worker = mapWorkerToEntity(dto);
        SpaWorker saved = repository.save(worker);
        return mapWorkerToDto(saved);
    }

    @Transactional
    public SpaWorkerDto update(Integer id, SpaWorkerDto details) {
        SpaWorker existing = getEntityById(id);

        existing.setFirstName(details.getFirstName());
        existing.setLastName(details.getLastName());
        existing.setGender(details.getGender());
        existing.setStatus(details.getStatus());
        existing.setWorkPhoneNumber(details.getWorkPhoneNumber());
        existing.setCompetentSpaUnitIds(details.getCompetentSpaUnitIds());

        return mapWorkerToDto(repository.save(existing));
    }

    @Transactional
    public void delete(Integer id) {
        boolean inUse = allocationRepository.existsFutureActiveAllocationForWorker(
                id.intValue(), LocalDateTime.now(), AllocationStatus.ACTIVE);

        if (inUse) {
            throw new ResourceInUseException("Неможливо видалити/звільнити працівника. У нього є активні бронювання.");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<WorkerScheduleDto> getSchedulesByWorkerId(Integer workerId) {
        getEntityById(workerId);
        List<WorkerSchedule> schedules = scheduleRepository.findByWorkerIdOrderByDayOfWeekAsc(workerId);
        return schedules.stream().map(this::mapScheduleToDto).toList();
    }

    @Transactional
    public WorkerScheduleDto saveOrUpdateWeeklySchedule(Integer workerId, WorkerScheduleDto dto) {
        SpaWorker worker = getEntityById(workerId);
        
        // чи не конфліктує новий розклад з вже існуючими бронюваннями?
        validateScheduleChange(worker.getId(), dto);

        // очищаємо попередній графік на цей конкретний день тижня для цього працівника
        scheduleRepository.deleteByWorkerIdAndDayOfWeek(workerId, dto.getDayOfWeek());

        //  зберігаємо новий шаблон графіку
        WorkerSchedule schedule = new WorkerSchedule();
        schedule.setWorker(worker);
        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime()); 
        schedule.setEndTime(dto.getEndTime());     

        if (dto.getBreaks() != null) {
            List<ScheduleBreak> breaks = dto.getBreaks().stream().map(b -> {
                ScheduleBreak sb = new ScheduleBreak();
                sb.setBreakStart(b.getBreakStart());
                sb.setBreakEnd(b.getBreakEnd());
                return sb;
            }).toList();
            schedule.setBreaks(breaks);
        }

        return mapScheduleToDto(scheduleRepository.save(schedule));
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        scheduleRepository.deleteById(scheduleId);
    }

    private void validateScheduleChange(Integer workerId, WorkerScheduleDto newSchedule) {
        int dbDayIndex = newSchedule.getDayOfWeek().getValue(); 
        
        List<AllocationResource> futureAllocations = allocationRepository
                .findFutureActiveAllocationsForWorkerAndDay(workerId, LocalDateTime.now(), dbDayIndex);

        for (AllocationResource alloc : futureAllocations) {
            LocalTime allocStart = alloc.getStart().toLocalTime();
            LocalTime allocEnd = alloc.getTechnicalEnd().toLocalTime();

            if (allocStart.isBefore(newSchedule.getStartTime()) || allocEnd.isAfter(newSchedule.getEndTime())) {
                throw new ScheduleConflictException(String.format(
                    "Неможливо змінити графік. Існує бронювання на %s (Час: %s - %s), яке виходить за нові межі робочого дня.",
                    alloc.getStart().toLocalDate(), allocStart, allocEnd
                ));
            }

            if (newSchedule.getBreaks() != null) {
                for (ScheduleBreakDto b : newSchedule.getBreaks()) {
                    if (allocStart.isBefore(b.getBreakEnd()) && allocEnd.isAfter(b.getBreakStart())) {
                        throw new ScheduleConflictException(String.format(
                            "Неможливо додати перерву %s-%s. У працівника вже є бронювання %s о %s.",
                            b.getBreakStart(), b.getBreakEnd(), alloc.getStart().toLocalDate(), allocStart
                        ));
                    }
                }
            }
        }
    }

    private SpaWorkerDto mapWorkerToDto(SpaWorker entity) {
        SpaWorkerDto dto = new SpaWorkerDto();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setGender(entity.getGender());
        dto.setStatus(entity.getStatus());
        dto.setWorkPhoneNumber(entity.getWorkPhoneNumber());
        dto.setCompetentSpaUnitIds(entity.getCompetentSpaUnitIds());
        
        if (entity.getWorkSchedules() != null) {
             dto.setWorkSchedules(entity.getWorkSchedules().stream().map(this::mapScheduleToDto).toList());
        }
        return dto;
    }

    private SpaWorker mapWorkerToEntity(SpaWorkerDto dto) {
        SpaWorker entity = new SpaWorker();
        if(dto.getId() != null){
             entity.setId(dto.getId()); 
        }
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setGender(dto.getGender());
        entity.setStatus(dto.getStatus());
        entity.setWorkPhoneNumber(dto.getWorkPhoneNumber());
        entity.setCompetentSpaUnitIds(dto.getCompetentSpaUnitIds());
        return entity;
    }

    private WorkerScheduleDto mapScheduleToDto(WorkerSchedule entity) {
        WorkerScheduleDto dto = new WorkerScheduleDto();
        dto.setId(entity.getId());
        dto.setDayOfWeek(entity.getDayOfWeek()); 
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());    
        
        if (entity.getBreaks() != null) {
            dto.setBreaks(entity.getBreaks().stream().map(b -> {
                ScheduleBreakDto bd = new ScheduleBreakDto();
                bd.setBreakStart(b.getBreakStart());
                bd.setBreakEnd(b.getBreakEnd());
                return bd;
            }).toList());
        }
        return dto;
    }
}