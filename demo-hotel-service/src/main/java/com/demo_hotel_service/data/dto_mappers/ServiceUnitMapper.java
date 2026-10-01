package com.demo_hotel_service.data.dto_mappers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.demo_hotel_service.data.models.hotel_offerings.abstraction.ServiceUnit;
import com.demo_hotel_service.data.models.hotel_offerings.rooms.RoomUnit;
import com.demo_hotel_service.data.models.hotel_offerings.spa.SpaUnit;

import lombok.NoArgsConstructor;

import com.common.dto.demo_hotel_service_dto.RoomUnitDto;
import com.common.dto.demo_hotel_service_dto.ServiceUnitDto;
import com.common.dto.demo_hotel_service_dto.SpaUnitDto;
import com.demo_hotel_service.data.dto_mappers.polymorphic.PolymorphicUpdateServiceUnitMapper;

@NoArgsConstructor
@Mapper(componentModel = "spring", uses = { ImageMapper.class, RoomUnitMapper.class, SpaUnitMapper.class })
public abstract class ServiceUnitMapper {
    private Map<Class<?>, PolymorphicUpdateServiceUnitMapper<?, ?>> mappers = new HashMap<>();

    @Autowired
    public void setMappers(List<PolymorphicUpdateServiceUnitMapper<?, ?>> mappers) {
        if (mappers != null) {
            for (var mapper : mappers) {
                this.mappers.put(mapper.getEntityClass(), mapper);
            }
        }
    }

    @SubclassMapping(target = RoomUnitDto.class, source = RoomUnit.class)
    @SubclassMapping(target = SpaUnitDto.class, source = SpaUnit.class)
    public abstract ServiceUnitDto toPolymorphicDto(ServiceUnit serviceUnit);

    @SubclassMapping(target = RoomUnit.class, source = RoomUnitDto.class)
    @SubclassMapping(target = SpaUnit.class, source = SpaUnitDto.class)
    @Mapping(target = "version", ignore = true)
    public abstract ServiceUnit toPolymorphicEntity(ServiceUnitDto serviceUnit);

    public abstract List<ServiceUnitDto> toPolymorphicDtos(List<ServiceUnit> units);

    public abstract List<ServiceUnit> toPolymorphicEntities(List<ServiceUnitDto> dtos);


    public ServiceUnit updateEntity(ServiceUnitDto dto, ServiceUnit entity) {
        if (dto == null || entity == null) {
            throw new IllegalArgumentException("cant be null");
        }

        PolymorphicUpdateServiceUnitMapper<?, ?> mapper = mappers.get(entity.getClass());

        if (mapper != null) {
            return castMapper(mapper).updateEntity(dto, entity);
        }

        if (dto.getPrice() != null) {
            entity.setPrice(dto.getPrice());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getType() != null) {
            entity.setType(dto.getType());
        }
        if (dto.getHiddenFromClient() != null) {
            entity.setHiddenFromClient(dto.getHiddenFromClient());
        }
        if (dto.getGuestCapacity() != null) {
            entity.setGuestCapacity(dto.getGuestCapacity());
        }
        if (entity.getFacilities() != null) {
            Set<String> set = dto.getFacilities();
            if (set != null) {
                entity.getFacilities().clear();
                entity.getFacilities().addAll(set);
            }
        } else {
            Set<String> set = dto.getFacilities();
            if (set != null) {
                entity.setFacilities(new HashSet<String>(set));
            }
        }

        return entity;
    }

    @SuppressWarnings("unchecked")
    private <E extends ServiceUnit, D extends ServiceUnitDto> PolymorphicUpdateServiceUnitMapper<E, D> castMapper(
            PolymorphicUpdateServiceUnitMapper<?, ?> mapper) {
        return (PolymorphicUpdateServiceUnitMapper<E, D>) mapper;
    }

}
