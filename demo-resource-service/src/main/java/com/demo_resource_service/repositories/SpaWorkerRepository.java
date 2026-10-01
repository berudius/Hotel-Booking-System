package com.demo_resource_service.repositories;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.common.enums.Gender;
import com.demo_resource_service.data.models.SpaWorker;

import jakarta.persistence.LockModeType;

@Repository
public interface SpaWorkerRepository extends JpaRepository<SpaWorker, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT w FROM SpaWorker w 
        JOIN w.competentSpaUnitIds su 
        JOIN w.workSchedules s 
        WHERE su = :serviceUnitId 
        AND w.status = 'ACTIVE' 
        AND (:gender IS NULL OR w.gender = :gender)
        AND w.id NOT IN :usedWorkerIds
        AND s.dayOfWeek = :dayOfWeek
        AND s.startTime <= :startLocalTime 
        AND s.endTime >= :endLocalTime 
        AND NOT EXISTS (
            SELECT a FROM AllocationResource a 
            JOIN a.assignedWorkerIds aw 
            WHERE aw = w.id 
            AND a.start < :end 
            AND a.technicalEnd > :start 
            AND a.status = 'ACTIVE'
        )
    """)
    List<SpaWorker> findAvailableWorkers(
            @Param("serviceUnitId") Long serviceUnitId,
            @Param("gender") com.common.enums.Gender gender,
            @Param("dayOfWeek") java.time.DayOfWeek dayOfWeek,
            @Param("startLocalTime") LocalTime startLocalTime,
            @Param("endLocalTime") LocalTime endLocalTime,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("usedWorkerIds") Set<Integer> usedWorkerIds,
            Pageable pageable
    );

@Query("""
            SELECT MIN(b.breakEnd) FROM SpaWorker w
            JOIN w.workSchedules ws
            JOIN ws.breaks b
            JOIN w.competentSpaUnitIds compId
            WHERE w.status = 'ACTIVE'
            AND compId = :serviceUnitId
            AND (:preferedGender IS NULL OR w.gender = :preferedGender)
            AND ws.dayOfWeek = :dayOfWeek
            AND ws.startTime <= :startLocalTime
            AND ws.endTime >= :endLocalTime
            AND b.breakStart < :endLocalTime AND b.breakEnd > :startLocalTime
            """)
    LocalTime findOverlappingBreakEndTime(
            @Param("serviceUnitId") Long serviceUnitId,
            @Param("preferedGender") Gender preferedGender,
            @Param("dayOfWeek") java.time.DayOfWeek dayOfWeek,
            @Param("startLocalTime") LocalTime startLocalTime,
            @Param("endLocalTime") LocalTime endLocalTime);

            // Перевіряє, чи працівники на зміні в цей час і чи не мають інщих бронювань
    @Query("""
            SELECT CASE WHEN COUNT(DISTINCT w.id) = :workerCount THEN true ELSE false END
            FROM SpaWorker w
            JOIN w.workSchedules s
            WHERE w.id IN :workerIds
            AND w.status = 'ACTIVE'
            AND s.dayOfWeek = :dayOfWeek
            AND s.startTime <= :startLocalTime
            AND s.endTime >= :endLocalTime
            AND NOT EXISTS (
                SELECT a FROM AllocationResource a
                JOIN a.assignedWorkerIds aw
                WHERE aw = w.id
                AND a.id != :excludeAllocationId 
                AND a.start < :end
                AND a.technicalEnd > :start
                AND a.status = 'ACTIVE'
            )
            """)
    boolean areSpecificWorkersAvailableForNewTime(
            @Param("workerIds") Set<Integer> workerIds,
            @Param("workerCount") long workerCount,
            @Param("excludeAllocationId") Long excludeAllocationId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startLocalTime") LocalTime startLocalTime,
            @Param("endLocalTime") LocalTime endLocalTime,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Перевіряє, чи не потрапляє новий час на перерву цих працівників
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM SpaWorker w
            JOIN w.workSchedules ws
            JOIN ws.breaks b
            WHERE w.id IN :workerIds
            AND ws.dayOfWeek = :dayOfWeek
            AND b.breakStart < :endLocalTime AND b.breakEnd > :startLocalTime
            """)
    boolean hasOverlappingBreaksForSpecificWorkers(
            @Param("workerIds") Set<Integer> workerIds,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startLocalTime") LocalTime startLocalTime,
            @Param("endLocalTime") LocalTime endLocalTime);
}