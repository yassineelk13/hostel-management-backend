package com.hostel.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "packs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "nightPrices")
@EqualsAndHashCode(exclude = "nightPrices")
public class Pack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ✅ NEW: prix supplémentaire par nuit pour la 2ème personne (SINGLE room)
    // Inclut toutes les activités PER_PERSON (surf, yoga...)
    // Nullable → si null ou 0, pas de surcharge 2ème personne
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal extraPersonPricePerNight = BigDecimal.ZERO;

    @OneToMany(mappedBy = "pack", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<PackNightPrice> nightPrices = new ArrayList<>();

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

    // ── Helpers ──────────────────────────────────────────────

    public BigDecimal getPromoPrice(Room.RoomType roomType, int nights) {
        return nightPrices.stream()
                .filter(p -> p.getRoomType() == roomType && p.getNights() == nights)
                .map(PackNightPrice::getPromoPrice)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getRegularPrice(Room.RoomType roomType, int nights) {
        return nightPrices.stream()
                .filter(p -> p.getRoomType() == roomType && p.getNights() == nights)
                .map(PackNightPrice::getRegularPrice)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getMinPromoPrice(Room.RoomType roomType) {
        return nightPrices.stream()
                .filter(p -> p.getRoomType() == roomType)
                .map(PackNightPrice::getPromoPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}