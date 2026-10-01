package com.common.dto.demo_resource_service_dto;

import java.util.List;
import java.util.Set;

import com.common.enums.Gender;
import com.common.enums.WorkerStatus;
import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpaWorkerDto {
    
    @Null(message = "ID має бути порожнім при створенні", groups = OnCreate.class)
    @NotNull(message = "ID є обов'язковим при оновленні", groups = OnUpdate.class)
    private Integer id;

    @NotBlank(message = "Ім'я не може бути порожнім", groups = {OnCreate.class, OnUpdate.class})
    private String firstName;

    @NotBlank(message = "Прізвище не може бути порожнім", groups = {OnCreate.class, OnUpdate.class})
    private String lastName;

    @NotNull(message = "Стать є обов'язковою", groups = {OnCreate.class, OnUpdate.class})
    private Gender gender;

    @NotNull(message = "Статус працівника є обов'язковим", groups = {OnCreate.class, OnUpdate.class})
    private WorkerStatus status;

    @Valid 
    private List<WorkerScheduleDto> workSchedules;

    @NotBlank(message = "Номер телефону не може бути порожнім", groups = {OnCreate.class, OnUpdate.class})
    // Виправлено регулярку: \+? означає необов'язковий плюс на початку замість знаку питання
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Невірний формат номеру телефону", groups = {OnCreate.class, OnUpdate.class})
    private String workPhoneNumber;

    private Set<Long> competentSpaUnitIds;
}