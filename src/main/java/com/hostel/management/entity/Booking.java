package com.hostel.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_booking_dates", columnList = "checkInDate, checkOutDate"),
                @Index(name = "idx_booking_status", columnList = "status"),
                @Index(name = "idx_booking_payment", columnList = "paymentStatus"),
                @Index(name = "idx_booking_email", columnList = "guestEmail"),
                @Index(name = "idx_booking_reference", columnList = "bookingReference"),
                @Index(name = "idx_booking_access_code", columnList = "accessCode")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Le nom du client est requis")
    private String guestName;

    @Column(nullable = false, length = 100)
    @Email(message = "Email invalide")
    @NotBlank(message = "L'email est requis")
    private String guestEmail;

    @Column(nullable = false, length = 20)
    @NotBlank(message = "Le téléphone est requis")
    private String guestPhone;

    @Column(nullable = false)
    @NotNull(message = "La date d'arrivée est requise")
    private LocalDate checkInDate;

    @Column(nullable = false)
    @NotNull(message = "La date de départ est requise")
    private LocalDate checkOutDate;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être positif")
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(unique = true, nullable = false, length = 10)
    private String accessCode;

    @Column(unique = true, nullable = false, length = 20)
    private String bookingReference;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "booking_beds",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "bed_id")
    )
    @JsonIgnoreProperties({"room", "bookings"})
    @Builder.Default
    private List<Bed> beds = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "booking_services",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @JsonIgnoreProperties({"packs", "bookings"})
    @Builder.Default
    private List<Service> services = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id")
    @JsonIgnoreProperties({"includedServices"})
    private Pack pack;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ✅ CORRECTION : Initialisation obligatoire à 0L
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;  // ✅ CHANGÉ ICI

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (version == null) {  // ✅ SÉCURITÉ SUPPLÉMENTAIRE
            version = 0L;
        }
        validateDates();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateDates();
    }

    private void validateDates() {
        if (checkInDate != null && checkOutDate != null) {
            if (checkOutDate.isBefore(checkInDate) || checkOutDate.isEqual(checkInDate)) {
                throw new IllegalArgumentException(
                        "La date de départ doit être après la date d'arrivée"
                );
            }
        }
    }

    public long getNumberOfNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public boolean isActive() {
        return status != BookingStatus.CANCELLED && status != BookingStatus.CHECKED_OUT;
    }

    public boolean isCurrentlyStaying() {
        LocalDate today = LocalDate.now();
        return status == BookingStatus.CHECKED_IN &&
                !today.isBefore(checkInDate) &&
                !today.isAfter(checkOutDate);
    }

    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        CHECKED_IN,
        CHECKED_OUT,
        CANCELLED
    }

    public enum PaymentStatus {
        UNPAID,
        PARTIAL,
        PAID
    }
}
