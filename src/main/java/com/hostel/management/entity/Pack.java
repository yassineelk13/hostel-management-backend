package com.hostel.management.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "packs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ✅ DORTOIR
    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal priceDortoir;       // Prix promo/nuit

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal regularPriceDortoir; // Prix barré/nuit

    // ✅ SINGLE
    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal priceSingle;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal regularPriceSingle;

    // ✅ DOUBLE
    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal priceDouble;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal regularPriceDouble;

    // ✅ Features incluses (texte libre)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pack_features", joinColumns = @JoinColumn(name = "pack_id"))
    @Column(name = "feature", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> includedFeatures = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pack_photos", joinColumns = @JoinColumn(name = "pack_id"))
    @Column(name = "photo_url")
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ✅ Helper : prix promo par room type
    public BigDecimal getPromoPrice(Room.RoomType roomType) {
        return switch (roomType) {
            case DORTOIR -> priceDortoir;
            case SINGLE  -> priceSingle;
            case DOUBLE  -> priceDouble;
        };
    }

    // ✅ Helper : prix regular par room type
    public BigDecimal getRegularPrice(Room.RoomType roomType) {
        return switch (roomType) {
            case DORTOIR -> regularPriceDortoir;
            case SINGLE  -> regularPriceSingle;
            case DOUBLE  -> regularPriceDouble;
        };
    }
}