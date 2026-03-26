package com.hostel.management.dto.response;

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

    // ✅ Prix promo/nuit par room type
    private BigDecimal priceDortoir;
    private BigDecimal priceSingle;
    private BigDecimal priceDouble;

    // ✅ Prix regular/nuit par room type (barré)
    private BigDecimal regularPriceDortoir;
    private BigDecimal regularPriceSingle;
    private BigDecimal regularPriceDouble;

    // ✅ Réductions calculées automatiquement (%)
    private BigDecimal discountDortoir;
    private BigDecimal discountSingle;
    private BigDecimal discountDouble;

    private List<String> includedFeatures;
    private List<String> photos;
    private boolean isActive;
}