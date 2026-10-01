package com.demo_hotel_service.data.models.hotel_offerings.spa;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class StringPair {
    private String name;
    private String description;
}
