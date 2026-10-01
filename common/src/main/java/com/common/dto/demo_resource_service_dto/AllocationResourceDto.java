package com.common.dto.demo_resource_service_dto;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import com.common.enums.AllocationStatus;
import com.common.enums.Gender;

@Getter
@Setter
public class AllocationResourceDto {
    
    private Long id;
    private Long physicalServiceUnitId;

    @NotNull(message = "generalBookingId обов'язковий")
    private Long generalBookingId;

    @NotNull(message = "bookingUnitId обов'язковий")
    private Long bookingUnitId;

    @NotNull(message = "serviceUnitId обов'язковий")
    private Long serviceUnitId;

    @NotNull(message = "Час початку обов'язковий")
    private LocalDateTime start;

    @NotNull(message = "Час завершення обов'язковий")
    private LocalDateTime end; 
    private LocalDateTime technicalEnd;  

    @NotNull(message = "Кількість клієнтів обов'язкова")
    @Min(value = 1, message = "Мінімум 1 клієнт")
    private Integer clientCount;

    private Gender preferedGender; 
    private Boolean requiresWorker;
    private Set<Integer> assignedWorkerIds;
    private List<WorkerDataDto> assignedWorkers;
    private AllocationStatus status; 
}




