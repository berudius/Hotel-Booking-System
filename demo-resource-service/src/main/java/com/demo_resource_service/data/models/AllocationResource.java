package com.demo_resource_service.data.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.common.enums.AllocationStatus;
import com.common.enums.Gender;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
public class AllocationResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private PhysicalServiceUnit physicalServiceUnit;

    private long generalBookingId;
    private long bookingUnitId;
    private long serviceUnitId;

    @Column(nullable = false)
    private LocalDateTime start;
    @Column(nullable = false)
    private LocalDateTime end;
    @Column(name = "technical_end_time")
    private LocalDateTime technicalEnd; // Час клієнта(end) + час на прибирання

    private int clientCount;

    @Column(name = "preferedGender")
    private Gender preferedGender;

    @ElementCollection
    @CollectionTable(name = "allocation_workers", joinColumns = @JoinColumn(name = "allocation_id"))
    @Column(name = "worker_id", nullable = false)
    private Set<Integer> assignedWorkerIds = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status = AllocationStatus.ACTIVE;

}
