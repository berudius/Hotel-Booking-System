package com.demo_hotel_service.data.models.bookings;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import com.demo_hotel_service.data.models.hotel_offerings.rooms.RoomUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)

@Entity
@Table(name = "booking_room_units")
public class BookingRoomUnit extends BookingUnit {
    public BookingRoomUnit(long id, GeneralBooking booking, int clientCount, LocalDateTime start, LocalDateTime end, RoomUnit roomUnit){
        super(id, booking, roomUnit, clientCount, start, end);
    }

    @Override
    public BigDecimal calculateAmount() {
        if (this.getServiceUnit() == null || this.getServiceUnit().getPrice() == null) {
            return BigDecimal.ZERO;
        }
        if (this.getStart() == null || this.getEnd() == null) {
            return this.getServiceUnit().getPrice();
        }

        long totalHours = Duration.between(this.getStart(), this.getEnd()).toHours();
        long fullDays = totalHours / 24;
        long extraHours = totalHours % 24;

        BigDecimal multiplier = BigDecimal.valueOf(fullDays);

        if (fullDays == 0) {
            // Мінімальний час проживання - 1 доба
            multiplier = BigDecimal.ONE;
        } else {
            // Правило: до 12 год = 50%, більше 12 год = 100%
            if (extraHours > 0 && extraHours <= 12) {
                multiplier = multiplier.add(new BigDecimal("0.5"));
            } else if (extraHours > 12) {
                multiplier = multiplier.add(BigDecimal.ONE);
            }
        }
        return this.getServiceUnit().getPrice().multiply(multiplier);
    }
}
