package com.demo_hotel_service.data.dto_mappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.common.dto.demo_hotel_service_dto.BookingRoomUnitDto;
import com.common.dto.demo_hotel_service_dto.BookingSpaUnitDto;
import com.common.dto.demo_hotel_service_dto.BookingUnitDto;
import com.demo_hotel_service.data.dto_mappers.polymorphic.PolymorphicUpdateBookingMapper;
import com.demo_hotel_service.data.models.bookings.BookingRoomUnit;
import com.demo_hotel_service.data.models.bookings.BookingSpaUnit;
import com.demo_hotel_service.data.models.bookings.BookingUnit;

@Mapper(componentModel = "spring", uses = { BookingRoomUnitMapper.class, BookingSpaUnitMapper.class,
        ServiceUnitMapper.class })
public abstract class BookingUnitMapper {
    private Map<Class<?>, PolymorphicUpdateBookingMapper<?, ?>> mappers = new HashMap<>();

    @Autowired
    public void setMappers(List<PolymorphicUpdateBookingMapper<?, ?>> mappers) {
        if (mappers != null) {
            for (var mapper : mappers) {
                this.mappers.put(mapper.getEntityClass(), mapper);
            }
        }
    }

    @SubclassMapping(target = BookingRoomUnitDto.class, source = BookingRoomUnit.class)
    @SubclassMapping(target = BookingSpaUnitDto.class, source = BookingSpaUnit.class)
    @Mapping(target = "serviceUnitId", source = "serviceUnit.id")
    public abstract BookingUnitDto toPolymorphicDto(BookingUnit bookingUnit);

    @SubclassMapping(target = BookingRoomUnit.class, source = BookingRoomUnitDto.class)
    @SubclassMapping(target = BookingSpaUnit.class, source = BookingSpaUnitDto.class)
    @Mapping(target = "generalBooking", ignore = true)
    @Mapping(target = "serviceUnit", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "orderDateTime", ignore = true)
    public abstract BookingUnit toPolymorphicEntity(BookingUnitDto serviceUnit);

    public abstract List<BookingUnitDto> toPolymorphicDtos(List<BookingUnit> units);

    public abstract List<BookingUnit> toPolymorphicEntities(List<BookingUnitDto> dtos);

    public BookingUnit updateEntity(BookingUnitDto dto, BookingUnit entity) {
        if (dto == null) {
            return entity;
        }

        PolymorphicUpdateBookingMapper<?, ?> mapper = mappers.get(entity.getClass());
        if (mapper != null) {
            return castMapper(mapper).updateEntity(dto, entity);
        }

        if (dto.getClientCount() != null) {
            entity.setClientCount(dto.getClientCount());
        }
        if (dto.getStart() != null) {
            entity.setStart(dto.getStart());
        }
        if (dto.getEnd() != null) {
            entity.setEnd(dto.getEnd());
        }

        return entity;
    }

    @SuppressWarnings("unchecked")
    private <E extends BookingUnit, D extends BookingUnitDto> PolymorphicUpdateBookingMapper<E, D> castMapper(
            PolymorphicUpdateBookingMapper<?, ?> mapper) {
        return (PolymorphicUpdateBookingMapper<E, D>) mapper;
    }

}
