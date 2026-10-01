package com.common.dto.demo_resource_service_dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkerScheduleDto {
    
    @Null(message = "ID розкладу має бути порожнім при створенні", groups = OnCreate.class)
    @NotNull(message = "ID розкладу є обов'язковим при оновленні", groups = OnUpdate.class)
    private Long id;

    @NotNull(message = "День тижня є обов'язковим", groups = {OnCreate.class, OnUpdate.class})
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Час початку є обов'язковим", groups = {OnCreate.class, OnUpdate.class})
    private LocalTime startTime;

    @NotNull(message = "Час завершення є обов'язковим", groups = {OnCreate.class, OnUpdate.class})
    private LocalTime endTime; 

    @Valid
    private List<ScheduleBreakDto> breaks = new ArrayList<>();
}