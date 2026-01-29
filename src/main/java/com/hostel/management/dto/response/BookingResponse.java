package com.hostel.management.dto.response;

import com.hostel.management.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;  // ✅ AJOUTER
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private Booking.BookingStatus status;
    private Booking.PaymentStatus paymentStatus;
    private String accessCode;
    private List<BedInfo> beds;
    private List<ServiceInfo> services;
    private PackInfo pack;
    private String notes;
    private LocalDateTime createdAt;  // ✅ AJOUTER CETTE LIGNE

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BedInfo {
        private Long bedId;
        private String bedNumber;
        private String roomNumber;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServiceInfo {
        private Long serviceId;
        private String name;
        private BigDecimal price;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PackInfo {
        private Long packId;
        private String name;
        private Integer durationDays;
        private BigDecimal promoPrice;
    }
}
