package com.demo_hotel_service.data.models.hotel_offerings.spa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.demo_hotel_service.data.models.hotel_offerings.abstraction.ServiceUnit;
import com.demo_hotel_service.data.models.images.ImageRecord;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)

@Entity
@Table(name = "spa_units")
public class SpaUnit extends ServiceUnit {
    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 2000, nullable = false)
    private String preparingInfoForClient;

    private int durationInMinutes;

    @ElementCollection
    @Column(name = "contraindication", length = 255, nullable = true)
    private List<String> contraindications;
    @ElementCollection
    @Column(name = "caution_note", length = 255, nullable = true)
    private List<String> cautionNotes;

    @ElementCollection
    @CollectionTable(name = "spa_unit_stages", joinColumns = @JoinColumn(name = "spa_unit_id"))
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "stage_name", length = 100, nullable = false)),
            @AttributeOverride(name = "description", column = @Column(name = "stage_description", length = 1000, nullable = false))
    })
    private List<StringPair> spaStagesDescriptions;

    @ElementCollection
    @CollectionTable(name = "spa_unit_care_products", joinColumns = @JoinColumn(name = "spa_unit_id"))
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "product_name", length = 100, nullable = false)),
            @AttributeOverride(name = "description", column = @Column(name = "product_description", length = 1000, nullable = false))
    })
    private List<StringPair> careProductsDescriptions;

    public SpaUnit(long id, BigDecimal price, String description, String type, boolean isHiddenFromClient,
                   int guestCapacity, Set<String> facilities, List<ImageRecord> images) {
        super(id, price, description, type, isHiddenFromClient, guestCapacity, facilities, images);
    }


}

