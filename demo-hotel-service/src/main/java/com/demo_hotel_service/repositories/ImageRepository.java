package com.demo_hotel_service.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.demo_hotel_service.data.models.images.ImageRecord;

import jakarta.persistence.LockModeType;

public interface ImageRepository extends JpaRepository<ImageRecord, Long> {

    @Query("SELECT i.url FROM ImageRecord i WHERE i.serviceUnit.id = :serviceUnitId")
    public List<String> findImageUrls(long serviceUnitId);

    @Modifying
    @Query("DELETE FROM ImageRecord i WHERE i.serviceUnit.id = :serviceUnitId")
    public void deleteByServiceUnitId(long serviceUnitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COALESCE(MAX(ir.position), 0) FROM ImageRecord ir WHERE ir.serviceUnit.id = :serviceUnitId")
    public int findMaxPosition(long serviceUnitId);

    @Query("SELECT i.position FROM ImageRecord i WHERE i.id = :id")
    Integer findPositionById(@Param("id") Long id);


    @Modifying
    @Query("UPDATE ImageRecord i SET i.position = :position WHERE i.id = :id")
    void updatePosition(@Param("id") long id, @Param("position") int position);


} 
