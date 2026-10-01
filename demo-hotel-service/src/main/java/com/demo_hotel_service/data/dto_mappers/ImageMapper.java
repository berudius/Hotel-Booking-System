package com.demo_hotel_service.data.dto_mappers;

import java.util.List;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.common.dto.demo_hotel_service_dto.ImageRecordDto;
import com.demo_hotel_service.data.models.images.ImageRecord;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    @Mapping(target = "serviceUnit", ignore = true)
    public ImageRecord toEntity(ImageRecordDto dto);

    public ImageRecordDto toDto(ImageRecord entity);

    public List<ImageRecordDto> toListDtos(List<ImageRecord> dtos);

    public List<ImageRecord> toListEntities(List<ImageRecordDto> entities);

    @Mapping(target = "serviceUnit", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public ImageRecord updateEntity(ImageRecordDto dto, @MappingTarget ImageRecord entity);
}
