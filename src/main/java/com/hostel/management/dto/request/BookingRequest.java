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
            message = "Le téléphone doit contenir uniquement des chiffres et symboles valides"
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

    // ✅ NEW: number of persons for SINGLE rooms (1 or 2)
    // DORTOIR: automatically equals bedIds.size() | DOUBLE: always 1
    @Min(value = 1, message = "Le nombre de personnes doit être au moins 1")
    @Max(value = 2, message = "Maximum 2 personnes par chambre")
    private int numberOfPersons = 1;

    @AssertTrue(message = "La date de départ doit être après la date d'arrivée")
    public boolean isCheckOutAfterCheckIn() {
        if (checkInDate == null || checkOutDate == null) return true;
        return checkOutDate.isAfter(checkInDate);
    }
}