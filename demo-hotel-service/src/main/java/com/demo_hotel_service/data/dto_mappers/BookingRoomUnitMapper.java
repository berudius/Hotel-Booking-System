package com.demo_hotel_service.data.dto_mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.common.dto.demo_hotel_service_dto.BookingRoomUnitDto;
import com.demo_hotel_service.data.dto_mappers.polymorphic.PolymorphicUpdateBookingMapper;
import com.demo_hotel_service.data.models.bookings.BookingRoomUnit;

@Mapper(componentModel = "spring", uses = { ServiceUnitMapper.class })
public interface BookingRoomUnitMapper extends PolymorphicUpdateBookingMapper<BookingRoomUnit, BookingRoomUnitDto> {

    @Mapping(target = "generalBooking", ignore = true)
    @Mapping(target = "version", ignore = true)
    public BookingRoomUnit toEntity(BookingRoomUnitDto dto);

    @Mapping(target = "serviceUnitId", source = "serviceUnit.id")
    public BookingRoomUnitDto toDto(BookingRoomUnit entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "generalBooking", ignore = true)
    @Mapping(target = "version", ignore = true)
    public BookingRoomUnit updateEntity(BookingRoomUnitDto dto, @MappingTarget BookingRoomUnit entity);

    @Override
    public default Class<?> getEntityClass() {
        return BookingRoomUnit.class;
    }

}
