package com.common.dto.demo_hotel_service_dto;

import com.common.enums.Gender;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingSpaUnitDto extends BookingUnitDto {
    private Gender preferedGender;
}
