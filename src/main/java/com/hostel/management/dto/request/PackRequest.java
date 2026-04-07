package com.hostel.management.dto.request;

import com.hostel.management.entity.Room;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal extraPersonPricePerNight = BigDecimal.ZERO;

    // ✅ NOUVEAU : liste des prix par nuits
    private List<NightPriceRequest> nightPrices = new ArrayList<>();

    private List<String> includedFeatures = new ArrayList<>();
    private List<String> photos = new ArrayList<>();

    // ✅ Sous-objet pour chaque ligne de prix
    @Data
    public static class NightPriceRequest {
        @NotNull
        private Integer nights; // 3 à 10

        @NotNull
        @Enumerated(EnumType.STRING)
        private Room.RoomType roomType;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal promoPrice;

        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal regularPrice;
    }
}