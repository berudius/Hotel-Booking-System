package com.common.dto.demo_hotel_service_dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "classType")
@JsonSubTypes({
                @JsonSubTypes.Type(value = SpaUnitDto.class, name = "SPA"),
                @JsonSubTypes.Type(value = RoomUnitDto.class, name = "ROOM")
})
public class ServiceUnitDto {
        @Null(message = "ID має бути порожнім при створенні", groups = OnCreate.class)
        @NotNull(message = "ID є обов'язковим при оновленні", groups = OnUpdate.class)
        private Long id;

        @NotNull(message = "Ціна є обов'язковою", groups = {OnCreate.class, OnUpdate.class})
        @Min(value = 0, message = "Ціна не може бути від'ємною", groups = {OnCreate.class, OnUpdate.class})
        private BigDecimal price;

        @NotBlank(message = "Опис не може бути порожнім", groups = {OnCreate.class, OnUpdate.class})
        @Size(max = 4000, message = "Опис не може перевищувати 4000 символів", groups = {OnCreate.class, OnUpdate.class})
        private String description;

        @NotBlank(message = "Тип послуги обов'язковий", groups = {OnCreate.class, OnUpdate.class})
        @Size(max = 30, message = "Тип не може перевищувати 30 символів", groups = {OnCreate.class, OnUpdate.class})
        protected String type;

        @NotNull(message = "Місткість клієнтів є обов'язковою", groups = {OnCreate.class, OnUpdate.class})
        @Min(value = 1, message = "Місткість має бути мінімум 1", groups = {OnCreate.class, OnUpdate.class})
        private Integer guestCapacity;

        @NotNull(message = "Статус hiddenFromClient обов'язковий", groups = {OnCreate.class, OnUpdate.class})
        private Boolean hiddenFromClient;

        private Set<
         @NotBlank(message = "Зручність не може бути порожньою", groups = {OnCreate.class, OnUpdate.class}) 
         @Size(max = 20, message = "Назва зручності не може перевищувати 20 символів", groups = {OnCreate.class, OnUpdate.class})
        String> facilities;

        private List<ImageRecordDto> imageRecords;
}
