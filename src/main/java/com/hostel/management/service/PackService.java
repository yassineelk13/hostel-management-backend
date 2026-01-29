package com.hostel.management.service;

import com.hostel.management.dto.request.PackRequest;
import com.hostel.management.dto.response.PackResponse;
import com.hostel.management.entity.Pack;
import com.hostel.management.entity.Room;
import com.hostel.management.entity.Service;
import com.hostel.management.exception.ResourceNotFoundException;
import com.hostel.management.repository.PackRepository;
import com.hostel.management.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

@Component // ✅ Changé de @Service à @Component pour éviter conflit de noms
@RequiredArgsConstructor
@Slf4j
public class PackService {

    private final PackRepository packRepository;
    private final ServiceRepository serviceRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public Pack createPack(PackRequest request) {
        List<Service> services = new ArrayList<>();
        if (request.getIncludedServiceIds() != null) {
            services = serviceRepository.findAllById(request.getIncludedServiceIds());
        }

        List<String> uploadedPhotos = uploadPhotosToCloudinary(request.getPhotos(), "shamshouse/packs");

        Pack pack = Pack.builder()
                .name(request.getName())
                .description(request.getDescription())
                .durationDays(request.getDurationDays())
                .originalPrice(request.getOriginalPrice())
                .promoPrice(request.getPromoPrice())
                .roomType(request.getRoomType())
                .photos(uploadedPhotos)
                .includedServices(services)
                .isActive(true)
                .build();

        return packRepository.save(pack);
    }

    @Transactional
    public Pack updatePack(Long id, PackRequest request) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));

        pack.setName(request.getName());
        pack.setDescription(request.getDescription());
        pack.setDurationDays(request.getDurationDays());
        pack.setOriginalPrice(request.getOriginalPrice());
        pack.setPromoPrice(request.getPromoPrice());
        pack.setRoomType(request.getRoomType());

        if (request.getPhotos() != null) {
            if (pack.getPhotos() != null) {
                pack.getPhotos().forEach(photoUrl -> {
                    try {
                        cloudinaryService.deleteImage(photoUrl);
                    } catch (Exception e) {
                        log.warn("Failed to delete old pack photo: {}", photoUrl);
                    }
                });
            }

            List<String> uploadedPhotos = uploadPhotosToCloudinary(request.getPhotos(), "shamshouse/packs");
            pack.setPhotos(uploadedPhotos);
        }

        if (request.getIncludedServiceIds() != null) {
            List<Service> services = serviceRepository.findAllById(request.getIncludedServiceIds());
            pack.setIncludedServices(services);
        }

        return packRepository.save(pack);
    }

    @Transactional
    public void deletePack(Long id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));

        if (pack.getPhotos() != null) {
            pack.getPhotos().forEach(photoUrl -> {
                try {
                    cloudinaryService.deleteImage(photoUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete photo during pack deletion: {}", photoUrl);
                }
            });
        }

        pack.setActive(false);
        packRepository.save(pack);
    }

    // ✅ AJOUTER @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public Pack getPackById(Long id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));

        // ✅ Force le chargement des services
        if (pack.getIncludedServices() != null) {
            pack.getIncludedServices().size();
        }

        return pack;
    }

    // ✅ AJOUTER @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public List<Pack> getAllPacks() {
        List<Pack> packs = packRepository.findByIsActiveTrue();

        // ✅ Force le chargement des services pour chaque pack
        packs.forEach(pack -> {
            if (pack.getIncludedServices() != null) {
                pack.getIncludedServices().size();
            }
        });

        return packs;
    }

    // ✅ AJOUTER @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public List<PackResponse> getAllPacksAsResponse() {
        List<Pack> packs = packRepository.findByIsActiveTrue();

        return packs.stream()
                .map(pack -> {
                    // ✅ Force le chargement des services
                    if (pack.getIncludedServices() != null) {
                        pack.getIncludedServices().size();
                    }

                    // ✅✅✅ AJOUTE CETTE PARTIE : Force le chargement des photos
                    if (pack.getPhotos() != null) {
                        pack.getPhotos().size();
                    }

                    return mapToResponse(pack);
                })
                .collect(Collectors.toList());
    }

    // ✅ AJOUTER @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public PackResponse getPackByIdAsResponse(Long id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));

        // ✅ Force le chargement
        if (pack.getIncludedServices() != null) {
            pack.getIncludedServices().size();
        }

        return mapToResponse(pack);
    }

    // ✅ AJOUTER @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public List<PackResponse> getPacksByRoomType(String roomType) {
        Room.RoomType type = Room.RoomType.valueOf(roomType.toUpperCase());
        List<Pack> packs = packRepository.findByRoomTypeAndIsActiveTrue(type);

        return packs.stream()
                .map(pack -> {
                    // ✅ Force le chargement
                    if (pack.getIncludedServices() != null) {
                        pack.getIncludedServices().size();
                    }
                    return mapToResponse(pack);
                })
                .collect(Collectors.toList());
    }

    // ✅ MÉTHODE DE MAPPING SÉCURISÉE
    private PackResponse mapToResponse(Pack pack) {
        List<PackResponse.ServiceInfo> serviceInfos = new ArrayList<>();

        // ✅ Vérifier que la collection est chargée
        if (pack.getIncludedServices() != null) {
            serviceInfos = pack.getIncludedServices().stream()
                    .map(service -> PackResponse.ServiceInfo.builder()
                            .id(service.getId())
                            .name(service.getName())
                            .description(service.getDescription())
                            .price(service.getPrice())
                            .category(service.getCategory())
                            .build())
                    .collect(Collectors.toList());
        }

        return PackResponse.builder()
                .id(pack.getId())
                .name(pack.getName())
                .description(pack.getDescription())
                .durationDays(pack.getDurationDays())
                .originalPrice(pack.getOriginalPrice())
                .promoPrice(pack.getPromoPrice())
                .roomType(pack.getRoomType())
                .photos(pack.getPhotos() != null ? pack.getPhotos() : new ArrayList<>())
                .includedServices(serviceInfos)
                .isActive(pack.isActive())
                .build();
    }

    private List<String> uploadPhotosToCloudinary(List<String> photos, String folder) {
        if (photos == null || photos.isEmpty()) {
            return new ArrayList<>();
        }

        List<CompletableFuture<String>> uploadFutures = photos.stream()
                .map(photo -> CompletableFuture.supplyAsync(() -> {
                    try {
                        if (photo.startsWith("data:image")) {
                            String url = cloudinaryService.uploadBase64Image(photo, folder);
                            log.info("Base64 image uploaded to Cloudinary: {}", url);
                            return url;
                        } else if (photo.startsWith("http")) {
                            log.info("URL kept as is: {}", photo);
                            return photo;
                        } else {
                            log.warn("Unknown photo format, skipping: {}",
                                    photo.substring(0, Math.min(50, photo.length())));
                            return null;
                        }
                    } catch (Exception e) {
                        log.error("Failed to upload photo to Cloudinary", e);
                        return null;
                    }
                }))
                .collect(Collectors.toList());

        return uploadFutures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
