package com.hostel.management.service;

import com.hostel.management.dto.request.RoomRequest;
import com.hostel.management.dto.response.RoomResponse;
import com.hostel.management.dto.response.BedResponse;
import com.hostel.management.entity.Bed;
import com.hostel.management.entity.Room;
import com.hostel.management.exception.ResourceNotFoundException;
import com.hostel.management.repository.BedRepository;
import com.hostel.management.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new IllegalArgumentException("Numéro de chambre déjà existant");
        }

        List<String> uploadedPhotos = uploadPhotosToCloudinary(request.getPhotos(), "shamshouse/rooms");

        Room room = Room.builder()
                .roomNumber(request.getRoomNumber())
                .roomType(request.getRoomType())
                .description(request.getDescription())
                .pricePerNight(request.getPricePerNight())
                .photos(uploadedPhotos)
                .isActive(true)
                .build();

        room = roomRepository.save(room);

        int numberOfBeds = getNumberOfBedsByType(request.getRoomType());
        if (request.getNumberOfBeds() != null) {
            numberOfBeds = request.getNumberOfBeds();
        }

        List<Bed> beds = new ArrayList<>();
        for (int i = 1; i <= numberOfBeds; i++) {
            Bed bed = Bed.builder()
                    .room(room)
                    .bedNumber(String.valueOf(i))
                    .isAvailable(true)
                    .build();
            beds.add(bedRepository.save(bed));
        }

        room.setBeds(beds);

        return mapToResponse(room, beds);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre non trouvée"));

        if (!room.getRoomNumber().equals(request.getRoomNumber())
                && roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new IllegalArgumentException("Numéro de chambre déjà existant");
        }

        room.setRoomNumber(request.getRoomNumber());
        room.setRoomType(request.getRoomType());
        room.setDescription(request.getDescription());
        room.setPricePerNight(request.getPricePerNight());

        if (request.getPhotos() != null) {
            if (room.getPhotos() != null) {
                room.getPhotos().forEach(photoUrl -> {
                    try {
                        cloudinaryService.deleteImage(photoUrl);
                    } catch (Exception e) {
                        log.warn("Failed to delete old photo: {}", photoUrl);
                    }
                });
            }

            List<String> uploadedPhotos = uploadPhotosToCloudinary(request.getPhotos(), "shamshouse/rooms");
            room.setPhotos(uploadedPhotos);
        }

        room = roomRepository.save(room);

        // ✅ Force le chargement des collections
        if (room.getBeds() != null) {
            room.getBeds().size();
        }
        if (room.getPhotos() != null) {
            room.getPhotos().size();
        }

        return mapToResponse(room, room.getBeds());
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre non trouvée"));

        if (room.getPhotos() != null) {
            room.getPhotos().forEach(photoUrl -> {
                try {
                    cloudinaryService.deleteImage(photoUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete photo during room deletion: {}", photoUrl);
                }
            });
        }

        room.setActive(false);
        roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        log.debug("getRoomById - ID: {}", id);

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre non trouvée"));

        // ✅✅✅ LIGNE CRITIQUE AJOUTÉE
        if (room.getPhotos() != null) {
            log.debug("Chargement de {} photos pour la chambre {}", room.getPhotos().size(), room.getRoomNumber());
        }

        // ✅ Force le chargement des lits
        if (room.getBeds() != null) {
            log.debug("Chargement de {} lits pour la chambre {}", room.getBeds().size(), room.getRoomNumber());
        }

        return mapToResponse(room, room.getBeds());
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        log.info("getAllRooms - Début de la récupération");

        try {
            List<Room> rooms = roomRepository.findByIsActiveTrue();
            log.info("Chambres trouvées dans la DB: {}", rooms.size());

            if (rooms.isEmpty()) {
                return new ArrayList<>();
            }

            List<RoomResponse> responses = new ArrayList<>();
            for (Room room : rooms) {
                try {
                    log.debug("Traitement de la chambre: {}", room.getRoomNumber());

                    // ✅ FORCE LE CHARGEMENT DES PHOTOS
                    if (room.getPhotos() != null) {
                        int photosCount = room.getPhotos().size();
                        log.debug("Chambre {} a {} photos", room.getRoomNumber(), photosCount);
                    }

                    // ✅ FORCE LE CHARGEMENT DES LITS
                    if (room.getBeds() != null) {
                        int bedsCount = room.getBeds().size();
                        log.debug("Chambre {} a {} lits", room.getRoomNumber(), bedsCount);
                    }

                    RoomResponse response = mapToResponse(room, room.getBeds());
                    responses.add(response);
                    log.debug("Chambre {} mappée avec succès", room.getRoomNumber());

                } catch (Exception e) {
                    log.error("Erreur lors du mapping de la chambre {}: {}",
                            room.getRoomNumber(), e.getMessage(), e);
                }
            }

            log.info("getAllRooms - {} chambres mappées avec succès sur {}", responses.size(), rooms.size());
            return responses;

        } catch (Exception e) {
            log.error("Erreur critique dans getAllRooms: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        log.info("getAvailableRooms - De {} à {}", checkIn, checkOut);

        List<Room> rooms = roomRepository.findAvailableRooms(checkIn, checkOut);
        log.info("Chambres disponibles trouvées: {}", rooms.size());

        return rooms.stream()
                .map(room -> {
                    try {
                        // ✅ Force le chargement des photos
                        if (room.getPhotos() != null) {
                            room.getPhotos().size();
                        }

                        // ✅ Force le chargement des lits
                        if (room.getBeds() != null) {
                            room.getBeds().size();
                        }

                        List<Bed> availableBeds = bedRepository.findAvailableBedsByRoomAndDates(
                                room.getId(), checkIn, checkOut
                        );

                        return mapToResponse(room, availableBeds);
                    } catch (Exception e) {
                        log.error("Erreur lors du traitement de la chambre {}: {}",
                                room.getRoomNumber(), e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(response -> response.getAvailableBeds() > 0)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomResponse createRoomWithFiles(RoomRequest request, List<MultipartFile> photoFiles) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new IllegalArgumentException("Numéro de chambre déjà existant");
        }

        List<String> uploadedPhotos = new ArrayList<>();
        if (photoFiles != null && !photoFiles.isEmpty()) {
            List<CompletableFuture<String>> uploadFutures = photoFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(() -> {
                        try {
                            String url = cloudinaryService.uploadImage(file, "shamshouse/rooms");
                            log.info("Photo uploaded successfully: {}", url);
                            return url;
                        } catch (Exception e) {
                            log.error("Failed to upload photo", e);
                            return null;
                        }
                    }))
                    .collect(Collectors.toList());

            uploadedPhotos = uploadFutures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        Room room = Room.builder()
                .roomNumber(request.getRoomNumber())
                .roomType(request.getRoomType())
                .description(request.getDescription())
                .pricePerNight(request.getPricePerNight())
                .photos(uploadedPhotos)
                .isActive(true)
                .build();

        room = roomRepository.save(room);

        int numberOfBeds = getNumberOfBedsByType(request.getRoomType());
        if (request.getNumberOfBeds() != null) {
            numberOfBeds = request.getNumberOfBeds();
        }

        List<Bed> beds = new ArrayList<>();
        for (int i = 1; i <= numberOfBeds; i++) {
            Bed bed = Bed.builder()
                    .room(room)
                    .bedNumber(String.valueOf(i))
                    .isAvailable(true)
                    .build();
            beds.add(bedRepository.save(bed));
        }

        room.setBeds(beds);

        return mapToResponse(room, beds);
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

    private int getNumberOfBedsByType(Room.RoomType roomType) {
        return switch (roomType) {
            case DOUBLE -> 1;
            case SINGLE -> 2;
            case DORTOIR -> 8;
        };
    }

    private RoomResponse mapToResponse(Room room, List<Bed> beds) {
        try {
            log.debug("mapToResponse - Chambre: {}, Lits: {}",
                    room.getRoomNumber(),
                    beds != null ? beds.size() : 0);

            List<BedResponse> bedResponses = new ArrayList<>();

            if (beds != null) {
                bedResponses = beds.stream()
                        .map(bed -> {
                            try {
                                return BedResponse.builder()
                                        .id(bed.getId())
                                        .bedNumber(bed.getBedNumber())
                                        .isAvailable(bed.isAvailable())
                                        .build();
                            } catch (Exception e) {
                                log.error("Erreur lors du mapping du lit {}: {}",
                                        bed.getId(), e.getMessage());
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            RoomResponse response = RoomResponse.builder()
                    .id(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .roomType(room.getRoomType())
                    .description(room.getDescription())
                    .pricePerNight(room.getPricePerNight())
                    .photos(room.getPhotos() != null ? room.getPhotos() : new ArrayList<>())
                    .totalBeds(room.getBeds() != null ? room.getBeds().size() : 0)
                    .availableBeds(bedResponses.size())
                    .isActive(room.isActive())
                    .beds(bedResponses)
                    .build();

            log.debug("mapToResponse - Success pour chambre {}", room.getRoomNumber());
            return response;

        } catch (Exception e) {
            log.error("Erreur critique dans mapToResponse pour chambre {}: {}",
                    room.getRoomNumber(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors du mapping de la chambre " + room.getRoomNumber(), e);
        }
    }
}
