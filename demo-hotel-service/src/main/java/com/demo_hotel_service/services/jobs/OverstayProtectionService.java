package com.demo_hotel_service.services.jobs;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.common.enums.BookingStatus;
import com.demo_hotel_service.clients.ResourceServiceClient;
import com.demo_hotel_service.data.models.bookings.BookingUnit;
import com.demo_hotel_service.repositories.BookingUnitRepository;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverstayProtectionService {

    private final BookingUnitRepository bookingUnitRepository;
    private final ResourceServiceClient resourceServiceClient;

    // Запускаємо кожні 15 хвилин
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void handleLateCheckouts() { 
        LocalDateTime now = LocalDateTime.now();
        List<BookingUnit> overstayingUnits = bookingUnitRepository
                .findAllByStatusAndEndBefore(BookingStatus.CONFIRMED, now.minusMinutes(5));

        for (BookingUnit unit : overstayingUnits) {
            try {
                LocalDateTime protectionTime = now.plusHours(1);
                
                // Блокуємо ресурс у мікросервісі алокацій
                resourceServiceClient.updateAllocationEndTime(unit.getId(), protectionTime);

                unit.setEnd(protectionTime);

                // перерераховуємо вартість
                unit.setAmount(unit.calculateAmount());

                bookingUnitRepository.save(unit);

                log.warn("Технічне подовження для BookingUnit {}: ресурс заблоковано до {}, нова сума: {}", 
                         unit.getId(), protectionTime, unit.getAmount());

            } catch (FeignException e) {
                log.error("КРИТИЧНО! Гість у BookingUnit {} не виїхав, але кімната вже заброньована наступним клієнтом! Потрібне втручання.", unit.getId());
            } catch (Exception e) {
                log.error("Невідома помилка при обробці Overstay для Unit {}: {}", unit.getId(), e.getMessage());
            }
        }
    }
}