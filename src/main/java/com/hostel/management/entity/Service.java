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
                @Index(name = "idx_service_category", columnList = "category"),
                @Index(name = "idx_service_active", columnList = "isActive")
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

    @Column(nullable = false)
    private boolean isActive = true;

    // ✅ AJOUTER relations inverses avec @JsonIgnore
    @ManyToMany(mappedBy = "includedServices")
    @JsonIgnore // ✅ Ne pas sérialiser les packs dans le JSON des services
    @Builder.Default
    private List<Pack> packs = new ArrayList<>();

    @ManyToMany(mappedBy = "services")
    @JsonIgnore // ✅ Ne pas sérialiser les bookings dans le JSON des services
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

    // ✅ Méthode pour calculer le prix total
    public BigDecimal calculateTotalPrice(int numberOfNights) {
        if (priceType == PriceType.PER_NIGHT) {
            return price.multiply(BigDecimal.valueOf(numberOfNights));
        }
        return price;
    }

    @Getter
    @AllArgsConstructor
    public enum ServiceCategory {
        TRANSPORT("🚗", "Transport", "Services de transport"),
        MEAL("🍽️", "Repas", "Services de restauration"),
        ACTIVITY("🎯", "Activités", "Activités et excursions"),
        OTHER("📦", "Autres", "Autres services");

        private final String icon;
        private final String displayName;
        private final String description;
    }

    @Getter
    @AllArgsConstructor
    public enum PriceType {
        FIXED("Prix fixe", "Tarif unique pour tout le séjour"),
        PER_NIGHT("Par nuit", "Tarif multiplié par le nombre de nuits");

        private final String displayName;
        private final String description;
    }
}
