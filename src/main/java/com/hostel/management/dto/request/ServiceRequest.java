package com.hostel.management.dto.request;

import com.hostel.management.entity.Service;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceRequest {
    @NotBlank(message = "Nom du service est obligatoire")
    private String name;

    private String description;

    @NotNull(message = "Prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Prix doit être positif")
    private BigDecimal price;

    @NotNull(message = "Catégorie est obligatoire")
    private Service.ServiceCategory category;

    // ✅ NOUVEAU CHAMP AJOUTÉ
    @NotNull(message = "Type de prix est obligatoire")
    private Service.PriceType priceType = Service.PriceType.FIXED;  // Valeur par défaut
}
