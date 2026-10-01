package com.demo_hotel_service.data.models.bookings;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.common.enums.Gender;
import com.demo_hotel_service.data.models.hotel_offerings.spa.SpaUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "booking_spa_units")
public class BookingSpaUnit extends BookingUnit {
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Gender preferedGender;

    public BookingSpaUnit(long id, GeneralBooking booking, int clientCount, LocalDateTime start, LocalDateTime end, SpaUnit spaUnit, Gender preferedGender){
        super(id, booking, spaUnit, clientCount, start, end);
        this.preferedGender = preferedGender;
    }

    @Override
    public BigDecimal calculateAmount() {
        if (this.getServiceUnit() == null || this.getServiceUnit().getPrice() == null) {
            return BigDecimal.ZERO;
        }
        
        int count = this.getClientCount();
        return this.getServiceUnit().getPrice().multiply(BigDecimal.valueOf(count));
    }
}
