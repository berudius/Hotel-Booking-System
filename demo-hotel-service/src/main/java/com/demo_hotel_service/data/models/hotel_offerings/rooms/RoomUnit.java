package com.demo_hotel_service.data.models.hotel_offerings.rooms;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.demo_hotel_service.data.models.hotel_offerings.abstraction.ServiceUnit;
import com.demo_hotel_service.data.models.images.ImageRecord;

import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@NoArgsConstructor
@ToString(callSuper = true)

@Entity
@Table(name = "room_units")
public class RoomUnit extends ServiceUnit {

    public RoomUnit(long id, BigDecimal price, String description, String type, boolean isHiddenFromClient,
            Set<String> facilities, int guestCapacity,
            List<ImageRecord> images) {
        super(id, price, description, type, isHiddenFromClient, guestCapacity, facilities, images);
    }

}
