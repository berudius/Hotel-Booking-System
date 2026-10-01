package com.demo_hotel_service.services.jobs;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.common.enums.BookingStatus;
import com.demo_hotel_service.data.models.bookings.BookingUnit;
import com.demo_hotel_service.data.models.bookings.GeneralBooking;
import com.demo_hotel_service.repositories.BookingUnitRepository;
import com.demo_hotel_service.repositories.GeneralBookingRepository;
import com.demo_hotel_service.services.BookingUnitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingExpirationService {

    private final GeneralBookingRepository generalBookingRepository;
    private final BookingUnitRepository bookingUnitRepository; 
    private final BookingUnitService bookingUnitService; 

    private static final int EXPIRATION_TIME_MINUTES = 120;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireUnconfirmedGeneralBookings() {
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(EXPIRATION_TIME_MINUTES);
        List<GeneralBooking> expiredBookings = generalBookingRepository
                .findAllByStatusAndOrderDateTimeBefore(BookingStatus.PENDING, timeLimit);

        if (expiredBookings.isEmpty()) return;

        log.info("Скасування {} застарілих GeneralBookings.", expiredBookings.size());

        for (GeneralBooking booking : expiredBookings) {
            try {
                booking.setStatus(BookingStatus.EXPIRED);
                booking.getBookingUnits().forEach(bu -> bu.setStatus(BookingStatus.EXPIRED));
            } catch (Exception e) {
                log.error("Помилка при скасуванні замовлення {}: {}", booking.getId(), e.getMessage());
            }
        }
        generalBookingRepository.saveAll(expiredBookings);
    }

    @Scheduled(fixedRate = 60500)
    @Transactional
    public void expireUnconfirmedBookingUnits() {
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(EXPIRATION_TIME_MINUTES);
        
        List<BookingUnit> expiredUnits = bookingUnitRepository
                .findAllByStatusAndOrderDateTimeBefore(BookingStatus.PENDING, timeLimit);

        if (expiredUnits.isEmpty()) return;

        log.info("Скасування {} застарілих окремих послуг (BookingUnits).", expiredUnits.size());

        for (BookingUnit unit : expiredUnits) {
            try {
                unit.setStatus(BookingStatus.EXPIRED);
                bookingUnitService.syncParentGeneralBookingStatus(unit.getGeneralBooking());
                
            } catch (Exception e) {
                log.error("Помилка при скасуванні послуги {}: {}", unit.getId(), e.getMessage());
            }
        }
        bookingUnitRepository.saveAll(expiredUnits);
    }
}