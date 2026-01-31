package com.hostel.management.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BookingRequest {

    @NotBlank(message = "Nom du client est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String guestName;

    @NotBlank(message = "Email du client est obligatoire")
    @Email(message = "Format email invalide")
    private String guestEmail;

    @NotBlank(message = "Téléphone du client est obligatoire")
    @Pattern(
            regexp = "^[+0-9][0-9\\s\\-().]{7,19}$",
            message = "Le téléphone doit contenir uniquement des chiffres et symboles valides (+, -, espaces, parenthèses)"
    )
    private String guestPhone;


    @NotNull(message = "Date d'arrivée est obligatoire")
    @FutureOrPresent(message = "Date d'arrivée doit être aujourd'hui ou future")
    private LocalDate checkInDate;

    @NotNull(message = "Date de départ est obligatoire")
    @Future(message = "Date de départ doit être future")
    private LocalDate checkOutDate;

    @NotEmpty(message = "Au moins un lit doit être sélectionné")
    @Size(min = 1, max = 10, message = "Vous pouvez réserver entre 1 et 10 lits")
    private List<Long> bedIds;

    @Size(max = 20, message = "Maximum 20 services peuvent être ajoutés")
    private List<Long> serviceIds;

    private Long packId;

    @Size(max = 1000, message = "Les notes ne peuvent pas dépasser 1000 caractères")
    private String notes;

    // ✅ Validation custom pour vérifier checkOut > checkIn
    @AssertTrue(message = "La date de départ doit être après la date d'arrivée")
    public boolean isCheckOutAfterCheckIn() {
        if (checkInDate == null || checkOutDate == null) {
            return true; // Laisse @NotNull gérer ça
        }
        return checkOutDate.isAfter(checkInDate);
    }
}
