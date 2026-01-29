package com.hostel.management.dto.request;

import com.hostel.management.entity.Room;
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

    @NotNull(message = "La durée est obligatoire")
    private Integer durationDays;

    // ✅ AJOUTER CE CHAMP
    private BigDecimal originalPrice;

    @NotNull(message = "Le prix promo est obligatoire")
    private BigDecimal promoPrice;

    @NotNull(message = "Le type de chambre est obligatoire")
    private Room.RoomType roomType;

    private List<String> photos = new ArrayList<>();

    private List<Long> includedServiceIds = new ArrayList<>();
}
