package com.demo_hotel_service.data.dto_mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.demo_hotel_service.data.models.bookings.BookingSpaUnit;
import com.common.dto.demo_hotel_service_dto.BookingSpaUnitDto;
import com.common.dto.demo_hotel_service_dto.BookingUnitDto;
import com.demo_hotel_service.data.dto_mappers.polymorphic.PolymorphicUpdateBookingMapper;

@Mapper(componentModel = "spring", uses = { ServiceUnitMapper.class })
public interface BookingSpaUnitMapper extends PolymorphicUpdateBookingMapper<BookingSpaUnit, BookingUnitDto> {

    @Mapping(target = "generalBooking", ignore = true)
    @Mapping(target = "serviceUnit", ignore = true)
    @Mapping(target = "version", ignore = true)
    public BookingSpaUnit toEntity(BookingSpaUnitDto dto);

    @Mapping(target = "serviceUnitId", source = "serviceUnit.id")
    public BookingSpaUnitDto toDto(BookingSpaUnit entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "generalBooking", ignore = true)
    @Mapping(target = "serviceUnit", ignore = true)
    @Mapping(target = "version", ignore = true)
    public BookingSpaUnit updateEntity(BookingSpaUnitDto dto, @MappingTarget BookingSpaUnit entity);

    @Override
    public default Class<?> getEntityClass() {
        return BookingSpaUnit.class;
    }
}
