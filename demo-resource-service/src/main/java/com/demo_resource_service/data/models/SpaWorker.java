package com.demo_resource_service.data.models;

import java.util.List;
import java.util.Set;

import com.common.enums.Gender;
import com.common.enums.WorkerStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "spa_workers")
@Getter
@Setter
public class SpaWorker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerStatus status;

    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL)
    private List<WorkerSchedule> workSchedules;

    @Column(nullable = false)
    private String workPhoneNumber;

   @ElementCollection(fetch = FetchType.LAZY)
   @CollectionTable(
        name = "spa_worker_competencies",
        joinColumns = @JoinColumn(name = "worker_id" )
    )
    @Column(name = "spa_unit_id", nullable = false)
    private Set<Long> competentSpaUnitIds;

}




