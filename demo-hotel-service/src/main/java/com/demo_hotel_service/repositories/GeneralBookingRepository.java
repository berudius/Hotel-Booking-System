package com.demo_hotel_service.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.common.enums.BookingStatus;
import com.demo_hotel_service.data.models.bookings.GeneralBooking;

public interface GeneralBookingRepository extends JpaRepository<GeneralBooking, Long> {

@Query("""
      SELECT g_b FROM GeneralBooking g_b
      LEFT JOIN FETCH g_b.bookingUnits bu
      LEFT JOIN FETCH bu.serviceUnit
      WHERE (:showAll = true OR g_b.status IN ('PENDING', 'CONFIRMED'))
      AND (g_b.userId = :userId OR g_b.phoneNumber = :phoneNumber)
    """)
    List<GeneralBooking> findAllFiltered(@Param("userId") Long userId, @Param("phoneNumber") String phoneNumber, @Param("showAll") boolean showAll, Pageable pageable);

    @Query("""
      SELECT g_b FROM GeneralBooking g_b
      LEFT JOIN FETCH g_b.bookingUnits bu
      LEFT JOIN FETCH bu.serviceUnit
      WHERE (:showAll = true OR g_b.status IN ('PENDING', 'CONFIRMED'))
    """)
    List<GeneralBooking> findAllFiltered(@Param("showAll") boolean showAll, Pageable pageable);

  List<GeneralBooking> findAllByStatusAndOrderDateTimeBefore(BookingStatus status, LocalDateTime timeLimit);

    @Modifying
    @Query("UPDATE GeneralBooking gb SET gb.userId = :userId WHERE gb.phoneNumber = :phoneNumber AND gb.userId IS NULL")
    void linkOrphanBookings(@Param("phoneNumber") String phoneNumber, @Param("userId") Long userId);


} 
