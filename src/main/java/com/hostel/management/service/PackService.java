package com.hostel.management.service;

import com.hostel.management.dto.request.PackRequest;
import com.hostel.management.dto.response.PackResponse;
import com.hostel.management.entity.Pack;
import com.hostel.management.exception.ResourceNotFoundException;
import com.hostel.management.repository.PackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PackService {

    private final PackRepository packRepository;
    private final CloudinaryService cloudinaryService;
    // ✅ ServiceRepository supprimé — plus besoin

    @Transactional
    public Pack createPack(PackRequest request) {
        List<String> uploadedPhotos = uploadPhotosToCloudinary(request.getPhotos(), "shamshouse/packs");

        Pack pack = Pack.builder()
                .name(request.getName())
                .description(request.getDescription())
                .priceDortoir(request.getPriceDortoir())
                .regularPriceDortoir(request.getRegularPriceDortoir())
                .priceSingle(request.getPriceSingle())
                .regularPriceSingle(request.getRegularPriceSingle())
                .priceDouble(request.getPriceDouble())
                .regularPriceDouble(request.getRegularPriceDouble())
                .includedFeatures(request.getIncludedFeatures() != null
                        ? request.getIncludedFeatures() : new ArrayList<>())
                .photos(uploadedPhotos)
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
        pack.setPriceDortoir(request.getPriceDortoir());
        pack.setRegularPriceDortoir(request.getRegularPriceDortoir());
        pack.setPriceSingle(request.getPriceSingle());
        pack.setRegularPriceSingle(request.getRegularPriceSingle());
        pack.setPriceDouble(request.getPriceDouble());
        pack.setRegularPriceDouble(request.getRegularPriceDouble());

        if (request.getIncludedFeatures() != null) {
            pack.getIncludedFeatures().clear();
            pack.getIncludedFeatures().addAll(request.getIncludedFeatures());
        }

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

        return packRepository.save(pack);
    }

    @Transactional
    public void deletePack(Long id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));
        pack.setActive(false);
        packRepository.save(pack);
    }

    @Transactional
    public void deletePackPermanently(Long id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));

        if (pack.getPhotos() != null && !pack.getPhotos().isEmpty()) {
            pack.getPhotos().forEach(photoUrl -> {
                try {
                    cloudinaryService.deleteImage(photoUrl);
                } catch (Exception e) {
                    log.warn("Erreur suppression photo: {}", photoUrl, e);
                }
            });
        }

        packRepository.deleteById(id);
        log.info("Pack {} supprimé définitivement", id);
    }

    @Transactional(readOnly = true)
    public Pack getPackById(Long id) {
        return packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));
    }

    @Transactional(readOnly = true)
    public List<Pack> getAllPacks() {
        return packRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<PackResponse> getAllPacksAsResponse() {
        return packRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PackResponse getPackByIdAsResponse(Long id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));
        return mapToResponse(pack);
    }

    private PackResponse mapToResponse(Pack pack) {
        return PackResponse.builder()
                .id(pack.getId())
                .name(pack.getName())
                .description(pack.getDescription())
                .priceDortoir(pack.getPriceDortoir())
                .priceSingle(pack.getPriceSingle())
                .priceDouble(pack.getPriceDouble())
                .regularPriceDortoir(pack.getRegularPriceDortoir())
                .regularPriceSingle(pack.getRegularPriceSingle())
                .regularPriceDouble(pack.getRegularPriceDouble())
                .discountDortoir(calculateDiscount(pack.getRegularPriceDortoir(), pack.getPriceDortoir()))
                .discountSingle(calculateDiscount(pack.getRegularPriceSingle(), pack.getPriceSingle()))
                .discountDouble(calculateDiscount(pack.getRegularPriceDouble(), pack.getPriceDouble()))
                .includedFeatures(pack.getIncludedFeatures() != null
                        ? pack.getIncludedFeatures() : new ArrayList<>())
                .photos(pack.getPhotos() != null ? pack.getPhotos() : new ArrayList<>())
                .isActive(pack.isActive())
                .build();
    }

    private BigDecimal calculateDiscount(BigDecimal regular, BigDecimal promo) {
        if (regular == null || promo == null || regular.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return regular.subtract(promo)
                .divide(regular, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private List<String> uploadPhotosToCloudinary(List<String> photos, String folder) {
        if (photos == null || photos.isEmpty()) return new ArrayList<>();

        List<CompletableFuture<String>> futures = photos.stream()
                .map(photo -> CompletableFuture.supplyAsync(() -> {
                    try {
                        if (photo.startsWith("data:image")) {
                            return cloudinaryService.uploadBase64Image(photo, folder);
                        } else if (photo.startsWith("http")) {
                            return photo;
                        }
                        return null;
                    } catch (Exception e) {
                        log.error("Failed to upload photo to Cloudinary", e);
                        return null;
                    }
                }))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}