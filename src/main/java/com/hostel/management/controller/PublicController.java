package com.hostel.management.controller;

import com.hostel.management.dto.response.ApiResponse;
import com.hostel.management.entity.HostelSettings;
import com.hostel.management.service.HostelSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")  // ✅ CHANGÉ de /api/public à /api
@RequiredArgsConstructor
public class PublicController {

    private final HostelSettingsService settingsService;

    // ✅ NOUVEAU : Endpoint public pour récupérer les settings
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<HostelSettings>> getPublicSettings() {
        HostelSettings settings = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success("Paramètres récupérés", settings));
    }

    @GetMapping("/public/hostel-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHostelInfo() {
        HostelSettings settings = settingsService.getSettings();

        Map<String, Object> info = new HashMap<>();
        info.put("name", settings.getHostelName());
        info.put("address", settings.getAddress());
        info.put("email", settings.getEmail());
        info.put("phone", settings.getPhone());
        info.put("checkIn24h", settings.isCheckIn24h());
        info.put("checkInInstructions", settings.getCheckInInstructions());
        info.put("checkOutTime", settings.getCheckOutTime());

        return ResponseEntity.ok(ApiResponse.success("Informations de l'hostel", info));
    }

    @GetMapping("/public/policies")
    public ResponseEntity<ApiResponse<Map<String, String>>> getHostelPolicies() {
        HostelSettings settings = settingsService.getSettings();

        Map<String, String> policies = new HashMap<>();
        policies.put("checkInPolicy", "✅ Check-in disponible 24h/24, 7j/7. Vous pouvez arriver à n'importe quelle heure en utilisant votre code d'accès personnel.");
        policies.put("checkOutPolicy", "❌ Check-out obligatoire avant " + settings.getCheckOutTime() + ". Veuillez libérer votre lit avant cette heure.");
        policies.put("paymentPolicy", "💰 Le paiement s'effectue à l'arrivée (espèces ou carte bancaire).");
        policies.put("accessPolicy", "🔑 Vous recevrez un code d'accès unique par email après votre réservation.");
        policies.put("cancellationPolicy", "📅 Annulation gratuite jusqu'à 24h avant l'arrivée.");

        return ResponseEntity.ok(ApiResponse.success("Politiques de l'hostel", policies));
    }
}
