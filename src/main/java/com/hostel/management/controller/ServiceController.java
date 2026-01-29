package com.hostel.management.controller;

import com.hostel.management.dto.response.ApiResponse;
import com.hostel.management.dto.response.ServiceResponse;
import com.hostel.management.entity.Service;
import com.hostel.management.service.ServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api") // ✅ CHANGÉ ICI
@RequiredArgsConstructor
@Slf4j
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping("/services") // ✅ AJOUTER /services ICI
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getAllServices() {
        log.info("GET /api/services - Récupération de tous les services");
        List<ServiceResponse> services = serviceService.getAllServicesAsResponse();
        return ResponseEntity.ok(ApiResponse.success("Services récupérés avec succès", services));
    }

    @GetMapping("/services/{id}") // ✅ AJOUTER /services ICI
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceById(@PathVariable Long id) {
        ServiceResponse service = serviceService.getServiceByIdAsResponse(id);
        return ResponseEntity.ok(ApiResponse.success("Service récupéré avec succès", service));
    }

    @GetMapping("/services/category/{category}") // ✅ AJOUTER /services ICI
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getServicesByCategory(
            @PathVariable Service.ServiceCategory category) {
        List<ServiceResponse> services = serviceService.getServicesByCategoryAsResponse(category);
        return ResponseEntity.ok(ApiResponse.success("Services récupérés par catégorie", services));
    }
}
