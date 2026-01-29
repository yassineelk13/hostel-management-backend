package com.hostel.management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityResponse {
    private Long roomId;
    private String roomNumber;
    private boolean isAvailable;
    private int availableBeds;
    private LocalDate nextAvailableDate;
    private List<Long> availableBedIds;
}
