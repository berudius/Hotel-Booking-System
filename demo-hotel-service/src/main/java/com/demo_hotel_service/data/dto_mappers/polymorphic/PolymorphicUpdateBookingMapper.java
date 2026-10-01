package com.demo_hotel_service.data.dto_mappers.polymorphic;

import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.common.dto.demo_hotel_service_dto.BookingUnitDto;
import com.demo_hotel_service.data.models.bookings.BookingUnit;

public interface PolymorphicUpdateBookingMapper<E extends BookingUnit, D extends BookingUnitDto> {
    @Mapping(target = "generalBooking", ignore = true)
    @Mapping(target = "serviceUnit", ignore = true)
    @Mapping(target = "version", ignore = true)
    public E updateEntity(D dto, @MappingTarget E entity);
    public Class<?> getEntityClass();
}
