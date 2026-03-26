package com.hostel.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PackRequest {

    @NotBlank(message = "Le nom du pack est obligatoire")
    private String name;

    private String description;

    // ✅ DORTOIR
    @NotNull(message = "Le prix promo DORTOIR est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal priceDortoir;

    @NotNull(message = "Le prix regular DORTOIR est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal regularPriceDortoir;

    // ✅ SINGLE
    @NotNull(message = "Le prix promo SINGLE est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal priceSingle;

    @NotNull(message = "Le prix regular SINGLE est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal regularPriceSingle;

    // ✅ DOUBLE
    @NotNull(message = "Le prix promo DOUBLE est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal priceDouble;

    @NotNull(message = "Le prix regular DOUBLE est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal regularPriceDouble;

    // ✅ Features texte libre
    private List<String> includedFeatures = new ArrayList<>();

    private List<String> photos = new ArrayList<>();
}