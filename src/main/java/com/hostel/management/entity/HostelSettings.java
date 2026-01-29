package com.hostel.management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hostel_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostelSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String hostelName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 10)
    private String doorCode;

    private String wifiPassword;

    // Check-in est 24/7 - pas de restriction d'heure
    @Column(nullable = false)
    private boolean checkIn24h = true;

    private String checkInInstructions = "Check-in disponible 24h/24. Utilisez votre code d'accès pour entrer.";

    // Check-out a une heure fixe
    @Column(nullable = false)
    private String checkOutTime = "12:00";

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
}
