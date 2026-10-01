package com.common.dto.demo_hotel_service_dto;


import java.time.Duration;

import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingRoomUnitDto extends BookingUnitDto {
    @AssertTrue(
        message = "Мінімальний час бронювання кімнати — 24 години", 
        groups = {OnCreate.class, OnUpdate.class}
    )
    @JsonIgnore
    public boolean isValidDuration() {
        // Якщо дати ще не задані, пропускаємо цю перевірку 
        // (відпрацюють стандартні @NotNull з базового класу)
        if (getStart() == null || getEnd() == null) {
            return true; 
        }
        long hoursBetween = Duration.between(getStart(), getEnd()).toHours();
        
        return hoursBetween >= 24;
    }
}
