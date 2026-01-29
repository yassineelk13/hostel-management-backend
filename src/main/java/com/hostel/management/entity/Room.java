package com.hostel.management.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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
        name = "rooms",
        indexes = {
                @Index(name = "idx_room_type", columnList = "roomType"),
                @Index(name = "idx_room_active", columnList = "isActive")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    @NotBlank(message = "Le numéro de chambre est requis")
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Le type de chambre est requis")
    private RoomType roomType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être positif")
    private BigDecimal pricePerNight;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "room_photos",
            joinColumns = @JoinColumn(name = "room_id")
    )
    @Column(name = "photo_url", length = 500)
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @OneToMany(
            mappedBy = "room",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonManagedReference  // ✅ Permet la sérialisation des beds
    @ToString.Exclude  // ✅ Évite boucle dans toString()
    @Builder.Default
    private List<Bed> beds = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
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

    // ✅ Méthodes utilitaires
    public int getTotalBeds() {
        return beds != null ? beds.size() : 0;
    }

    public int getCapacity() {
        return roomType != null ? roomType.getCapacity() : 0;
    }

    public int getAvailableBedsCount() {
        if (beds == null) return 0;
        return (int) beds.stream()
                .filter(Bed::isAvailable)
                .count();
    }

    public boolean hasAvailableBeds() {
        return getAvailableBedsCount() > 0;
    }

    @Getter
    @AllArgsConstructor
    public enum RoomType {
        DOUBLE(1, "Chambre Double", "1 grand lit double"),
        SINGLE(2, "Chambre Simple", "2 lits simples"),
        DORTOIR(8, "Dortoir", "8 lits superposés");

        private final int capacity;
        private final String displayName;
        private final String description;
    }
}
