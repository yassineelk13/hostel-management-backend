package com.hostel.management.controller;

import com.hostel.management.dto.request.RoomRequest;
import com.hostel.management.dto.response.ApiResponse;
import com.hostel.management.dto.response.AvailabilityResponse;
import com.hostel.management.dto.response.RoomResponse;
import com.hostel.management.service.AvailabilityService;
import com.hostel.management.service.RoomService;
import com.hostel.management.service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;
    private final AvailabilityService availabilityService;
    private final CloudinaryService cloudinaryService;

    // ========== ENDPOINTS PUBLICS ==========

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAllRooms() {
        log.info("GET /api/rooms - Récupération de toutes les chambres");
        List<RoomResponse> rooms = roomService.getAllRooms();
        log.info("✅ {} chambres récupérées", rooms.size());
        return ResponseEntity.ok(ApiResponse.success("Chambres récupérées avec succès", rooms));
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long id) {
        RoomResponse room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success("Chambre récupérée avec succès", room));
    }

    @GetMapping("/rooms/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {

        List<RoomResponse> rooms = roomService.getAvailableRooms(checkIn, checkOut);
        log.info("Recherche de disponibilité: {} à {}, {} chambres trouvées",
                checkIn, checkOut, rooms.size());

        return ResponseEntity.ok(ApiResponse.success("Chambres disponibles récupérées", rooms));
    }

    @GetMapping("/rooms/{id}/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {

        AvailabilityResponse availability = availabilityService.checkRoomAvailability(id, checkIn, checkOut);
        return ResponseEntity.ok(ApiResponse.success("Disponibilité vérifiée", availability));
    }

    // ========== ENDPOINTS ADMIN (protégés par SecurityConfig) ==========

    @PostMapping("/rooms/upload-photo")
    public ResponseEntity<ApiResponse<String>> uploadPhoto(
            @RequestPart("photo") MultipartFile photo) {

        log.info("📤 Upload photo: {}", photo.getOriginalFilename());

        if (photo.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Fichier vide"));
        }

        try {
            String url = cloudinaryService.uploadImage(photo, "shamshouse/rooms");
            log.info("✅ Photo uploadée avec succès: {}", url);
            return ResponseEntity.ok(ApiResponse.success("Photo uploadée", url));
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'upload de la photo", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur upload: " + e.getMessage()));
        }
    }

    @DeleteMapping("/rooms/delete-photo")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(@RequestParam String photoUrl) {
        try {
            cloudinaryService.deleteImage(photoUrl);
            log.info("✅ Photo supprimée: {}", photoUrl);
            return ResponseEntity.ok(ApiResponse.success("Photo supprimée", null));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression de la photo", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur suppression: " + e.getMessage()));
        }
    }

    // ✅ NOUVELLE MÉTHODE : Créer une chambre avec URLs de photos
    @PostMapping("/rooms/create-with-urls")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoomWithUrls(
            @Valid @RequestBody RoomRequest request) {

        log.info("🆕 Création d'une chambre avec URLs: {}", request.getRoomNumber());

        try {
            RoomResponse room = roomService.createRoom(request);
            log.info("✅ Chambre créée avec succès: {}", room.getRoomNumber());
            return ResponseEntity.ok(ApiResponse.success("Chambre créée avec succès", room));
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de la chambre", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur création: " + e.getMessage()));
        }
    }
}
