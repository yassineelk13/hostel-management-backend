package com.hostel.management.dto.response;

import com.hostel.management.entity.Service;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Service.ServiceCategory category;
    private Service.PriceType priceType;
    // ✅ NEW: exposed to frontend so it knows PER_PERSON vs PER_ROOM
    private Service.PricingType pricingType;
    private boolean isActive;
}