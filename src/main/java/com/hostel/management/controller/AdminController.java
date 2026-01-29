package com.hostel.management.controller;

import com.hostel.management.dto.request.PackRequest;
import com.hostel.management.dto.request.RoomRequest;
import com.hostel.management.dto.request.ServiceRequest;
import com.hostel.management.dto.response.ApiResponse;
import com.hostel.management.dto.response.BookingResponse;
import com.hostel.management.dto.response.RoomResponse;
import com.hostel.management.entity.Booking;
import com.hostel.management.entity.HostelSettings;
import com.hostel.management.entity.Pack;
import com.hostel.management.entity.Service;
import com.hostel.management.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final ServiceService serviceService;
    private final PackService packService;
    private final HostelSettingsService settingsService;

    // ===== ROOM MANAGEMENT =====

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody RoomRequest request) {
        RoomResponse room = roomService.createRoom(request);
        return ResponseEntity.ok(ApiResponse.success("Chambre créée avec succès", room));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request) {
        RoomResponse room = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.success("Chambre mise à jour", room));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Chambre supprimée", null));
    }

    // ===== BOOKING MANAGEMENT =====

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings() {
        List<BookingResponse> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(ApiResponse.success("Réservations récupérées", bookings));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(@PathVariable Long id) {
        BookingResponse booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success("Réservation récupérée", booking));
    }

    @GetMapping("/bookings/checkins")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCheckInsForToday() {
        List<BookingResponse> bookings = bookingService.getCheckInsForDate(LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success("Check-ins du jour récupérés", bookings));
    }

    @GetMapping("/bookings/checkouts")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCheckOutsForToday() {
        List<BookingResponse> bookings = bookingService.getCheckOutsForDate(LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success("Check-outs du jour récupérés", bookings));
    }

    @GetMapping("/bookings/checkins/{date}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCheckInsForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<BookingResponse> bookings = bookingService.getCheckInsForDate(date);
        return ResponseEntity.ok(ApiResponse.success("Check-ins récupérés", bookings));
    }

    @GetMapping("/bookings/checkouts/{date}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCheckOutsForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<BookingResponse> bookings = bookingService.getCheckOutsForDate(date);
        return ResponseEntity.ok(ApiResponse.success("Check-outs récupérés", bookings));
    }

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam Booking.BookingStatus status) {
        BookingResponse booking = bookingService.updateBookingStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Statut de réservation mis à jour", booking));
    }

    @PutMapping("/bookings/{id}/payment")
    public ResponseEntity<ApiResponse<BookingResponse>> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam Booking.PaymentStatus paymentStatus) {
        BookingResponse booking = bookingService.updatePaymentStatus(id, paymentStatus);
        return ResponseEntity.ok(ApiResponse.success("Statut de paiement mis à jour", booking));
    }

    // ✅ UNE SEULE MÉTHODE DELETE - Suppression définitive
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBooking(@PathVariable Long id) {
        log.info("🗑️ Suppression définitive de la réservation ID: {}", id);
        bookingService.deleteBooking(id);
        return ResponseEntity.ok(ApiResponse.success("Réservation supprimée définitivement", null));
    }

    // ===== SERVICE MANAGEMENT =====

    @PostMapping("/services")
    public ResponseEntity<ApiResponse<Service>> createService(@Valid @RequestBody ServiceRequest request) {
        Service service = serviceService.createService(request);
        return ResponseEntity.ok(ApiResponse.success("Service créé avec succès", service));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<ApiResponse<Service>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        Service service = serviceService.updateService(id, request);
        return ResponseEntity.ok(ApiResponse.success("Service mis à jour", service));
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.success("Service supprimé", null));
    }

    // ===== PACK MANAGEMENT =====

    @PostMapping("/packs")
    public ResponseEntity<ApiResponse<Pack>> createPack(@Valid @RequestBody PackRequest request) {
        Pack pack = packService.createPack(request);
        return ResponseEntity.ok(ApiResponse.success("Pack créé avec succès", pack));
    }

    @PutMapping("/packs/{id}")
    public ResponseEntity<ApiResponse<Pack>> updatePack(
            @PathVariable Long id,
            @Valid @RequestBody PackRequest request) {
        Pack pack = packService.updatePack(id, request);
        return ResponseEntity.ok(ApiResponse.success("Pack mis à jour", pack));
    }

    @DeleteMapping("/packs/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePack(@PathVariable Long id) {
        packService.deletePack(id);
        return ResponseEntity.ok(ApiResponse.success("Pack supprimé", null));
    }

    // ===== HOSTEL SETTINGS =====

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<HostelSettings>> getSettings() {
        HostelSettings settings = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success("Paramètres récupérés", settings));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<HostelSettings>> updateSettings(
            @RequestBody HostelSettings settings) {
        HostelSettings updated = settingsService.updateSettings(settings);
        return ResponseEntity.ok(ApiResponse.success("Paramètres mis à jour", updated));
    }

    @PutMapping("/settings/door-code")
    public ResponseEntity<ApiResponse<HostelSettings>> updateDoorCode(@RequestParam String newCode) {
        HostelSettings settings = settingsService.updateDoorCode(newCode);
        return ResponseEntity.ok(ApiResponse.success("Code de porte mis à jour", settings));
    }
}
