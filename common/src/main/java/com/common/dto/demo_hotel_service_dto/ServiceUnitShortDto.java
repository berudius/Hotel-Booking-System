package com.common.dto.demo_hotel_service_dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServiceUnitShortDto {
    private Long id;
    private String name; // name для Spa, 
    private String type; //type для Room
    private String imageUrl;
    private String category;

    public ServiceUnitShortDto(long id, String title, String type, String imageUrl, String category) {
        this.id = id;
        this.name = title;
        this.type = type;
        this.imageUrl = imageUrl;
        this.category = category;
    }
}
