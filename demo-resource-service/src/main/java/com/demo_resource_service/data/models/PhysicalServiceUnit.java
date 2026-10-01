package com.demo_resource_service.data.models;

import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
public class PhysicalServiceUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ElementCollection
    @CollectionTable(name = "service_unit_ids", joinColumns = @JoinColumn(name = "physical_service_unit_id"))
    @Column(name = "service_unit_id", nullable = false)
    private Set<Long> serviceUnitIds;

    @Column(unique = true, nullable = false, length = 50)
    private String premisesNumber;

    @Column(nullable = false)
    private int clientCapacity;
    @Column(nullable = false)
    private int cleaningTimeInMinutes;
    @Column(nullable = false)
    private boolean outOfService;
}
