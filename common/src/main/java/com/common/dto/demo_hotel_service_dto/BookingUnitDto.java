package com.common.dto.demo_hotel_service_dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import com.common.enums.BookingStatus;
import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "classType")
@JsonSubTypes({
                @JsonSubTypes.Type(value = BookingSpaUnitDto.class, name = "SPA_BOOKING"),
                @JsonSubTypes.Type(value = BookingRoomUnitDto.class, name = "ROOM_BOOKING")
})
public class BookingUnitDto {
        @Null(message = "ID послуги має бути порожнім при створенні", groups = OnCreate.class)
        @NotNull(message = "ID послуги обов'язковий при оновленні", groups = OnUpdate.class)
        private Long id;
        
        private ServiceUnitDto serviceUnit;
        
        @NotNull(message = "ID базової послуги (serviceUnitId) обов'язковий", groups = {OnCreate.class, OnUpdate.class})
        private Long serviceUnitId;

        @Null(message = "Amount вираховується автоматично!", groups = {OnCreate.class, OnUpdate.class})
        private BigDecimal amount;
        
        private BookingStatus status;
        
        @NotNull(message = "Кількість клієнтів обов'язкова", groups = {OnCreate.class, OnUpdate.class})
        @Min(value = 1, message = "Мінімум 1 клієнт", groups = {OnCreate.class, OnUpdate.class})
        @Max(value = 5, message = "Максимум 5 клієнтів", groups = {OnCreate.class, OnUpdate.class})
        private Integer clientCount;
        
        @NotNull(message = "Час початку обов'язковий", groups = {OnCreate.class, OnUpdate.class})
        private LocalDateTime start;
        
        @NotNull(message = "Час завершення обов'язковий", groups = {OnCreate.class, OnUpdate.class})
        private LocalDateTime end;
        
        private LocalDateTime orderDateTime;
        // private List<WorkerDataDto> assignedWorkers;

        @AssertTrue(message = "Час початку бронювання не може бути в минулому", groups = {OnCreate.class, OnUpdate.class})
        @JsonIgnore
        public boolean isStartTimeValid() {
            if (start == null) {
                return true;
            }
            // Перевіряємо, чи час початку не раніше ніж поточний час мінус 2 хвилини
            return !start.isBefore(LocalDateTime.now().minusMinutes(2));
        }

        @AssertTrue(message = "Час завершення повинен бути пізніше за час початку", groups = {OnCreate.class, OnUpdate.class})
        @JsonIgnore
        public boolean isEndTimeValid() {
            if (start == null || end == null) {
                return true; 
            }
            return end.isAfter(start);
        }
}
