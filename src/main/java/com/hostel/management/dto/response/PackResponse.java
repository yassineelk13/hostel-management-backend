package com.hostel.management.dto.response;

import com.hostel.management.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PackResponse {
    private Long id;
    private String name;
    private String description;

    // ✅ NOUVEAU : liste des prix par nuits
    private List<NightPriceResponse> nightPrices;

    // ✅ Prix minimum par room type (pour affichage "à partir de")
    private BigDecimal minPriceDortoir;
    private BigDecimal minPriceSingle;
    private BigDecimal minPriceDouble;

    private List<String> includedFeatures;
    private List<String> photos;
    private boolean isActive;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NightPriceResponse {
        private int nights;
        private Room.RoomType roomType;
        private BigDecimal promoPrice;
        private BigDecimal regularPrice;
    }
}