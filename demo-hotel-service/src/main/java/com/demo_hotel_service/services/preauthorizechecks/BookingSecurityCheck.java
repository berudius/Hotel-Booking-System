package com.demo_hotel_service.services.preauthorizechecks;

import org.springframework.stereotype.Component;
import com.common.security.AuthPrincipal;
import com.demo_hotel_service.repositories.GeneralBookingRepository;
import com.demo_hotel_service.repositories.BookingUnitRepository;
import lombok.RequiredArgsConstructor;

@Component("securityCheck")
@RequiredArgsConstructor
public class BookingSecurityCheck {
    private final GeneralBookingRepository gBookingRepository;
    private final BookingUnitRepository bookingRepository;

    public boolean isGeneralBookingOwner(Long generalBookingId, AuthPrincipal principal) {
        if (principal == null || generalBookingId == null) return false;
        if ("ADMIN".equals(principal.getRole())) return true;
        
        return gBookingRepository.findById(generalBookingId)
                .map(gb -> gb.getUserId().equals(principal.getId()))
                .orElse(false);
    }

    public boolean isBookingUnitOwner(Long bookingUnitId, AuthPrincipal principal) {
        if (principal == null || bookingUnitId == null) return false;
        if ("ADMIN".equals(principal.getRole())) return true;
        
        return bookingRepository.findById(bookingUnitId)
                .map(bu -> bu.getGeneralBooking().getUserId().equals(principal.getId()))
                .orElse(false);
    }
}
