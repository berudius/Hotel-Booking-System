package com.common.dto.demo_hotel_service_dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomUnitDto extends ServiceUnitDto {
    public String getH5BlockValue(){
        return this.type;
    }
}
