package com.common.dto.demo_resource_service_dto;

import java.time.LocalTime;

import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleBreakDto {
    @NotNull(message = "Початок перерви обов'язковий", groups = {OnCreate.class, OnUpdate.class})
    private LocalTime breakStart;

    @NotNull(message = "Кінець перерви обов'язковий", groups = {OnCreate.class, OnUpdate.class})
    private LocalTime breakEnd;
}