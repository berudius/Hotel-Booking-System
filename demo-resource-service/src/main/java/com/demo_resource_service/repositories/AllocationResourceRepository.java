package com.demo_resource_service.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.demo_resource_service.data.models.AllocationResource;
import com.common.enums.AllocationStatus;

public interface AllocationResourceRepository extends JpaRepository<AllocationResource, Long> {

    Optional<AllocationResource> findByBookingUnitId(Long bookingUnitId);

    List<AllocationResource> findAllByPhysicalServiceUnitId(Long physicalServiceUnitId);

        @Query("""
                SELECT CASE WHEN COUNT(ar) > 0 THEN true ELSE false END 
                FROM AllocationResource ar 
                WHERE ar.physicalServiceUnit.id = :physicalUnitId 
                AND ar.id != :excludeAllocationId 
                AND ar.start < :newTechnicalEnd 
                AND ar.technicalEnd > :newStart
                """)
        boolean existsOverlappingAllocation(
            @Param("physicalUnitId") Long physicalUnitId,
            @Param("excludeAllocationId") Long excludeAllocationId,
            @Param("newStart") LocalDateTime newStart,
            @Param("newTechnicalEnd") LocalDateTime newTechnicalEnd);

    List<AllocationResource> findByGeneralBookingIdAndStatus(Long generalBookingId, AllocationStatus status);

    Optional<AllocationResource> findByBookingUnitIdAndStatus(Long bookingUnitId, AllocationStatus status);

        boolean existsByPhysicalServiceUnitIdAndTechnicalEndAfterAndStatus(
            Long physicalServiceUnitId, 
            LocalDateTime now, 
            AllocationStatus status);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AllocationResource a JOIN a.assignedWorkerIds w WHERE w = :workerId AND a.technicalEnd > :now AND a.status = :status")
    boolean existsFutureActiveAllocationForWorker(
            @Param("workerId") Integer workerId,
            @Param("now") LocalDateTime now,
            @Param("status") AllocationStatus status);

    @Query("""
            SELECT a FROM AllocationResource a 
            WHERE a.start < :end AND a.technicalEnd > :start
            AND a.status IN ('ACTIVE', 'COMPLETED')
            """)
    List<AllocationResource> findAllocationsInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);


        @Query("""
                SELECT a FROM AllocationResource a 
                JOIN a.assignedWorkerIds aw 
                WHERE aw = :workerId 
                AND a.start >= :now 
                AND a.status = 'ACTIVE' 
                AND FUNCTION('DAYOFWEEK', a.start) = :dayOfWeekIndex
        """)
        List<AllocationResource> findFutureActiveAllocationsForWorkerAndDay(
                @Param("workerId") Integer workerId,
                @Param("now") LocalDateTime now,
                @Param("dayOfWeekIndex") Integer dayOfWeekIndex);
}
