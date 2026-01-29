package com.hostel.management.controller;

import com.hostel.management.dto.response.ApiResponse;
import com.hostel.management.dto.response.PackResponse;
import com.hostel.management.service.PackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api") // ✅ CHANGÉ ICI
@RequiredArgsConstructor
@Slf4j
public class PackController {

    private final PackService packService;

    @GetMapping("/packs") // ✅ AJOUTER /packs ICI
    public ResponseEntity<ApiResponse<List<PackResponse>>> getAllPacks() {
        log.info("GET /api/packs - Récupération de tous les packs");
        try {
            List<PackResponse> packs = packService.getAllPacksAsResponse();
            log.info("Packs récupérés: {}", packs.size());
            return ResponseEntity.ok(ApiResponse.success("Packs récupérés avec succès", packs));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des packs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/packs/{id}") // ✅ AJOUTER /packs ICI
    public ResponseEntity<ApiResponse<PackResponse>> getPackById(@PathVariable Long id) {
        PackResponse pack = packService.getPackByIdAsResponse(id);
        return ResponseEntity.ok(ApiResponse.success("Pack récupéré avec succès", pack));
    }

    @GetMapping("/packs/type/{roomType}") // ✅ AJOUTER /packs ICI
    public ResponseEntity<ApiResponse<List<PackResponse>>> getPacksByRoomType(
            @PathVariable String roomType) {
        List<PackResponse> packs = packService.getPacksByRoomType(roomType);
        return ResponseEntity.ok(ApiResponse.success("Packs récupérés par type", packs));
    }
}
