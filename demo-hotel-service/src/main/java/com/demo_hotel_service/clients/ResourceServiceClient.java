package com.demo_hotel_service.clients;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.common.dto.demo_resource_service_dto.AllocationResourceDto;
import com.demo_hotel_service.config.feignconfig.ResourceClientFeignConfig;

@FeignClient(name = "resource-service", path = "/api/allocations", configuration = ResourceClientFeignConfig.class)
public interface ResourceServiceClient {
    
    @PostMapping("/allocate")
    List<AllocationResourceDto> allocateResources(@RequestBody List<AllocationResourceDto> allocationRequests);

    @PutMapping("/general-booking/{generalBookingId}/status")
    void updateStatusByGeneralBooking(@PathVariable("generalBookingId") Long generalBookingId, @RequestParam("status") String status); 

    @PutMapping("/booking-unit/{bookingUnitId}/status")
    void updateStatusByBookingUnit(@PathVariable("bookingUnitId") Long bookingUnitId, @RequestParam("status") String status);

    @PutMapping("/booking-unit/{bookingUnitId}/dates")
    void updateAllocationDates(
        @PathVariable("bookingUnitId") Long bookingUnitId,
        @RequestParam("start") LocalDateTime start,
        @RequestParam("end") LocalDateTime end);

        @PutMapping("/booking-unit/{bookingUnitId}/end-time")
        void updateAllocationEndTime(
            @PathVariable("bookingUnitId") Long bookingUnitId,
            @RequestParam("newEndTime") LocalDateTime newEndTime);
}
