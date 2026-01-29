package com.hostel.management.dto.request;

import com.hostel.management.entity.Room;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotBlank(message = "Numéro de chambre est obligatoire")
    private String roomNumber;

    @NotNull(message = "Type de chambre est obligatoire")
    private Room.RoomType roomType;

    private String description;

    @NotNull(message = "Prix par nuit est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Prix doit être positif")
    private BigDecimal pricePerNight;

    private List<String> photos;

    @Min(value = 1, message = "Nombre de lits doit être au moins 1")
    private Integer numberOfBeds;
}
