package com.common.dto.demo_resource_service_dto;

import java.util.Set;

import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhysicalServiceUnitDto {
    
    @Null(message = "ID має бути порожнім при створенні", groups = OnCreate.class)
    @NotNull(message = "ID є обов'язковим при оновленні", groups = OnUpdate.class)
    private Long id;

    private Set<Long> serviceUnitIds;

    @NotBlank(message = "Номер приміщення не може бути порожнім", groups = {OnCreate.class, OnUpdate.class})
    @NotNull(message = "Номер приміщення не може бути null", groups = {OnCreate.class, OnUpdate.class})
    private String premisesNumber; 

    @NotNull(message = "Місткість клієнтів є обов'язковою", groups = {OnCreate.class, OnUpdate.class})
    @Min(value = 1, message = "Місткість клієнтів має бути мінімум 1", groups = {OnCreate.class, OnUpdate.class})
    private Integer clientCapacity;

    @NotNull(message = "Час прибирання є обов'язковим", groups = {OnCreate.class, OnUpdate.class})
    @Min(value = 0, message = "Час прибирання не може бути від'ємним", groups = {OnCreate.class, OnUpdate.class})
    private Integer cleaningTimeInMinutes;

    @NotNull(message = "Статус outOfService має бути вказаний", groups = {OnCreate.class, OnUpdate.class})
    private Boolean outOfService;
}



