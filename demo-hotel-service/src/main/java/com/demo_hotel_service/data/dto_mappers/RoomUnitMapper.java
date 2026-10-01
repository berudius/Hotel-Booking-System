package com.demo_hotel_service.data.dto_mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.common.dto.demo_hotel_service_dto.RoomUnitDto;
import com.demo_hotel_service.data.dto_mappers.polymorphic.PolymorphicUpdateServiceUnitMapper;
import com.demo_hotel_service.data.models.hotel_offerings.rooms.RoomUnit;

@Mapper(componentModel = "spring", uses = { ImageMapper.class })
public interface RoomUnitMapper extends PolymorphicUpdateServiceUnitMapper<RoomUnit, RoomUnitDto> {
    @Mapping(target = "version", ignore = true)
    public RoomUnit toEntity(RoomUnitDto dto);

    public RoomUnitDto toDto(RoomUnit entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "imageRecords", ignore = true)
    @Mapping(target = "version", ignore = true)
    public RoomUnit updateEntity(RoomUnitDto dto, @MappingTarget RoomUnit entity);

    @Override
    public default Class<?> getEntityClass() {
        return RoomUnit.class;
    }
}
