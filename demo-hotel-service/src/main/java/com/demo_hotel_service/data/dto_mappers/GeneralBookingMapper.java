package com.demo_hotel_service.data.dto_mappers;

import java.util.List;

import org.mapstruct.BeanMapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.domain.Page;

import com.common.dto.demo_hotel_service_dto.GeneralBookingDto;
import com.demo_hotel_service.data.models.bookings.GeneralBooking;

@Mapper(componentModel = "spring", uses = { BookingUnitMapper.class })
public interface GeneralBookingMapper {

    @Mapping(target = "orderDateTime", ignore = true)
    public GeneralBooking toEntity(GeneralBookingDto dto);

    public GeneralBookingDto toDto(GeneralBooking entity);

    public List<GeneralBookingDto> toDtos(Page<GeneralBooking> entities);
    public List<GeneralBookingDto> toDtos(List<GeneralBooking> entities);
    

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "bookingUnits", ignore = true)
    @Mapping(target = "orderDateTime", ignore = true)
    public GeneralBooking updateEntity(GeneralBookingDto dto, @MappingTarget GeneralBooking entity);
}
