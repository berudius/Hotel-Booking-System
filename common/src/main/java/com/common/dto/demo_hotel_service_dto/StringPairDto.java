package com.common.dto.demo_hotel_service_dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class StringPairDto {
    @NotBlank(message = "Назва обов'язкова")
    @Size(max = 100, message = "Назва не може перевищувати 100 символів")
    private String name;
    
    @NotBlank(message = "Опис обов'язковий")
    @Size(max = 1000, message = "Опис не може перевищувати 1000 символів")
    private String description;
}
