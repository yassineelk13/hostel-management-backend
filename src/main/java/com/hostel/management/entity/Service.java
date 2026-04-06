package com.hostel.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "services",
        indexes = {
                @Index(name = "idx_service_category",    columnList = "category"),
                @Index(name = "idx_service_active",      columnList = "isActive"),
                @Index(name = "idx_service_pricing_type", columnList = "pricingType")  // ✅ NEW
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Le nom du service est requis")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true, message = "Le prix ne peut pas être négatif")
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "La catégorie est requise")
    private ServiceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PriceType priceType = PriceType.FIXED;

    // ✅ NEW: determines if the service is charged per person or per room
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PricingType pricingType = PricingType.PER_PERSON;

    @Column(nullable = false)
    private boolean isActive = true;

    @ManyToMany(mappedBy = "services")
    @JsonIgnore
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

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

    // ✅ KEPT for backward compatibility (single-person or room-level calculation)
    public BigDecimal calculateTotalPrice(int numberOfNights) {
        return calculateTotalPrice(numberOfNights, 1);
    }

    // ✅ NEW: calculates price taking persons count into account
    // - PER_ROOM services (transport etc.)  → persons multiplier is always 1
    // - PER_PERSON services (surf, yoga...) → multiplied by numberOfPersons
    public BigDecimal calculateTotalPrice(int numberOfNights, int numberOfPersons) {
        int personMultiplier = (pricingType == PricingType.PER_ROOM) ? 1 : numberOfPersons;
        if (priceType == PriceType.PER_NIGHT) {
            return price
                    .multiply(BigDecimal.valueOf(numberOfNights))
                    .multiply(BigDecimal.valueOf(personMultiplier));
        }
        return price.multiply(BigDecimal.valueOf(personMultiplier));
    }

    // ========== ENUMS ==========

    @Getter
    @AllArgsConstructor
    public enum ServiceCategory {
        TRANSPORT("🚗", "Transport",   "Services de transport"),
        MEAL     ("🍽️", "Repas",       "Services de restauration"),
        ACTIVITY ("🎯", "Activités",   "Activités et excursions"),
        OTHER    ("📦", "Autres",      "Autres services");

        private final String icon;
        private final String displayName;
        private final String description;
    }

    @Getter
    @AllArgsConstructor
    public enum PriceType {
        FIXED    ("Prix fixe", "Tarif unique pour tout le séjour"),
        PER_NIGHT("Par nuit",  "Tarif multiplié par le nombre de nuits");

        private final String displayName;
        private final String description;
    }

    // ✅ NEW enum
    @Getter
    @AllArgsConstructor
    public enum PricingType {
        PER_PERSON("Par personne", "Tarif multiplié par le nombre de personnes — ex: surf, yoga, petit-déjeuner"),
        PER_ROOM  ("Par chambre",  "Tarif unique pour toute la chambre — ex: transport, navette");

        private final String displayName;
        private final String description;
    }
}