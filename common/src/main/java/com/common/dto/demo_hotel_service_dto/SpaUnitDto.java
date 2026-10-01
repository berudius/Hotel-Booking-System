package com.common.dto.demo_hotel_service_dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.common.validation.OnCreate;
import com.common.validation.OnUpdate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpaUnitDto extends ServiceUnitDto {
    
    @NotBlank(message = "Назва SPA-послуги обов'язкова", groups = {OnCreate.class, OnUpdate.class})
    @Size(max = 100, message = "Назва не може перевищувати 100 символів")
    private String name;
    
    @Valid
    private List<StringPairDto> careProductsDescriptions; 
    
    @Valid
    private List<StringPairDto> spaStagesDescriptions;
    
    @NotBlank(message = "Інформація про підготовку обов'язкова", groups = {OnCreate.class, OnUpdate.class})
    @Size(max = 2000, message = "Інформація про підготовку не може перевищувати 2000 символів")
    private String preparingInfoForClient;

    private List<
     @Size(max = 255, message = "Протипоказання не може перевищувати 255 символів")
     String> contraindications;
    private List<
     @Size(max = 255, message = "Застереження не може перевищувати 255 символів")
     String> cautionNotes;

    @NotNull(message = "Тривалість послуги обов'язкова", groups = {OnCreate.class, OnUpdate.class})
    @Min(value = 1, message = "Тривалість має бути більше 0", groups = {OnCreate.class, OnUpdate.class})
    private Integer durationInMinutes;

    public String getH5BlockValue(){
        return this.name;
    }
}
