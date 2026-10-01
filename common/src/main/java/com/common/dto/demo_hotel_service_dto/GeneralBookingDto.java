package com.common.dto.demo_hotel_service_dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.common.enums.BookingStatus;
import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class GeneralBookingDto {
    @Null(message = "ID має бути порожнім при створенні", groups = OnCreate.class)
    @NotNull(message = "ID є обов'язковим при оновленні", groups = OnUpdate.class)
    private Long id;
    
    private LocalDateTime orderDateTime;
    private Long userId;
    
    @NotBlank(message = "Номер телефону обов'язковий", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^380\\d{9}$", message = "Номер телефону має бути у форматі 380XXXXXXXXX", groups = {OnCreate.class, OnUpdate.class})
    private String phoneNumber;
    
    private BookingStatus status;

    @NotEmpty(message = "Бронювання повинно містити хоча б одну послугу", groups = OnCreate.class)
    @Valid
    private List<BookingUnitDto> bookingUnits;
    
    @Size(max = 2000, message = "Коментар не може перевищувати 2000 символів")
    private String clientComment;
}


