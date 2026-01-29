package com.hostel.management.dto.response;

import com.hostel.management.entity.Room;
import com.hostel.management.entity.Service;
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
    private Integer durationDays;
    private BigDecimal originalPrice;
    private BigDecimal promoPrice;
    private BigDecimal discount;  // ✅ Calculé : (original - promo) / original * 100
    private Room.RoomType roomType;
    private List<String> photos;
    private List<ServiceInfo> includedServices;
    private boolean isActive;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceInfo {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Service.ServiceCategory category;
    }

    // ✅ Méthode helper pour calculer le discount
    public BigDecimal getDiscount() {
        if (originalPrice != null && promoPrice != null &&
                originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            return originalPrice.subtract(promoPrice)
                    .divide(originalPrice, 2, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
}
