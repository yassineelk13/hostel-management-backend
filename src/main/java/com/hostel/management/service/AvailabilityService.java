package com.hostel.management.service;

import com.hostel.management.dto.response.AvailabilityResponse;
import com.hostel.management.entity.Bed;
import com.hostel.management.entity.Booking;
import com.hostel.management.entity.Room;
import com.hostel.management.exception.ResourceNotFoundException;
import com.hostel.management.repository.BedRepository;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;

    @Transactional(readOnly = true)
    public boolean isBedAvailable(Long bedId, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> overlappingBookings = bookingRepository
                .findOverlappingBookingsForBed(bedId, checkIn, checkOut);

        boolean isAvailable = overlappingBookings.isEmpty();

        log.debug("Lit {} disponible: {} pour {} - {}",
                bedId, isAvailable, checkIn, checkOut);

        return isAvailable;
    }

    @Transactional(readOnly = true)
    public boolean areBedsAvailable(List<Long> bedIds, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> overlappingBookings = bookingRepository
                .findOverlappingBookingsForBeds(bedIds, checkIn, checkOut);

        boolean isAvailable = overlappingBookings.isEmpty();

        log.debug("{} lits disponibles: {} pour {} - {}",
                bedIds.size(), isAvailable, checkIn, checkOut);

        return isAvailable;
    }

    @Transactional(readOnly = true)
    public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        List<Booking> overlappingBookings = bookingRepository
                .findOverlappingBookingsForRoom(roomId, checkIn, checkOut);

        boolean isAvailable = overlappingBookings.isEmpty();

        log.debug("Chambre {} disponible: {} pour {} - {}",
                roomId, isAvailable, checkIn, checkOut);

        return isAvailable;
    }

    // ✅ AJOUTER CETTE MÉTHODE
    @Transactional(readOnly = true)
    public AvailabilityResponse checkRoomAvailability(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        // Récupérer la chambre
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chambre non trouvée"));

        // Vérifier les réservations qui chevauchent
        List<Booking> overlappingBookings = bookingRepository
                .findOverlappingBookingsForRoom(roomId, checkIn, checkOut);

        // Récupérer tous les lits de la chambre
        List<Bed> allBeds = bedRepository.findByRoomId(roomId);

        // Trouver les lits disponibles (ceux qui ne sont pas dans les réservations)
        List<Long> bookedBedIds = overlappingBookings.stream()
                .flatMap(booking -> booking.getBeds().stream())
                .map(Bed::getId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> availableBedIds = allBeds.stream()
                .filter(bed -> !bookedBedIds.contains(bed.getId()))
                .map(Bed::getId)
                .collect(Collectors.toList());

        boolean isAvailable = !availableBedIds.isEmpty();
        int availableBeds = availableBedIds.size();

        // Calculer la prochaine date disponible si pas de lits disponibles
        LocalDate nextAvailableDate = null;
        if (!isAvailable) {
            nextAvailableDate = findNextAvailableDate(roomId, checkOut);
        }

        log.info("Disponibilité chambre {}: {} lits disponibles sur {} pour {} - {}",
                room.getRoomNumber(), availableBeds, allBeds.size(), checkIn, checkOut);

        return AvailabilityResponse.builder()
                .roomId(roomId)
                .roomNumber(room.getRoomNumber())
                .isAvailable(isAvailable)
                .availableBeds(availableBeds)
                .availableBedIds(availableBedIds)
                .nextAvailableDate(nextAvailableDate)
                .build();
    }

    // ✅ AJOUTER CETTE MÉTHODE HELPER
    private LocalDate findNextAvailableDate(Long roomId, LocalDate fromDate) {
        // Chercher les 60 prochains jours
        LocalDate searchDate = fromDate;
        LocalDate maxDate = fromDate.plusDays(60);

        while (searchDate.isBefore(maxDate)) {
            List<Booking> bookings = bookingRepository
                    .findOverlappingBookingsForRoom(roomId, searchDate, searchDate.plusDays(1));

            if (bookings.isEmpty()) {
                return searchDate;
            }

            // Passer à la date de checkout de la réservation la plus proche
            searchDate = bookings.stream()
                    .map(Booking::getCheckOutDate)
                    .max(LocalDate::compareTo)
                    .orElse(searchDate.plusDays(1));
        }

        return null; // Pas de disponibilité trouvée dans les 60 prochains jours
    }
}
