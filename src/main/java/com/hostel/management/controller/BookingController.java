package com.hostel.management.controller;

import com.hostel.management.dto.request.BookingRequest;
import com.hostel.management.dto.response.ApiResponse;
import com.hostel.management.dto.response.BookingResponse;
import com.hostel.management.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/bookings")  // ✅ Garde ça
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping  // ✅ /api/bookings (vide = racine)
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request) {
        BookingResponse booking = bookingService.createBooking(request);
        return ResponseEntity.ok(ApiResponse.success("Réservation créée avec succès", booking));
    }

    @GetMapping  // ✅ /api/bookings (vide = racine)
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
        log.info("📋 GET /api/bookings");
        List<BookingResponse> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(ApiResponse.success("Réservations récupérées", bookings));
    }

    @GetMapping("/reference/{reference}")  // ✅ /api/bookings/reference/XXX
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @PathVariable String reference) {
        BookingResponse booking = bookingService.getBookingByReference(reference);
        return ResponseEntity.ok(ApiResponse.success("Réservation récupérée", booking));
    }

    @GetMapping("/code/{code}")  // ✅ /api/bookings/code/XXX
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByAccessCode(
            @PathVariable String code) {
        BookingResponse booking = bookingService.getBookingByAccessCode(code);
        return ResponseEntity.ok(ApiResponse.success("Réservation récupérée", booking));
    }
}
