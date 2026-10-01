package com.demo_hotel_service.data.dto_mappers;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.common.dto.demo_hotel_service_dto.SpaUnitDto;
import com.demo_hotel_service.data.dto_mappers.polymorphic.PolymorphicUpdateServiceUnitMapper;
import com.demo_hotel_service.data.models.hotel_offerings.spa.SpaUnit;

@Mapper(componentModel = "spring", uses = { ImageMapper.class })
public interface SpaUnitMapper extends PolymorphicUpdateServiceUnitMapper<SpaUnit, SpaUnitDto> {
    @Mapping(target = "version", ignore = true)
    public SpaUnit toEntity(SpaUnitDto dto);

    public SpaUnitDto toDto(SpaUnit entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "imageRecords", ignore = true)
    @Mapping(target = "version", ignore = true)
    public SpaUnit updateEntity(SpaUnitDto dto, @MappingTarget SpaUnit entity);

    @Override
    public default Class<?> getEntityClass() {
        return SpaUnit.class;
    }
}
