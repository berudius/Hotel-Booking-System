package com.demo_hotel_service.repositories;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.common.dto.demo_hotel_service_dto.ServiceUnitShortDto;
import com.demo_hotel_service.data.models.hotel_offerings.abstraction.ServiceUnit;

public interface ServiceUnitRepository extends JpaRepository<ServiceUnit, Long> {
    
    @Query("SELECT s_u FROM ServiceUnit s_u WHERE TYPE(s_u) = :clazz AND (:isAdmin = true OR s_u.hiddenFromClient = false)")
List<ServiceUnit> findByTypeFiltered(@Param("clazz") Class<? extends ServiceUnit> clazz, @Param("isAdmin") boolean isAdmin, Pageable pageable);

    @Query("""
        SELECT new com.common.dto.demo_hotel_service_dto.ServiceUnitShortDto(
            spa.id, 
            spa.name, 
            spa.type, 
            (SELECT i.url FROM ImageRecord i WHERE i.serviceUnit = spa ORDER BY i.position ASC LIMIT 1),
            'SPA'
        )
        FROM SpaUnit spa
        WHERE CAST(spa.id AS string) LIKE CONCAT('%', :term, '%')
           OR LOWER(spa.name) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(spa.type) LIKE LOWER(CONCAT('%', :term, '%'))
    """)
    List<ServiceUnitShortDto> searchSpaUnitsShort(@Param("term") String term, Pageable pageable);


    @Query("""
        SELECT new com.common.dto.demo_hotel_service_dto.ServiceUnitShortDto(
            room.id, 
            room.type, 
            room.type, 
            (SELECT i.url FROM ImageRecord i WHERE i.serviceUnit = room ORDER BY i.position ASC LIMIT 1),
            'ROOM'
        )
        FROM RoomUnit room
        WHERE CAST(room.id AS string) LIKE CONCAT('%', :term, '%')
           OR LOWER(room.type) LIKE LOWER(CONCAT('%', :term, '%'))
    """)
    List<ServiceUnitShortDto> searchRoomUnitsShort(@Param("term") String term, Pageable pageable);


    @Query("""
        SELECT new com.common.dto.demo_hotel_service_dto.ServiceUnitShortDto(
            s.id, 
            (CASE WHEN TYPE(s) = SpaUnit THEN TREAT(s AS SpaUnit).name ELSE s.type END), 
            s.type, 
            (SELECT i.url FROM ImageRecord i WHERE i.serviceUnit = s ORDER BY i.position ASC LIMIT 1),
            (CASE WHEN TYPE(s) = SpaUnit THEN 'SPA' ELSE 'ROOM' END)
        )
        FROM ServiceUnit s WHERE s.id IN :ids
    """)
    List<ServiceUnitShortDto> findServiceUnitsShortByIds(@Param("ids") Set<Long> ids);
}
