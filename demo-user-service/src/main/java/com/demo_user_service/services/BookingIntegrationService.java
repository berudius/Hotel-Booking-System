package com.demo_user_service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.common.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(BookingIntegrationService.class);
    private final RestClient restClient = RestClient.create();
    private final JwtUtil jwtUtil;

    @Value("${booking_service_url}")
    private String bookingServiceUrl;

    @Async
    public void claimBookingsForUser(Long userId, String phoneNumber) {
        try {
            log.info("Асинхронний запуск підтягування бронювань для userId: {}", userId);

            String systemToken = jwtUtil.generateServiceToken("user-service");

            restClient.put()
                    .uri(bookingServiceUrl + "/api/internal/bookings/claim?phoneNumber={phone}&userId={id}",
                            phoneNumber, userId)
                    .header("Authorization", "Bearer " + systemToken)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Бронювання успішно підтягнуті для користувача {}", userId);

        } catch (Exception e) {
            log.error("Помилка підтягування бронювань. Booking Service недоступний: {}", e.getMessage());
        }
    }
}