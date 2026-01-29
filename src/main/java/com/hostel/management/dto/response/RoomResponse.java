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
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private Room.RoomType roomType;
    private String description;
    private BigDecimal pricePerNight;
    private List<String> photos;
    private int totalBeds;
    private int availableBeds;
    private boolean isActive;
    private List<BedResponse> beds;  // ✅ AJOUTER CETTE LIGNE
}
