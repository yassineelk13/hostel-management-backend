package com.hostel.management.service;

import com.hostel.management.dto.request.ServiceRequest;
import com.hostel.management.dto.response.ServiceResponse;  // ✅ AJOUTER
import com.hostel.management.entity.Service;
import com.hostel.management.exception.ResourceNotFoundException;
import com.hostel.management.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;  // ✅ AJOUTER
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;  // ✅ AJOUTER

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j  // ✅ AJOUTER
public class ServiceService {

    private final ServiceRepository serviceRepository;

    @Transactional
    public Service createService(ServiceRequest request) {
        Service service = Service.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .priceType(request.getPriceType())
                .isActive(true)
                .build();

        Service saved = serviceRepository.save(service);
        log.info("Service créé: {}", saved.getName());
        return saved;
    }

    @Transactional
    public Service updateService(Long id, ServiceRequest request) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service non trouvé"));

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        service.setCategory(request.getCategory());
        service.setPriceType(request.getPriceType());

        Service updated = serviceRepository.save(service);
        log.info("Service mis à jour: {}", updated.getId());
        return updated;
    }

    @Transactional
    public void deleteService(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service non trouvé"));

        service.setActive(false);
        serviceRepository.save(service);
        log.info("Service désactivé: {}", id);
    }

    public Service getServiceById(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service non trouvé"));
    }

    public List<Service> getAllServices() {
        return serviceRepository.findByIsActiveTrue();
    }

    public List<Service> getServicesByCategory(Service.ServiceCategory category) {
        return serviceRepository.findByCategory(category);
    }

    // ✅ Méthodes pour retourner des DTOs
    public List<ServiceResponse> getAllServicesAsResponse() {
        return serviceRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ServiceResponse getServiceByIdAsResponse(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service non trouvé"));
        return mapToResponse(service);
    }

    public List<ServiceResponse> getServicesByCategoryAsResponse(Service.ServiceCategory category) {
        return serviceRepository.findByCategoryAndIsActiveTrue(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ServiceResponse mapToResponse(Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .category(service.getCategory())
                .priceType(service.getPriceType())
                .isActive(service.isActive())
                .build();
    }
}
