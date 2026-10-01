package com.demo_resource_service.repositories;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.demo_resource_service.data.models.PhysicalServiceUnit;

import jakarta.persistence.LockModeType;

public interface PhysicalServiceUnitRepository extends JpaRepository<PhysicalServiceUnit, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
        SELECT p FROM PhysicalServiceUnit p 
        JOIN p.serviceUnitIds su 
        WHERE su = :serviceUnitId 
        AND p.clientCapacity >= :capacity 
        AND p.outOfService = false 
        AND p.id NOT IN :usedIds 
        AND NOT EXISTS (
            SELECT a FROM AllocationResource a 
            WHERE a.physicalServiceUnit = p 
            AND a.start < :end 
            AND a.end > :start 
            AND a.status = com.common.enums.AllocationStatus.ACTIVE
        )
    """)
    List<PhysicalServiceUnit> findAvailableUnits(
        @Param("serviceUnitId") Long serviceUnitId,
        @Param("capacity") int capacity,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("usedIds") Collection<Long> usedIds,
        Pageable pageable
);

}
