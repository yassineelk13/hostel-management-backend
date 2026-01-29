package com.hostel.management.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "beds",
        indexes = {
                @Index(name = "idx_bed_room", columnList = "room_id"),
                @Index(name = "idx_bed_available", columnList = "isAvailable")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_room_bed_number",
                        columnNames = {"room_id", "bedNumber"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @JsonBackReference  // ✅ Empêche la sérialisation de room (évite boucle)
    @ToString.Exclude  // ✅ Évite boucle dans toString()
    private Room room;

    @Column(nullable = false, length = 10)
    private String bedNumber;

    @Column(nullable = false)
    @Builder.Default
    private boolean isAvailable = true;

    @ManyToMany(mappedBy = "beds", fetch = FetchType.LAZY)
    @JsonIgnore  // ✅ Ne pas sérialiser les bookings
    @ToString.Exclude  // ✅ Évite boucle dans toString()
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

    // ✅ MÉTHODE TRANSIENT (ne cause pas de LazyInitializationException)
    @Transient
    @JsonIgnore
    public String getRoomNumber() {
        return room != null ? room.getRoomNumber() : null;
    }
}
