package com.hostel.management.service;

import com.hostel.management.dto.request.BookingRequest;
import com.hostel.management.dto.response.BookingResponse;
import com.hostel.management.entity.*;
import com.hostel.management.exception.BookingException;
import com.hostel.management.exception.ResourceNotFoundException;
import com.hostel.management.exception.ValidationException;
import com.hostel.management.repository.BedRepository;
import com.hostel.management.repository.BookingRepository;
import com.hostel.management.repository.PackRepository;
import com.hostel.management.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hostel.management.entity.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.hostel.management.entity.Room;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BedRepository bedRepository;
    private final ServiceRepository serviceRepository;
    private final PackRepository packRepository;
    private final EmailService emailService;
    private final AvailabilityService availabilityService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // ✅ Breakfast supplement for 2nd person in a SINGLE room pack
    private static final BigDecimal BREAKFAST_EXTRA_PER_PERSON_PER_NIGHT = new BigDecimal("5.00");

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponse createBooking(BookingRequest request) {
        validateBookingDates(request.getCheckInDate(), request.getCheckOutDate());

        long numberOfNights = ChronoUnit.DAYS.between(
                request.getCheckInDate(),
                request.getCheckOutDate()
        );
        if (numberOfNights <= 0) {
            throw new ValidationException("Le séjour doit être d'au moins 1 nuit");
        }

        List<Bed> beds = lockAvailableBeds(
                request.getBedIds(),
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        // For SINGLE and DOUBLE → force ALL beds of the room to be reserved
        if (!beds.isEmpty()) {
            Room room = beds.get(0).getRoom();
            if (room.getRoomType() == Room.RoomType.SINGLE ||
                    room.getRoomType() == Room.RoomType.DOUBLE) {

                List<Bed> allBeds = bedRepository.findByRoomId(room.getId());

                for (Bed bed : allBeds) {
                    if (!availabilityService.isBedAvailable(
                            bed.getId(),
                            request.getCheckInDate(),
                            request.getCheckOutDate())) {
                        throw new BookingException(
                                "La chambre " + room.getRoomNumber() +
                                        " n'est pas disponible pour ces dates"
                        );
                    }
                }
                beds = allBeds;
                log.info("Chambre {} ({}) → {} lits sélectionnés automatiquement",
                        room.getRoomNumber(), room.getRoomType(), allBeds.size());
            }
        }

        // ✅ Determine effective numberOfPersons based on room type
        int numberOfPersons = resolveNumberOfPersons(beds, request.getNumberOfPersons());

        Pack pack = null;
        if (request.getPackId() != null) {
            pack = packRepository.findById(request.getPackId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));
            if (!pack.isActive()) {
                throw new ValidationException("Ce pack n'est plus disponible");
            }
        }

        List<Service> services = new ArrayList<>();
        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            services = serviceRepository.findAllById(request.getServiceIds());
            if (services.size() != request.getServiceIds().size()) {
                throw new ResourceNotFoundException("Un ou plusieurs services non trouvés");
            }
        }

        BigDecimal totalPrice = calculateTotalPrice(beds, services, pack, numberOfNights, numberOfPersons);

        Booking booking = Booking.builder()
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail().toLowerCase().trim())
                .guestPhone(request.getGuestPhone())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .beds(beds)
                .services(services)
                .pack(pack)
                .totalPrice(totalPrice)
                .numberOfPersons(numberOfPersons)   // ✅ NEW
                .status(Booking.BookingStatus.CONFIRMED)
                .paymentStatus(Booking.PaymentStatus.UNPAID)
                .accessCode(generateAccessCode())
                .bookingReference(generateBookingReference())
                .notes(request.getNotes())
                .build();

        bookingRepository.save(booking);

        log.info("Réservation créée: {} | {} | {} nuits | {} personnes | total={}",
                booking.getBookingReference(),
                booking.getGuestEmail(),
                numberOfNights,
                numberOfPersons,
                totalPrice
        );

        try {
            emailService.sendBookingConfirmation(booking);
        } catch (Exception e) {
            log.error("Erreur envoi email confirmation {}: {}", booking.getBookingReference(), e.getMessage());
        }

        return mapToResponse(booking);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ NEW HELPER: resolve number of persons depending on room type
    //   - DORTOIR → number of beds selected
    //   - SINGLE  → value from request (1 or 2)
    //   - DOUBLE  → always 1 (fixed room price)
    // ─────────────────────────────────────────────────────────────────────────
    private int resolveNumberOfPersons(List<Bed> beds, int requestedPersons) {
        if (beds.isEmpty()) return 1;
        Room.RoomType roomType = beds.get(0).getRoom().getRoomType();
        return switch (roomType) {
            case DORTOIR -> beds.size();
            case SINGLE  -> Math.max(1, Math.min(2, requestedPersons)); // clamp 1–2
            default      -> 1; // DOUBLE: always 1
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ UPDATED: calculateTotalPrice now takes numberOfPersons
    // ─────────────────────────────────────────────────────────────────────────
    private BigDecimal calculateTotalPrice(
            List<Bed> beds,
            List<Service> services,
            Pack pack,
            long numberOfNights,
            int numberOfPersons
    ) {
        if (beds.isEmpty()) return BigDecimal.ZERO;

        Room room        = beds.get(0).getRoom();
        Room.RoomType rt = room.getRoomType();
        int  bedCount    = beds.size();

        // ── PACK path ──────────────────────────────────────────────────────
        if (pack != null) {
            BigDecimal pricePerNight = pack.getPromoPrice(rt, (int) numberOfNights);

            if (pricePerNight.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("Aucun prix trouvé pour {} nuits, roomType={}, pack={}",
                        numberOfNights, rt, pack.getId());
            }

            BigDecimal total;
            if (rt == Room.RoomType.DORTOIR) {
                // DORTOIR: price × nights × beds
                total = pricePerNight
                        .multiply(BigDecimal.valueOf(numberOfNights))
                        .multiply(BigDecimal.valueOf(bedCount));
            } else if (rt == Room.RoomType.SINGLE && numberOfPersons > 1) {
                BigDecimal base = pricePerNight.multiply(BigDecimal.valueOf(numberOfNights));
                BigDecimal breakfastExtra = BREAKFAST_EXTRA_PER_PERSON_PER_NIGHT
                        .multiply(BigDecimal.valueOf(numberOfNights));
                BigDecimal activitiesExtra = (pack.getExtraPersonPricePerNight() != null
                        ? pack.getExtraPersonPricePerNight()
                        : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(numberOfNights));
                total = base.add(breakfastExtra).add(activitiesExtra);
                log.info("Pack SINGLE 2 personnes: base={}, breakfast={}, activities={}, total={}",
                        base, breakfastExtra, activitiesExtra, total);
            }else {
                // SINGLE (1 person) or DOUBLE: fixed price × nights
                total = pricePerNight.multiply(BigDecimal.valueOf(numberOfNights));
            }
            return total;
        }

        // ── Normal path (no pack) ─────────────────────────────────────────
        BigDecimal total;

        // ✅ APRÈS
        if (rt == Room.RoomType.SINGLE || rt == Room.RoomType.DOUBLE) {
            BigDecimal base = room.getPricePerNight().multiply(BigDecimal.valueOf(numberOfNights));
            total = base;
            if (rt == Room.RoomType.SINGLE && numberOfPersons > 1) {
                // Sans pack → pas d'activitiesExtra, seulement le breakfast
                BigDecimal breakfastExtra = BREAKFAST_EXTRA_PER_PERSON_PER_NIGHT
                        .multiply(BigDecimal.valueOf(numberOfNights));
                total = base.add(breakfastExtra);
                log.info("SINGLE sans pack, 2 personnes: base={}, breakfast={}, total={}",
                        base, breakfastExtra, total);
            }
        }else {
            // DORTOIR: price × nights × beds
            total = room.getPricePerNight()
                    .multiply(BigDecimal.valueOf(numberOfNights))
                    .multiply(BigDecimal.valueOf(bedCount));
        }

        // ✅ Services: use numberOfPersons for SINGLE, bedCount for DORTOIR, 1 for DOUBLE
        int personsForServices = (rt == Room.RoomType.DORTOIR) ? bedCount : numberOfPersons;

        for (Service service : services) {
            // Service.calculateTotalPrice(nights, persons) already handles PER_ROOM vs PER_PERSON
            BigDecimal serviceTotal = service.calculateTotalPrice((int) numberOfNights, personsForServices);
            total = total.add(serviceTotal);
            log.debug("Service '{}' [{}]: +{} (×{} personnes)",
                    service.getName(), service.getPricingType(), serviceTotal, personsForServices);
        }

        return total;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rest of the methods (unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    private List<Bed> lockAvailableBeds(List<Long> bedIds, LocalDate checkIn, LocalDate checkOut) {
        if (bedIds == null || bedIds.isEmpty()) {
            throw new ValidationException("Au moins un lit doit être sélectionné");
        }
        List<Bed> beds = bedRepository.findAllById(bedIds);
        if (beds.size() != bedIds.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs lits non trouvés");
        }
        beds.forEach(bed -> {
            if (bed.getRoom() != null) bed.getRoom().getRoomNumber();
        });
        for (Bed bed : beds) {
            if (!availabilityService.isBedAvailable(bed.getId(), checkIn, checkOut)) {
                throw new BookingException(
                        String.format("Le lit %s de la chambre %s n'est pas disponible pour ces dates",
                                bed.getBedNumber(), bed.getRoom().getRoomNumber())
                );
            }
        }
        return beds;
    }

    private void validateBookingDates(LocalDate checkIn, LocalDate checkOut) {
        LocalDate today = LocalDate.now();
        if (checkIn.isBefore(today)) {
            throw new ValidationException("La date d'arrivée ne peut pas être dans le passé");
        }
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ValidationException("La date de départ doit être après la date d'arrivée");
        }
        if (checkIn.isAfter(today.plusYears(1))) {
            throw new ValidationException("Impossible de réserver plus d'un an à l'avance");
        }
    }

    private String generateAccessCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    private String generateBookingReference() {
        String datePart = LocalDate.now().toString().replace("-", "");
        return "BK-" + datePart + "-" + generateRandomString(5);
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA_NUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHA_NUMERIC.length())));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Réservation non trouvée avec la référence: " + reference));
        forceLoadCollections(booking);
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        log.info("GET /api/bookings - Récupération de toutes les réservations");
        return bookingRepository.findAll().stream()
                .map(booking -> {
                    try {
                        forceLoadCollections(booking);
                        return mapToResponse(booking);
                    } catch (Exception e) {
                        log.error("Erreur mapping réservation {}: {}", booking.getBookingReference(), e.getMessage(), e);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));
        forceLoadCollections(booking);
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getTodayCheckIns() {
        List<Booking> bookings = bookingRepository.findByCheckInDate(LocalDate.now());
        bookings.forEach(this::forceLoadCollections);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getTodayCheckOuts() {
        List<Booking> bookings = bookingRepository.findByCheckOutDate(LocalDate.now());
        bookings.forEach(this::forceLoadCollections);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));
        validateStatusTransition(booking.getStatus(), status);
        booking.setStatus(status);
        bookingRepository.save(booking);
        log.info("Statut réservation {} → {}", booking.getBookingReference(), status);
        forceLoadCollections(booking);
        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse updatePaymentStatus(Long id, Booking.PaymentStatus paymentStatus) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));
        booking.setPaymentStatus(paymentStatus);
        bookingRepository.save(booking);
        log.info("Paiement réservation {} → {}", booking.getBookingReference(), paymentStatus);
        forceLoadCollections(booking);
        return mapToResponse(booking);
    }

    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));
        if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
            throw new ValidationException("Impossible d'annuler une réservation en cours");
        }
        if (booking.getStatus() == Booking.BookingStatus.CHECKED_OUT) {
            throw new ValidationException("Impossible d'annuler une réservation terminée");
        }
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Réservation {} annulée", booking.getBookingReference());
    }

    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));
        log.info("Suppression réservation {} - {}", booking.getBookingReference(), booking.getGuestName());
        bookingRepository.delete(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByAccessCode(String accessCode) {
        Booking booking = bookingRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Réservation non trouvée avec le code: " + accessCode));
        forceLoadCollections(booking);
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getCheckInsForDate(LocalDate date) {
        List<Booking> bookings = bookingRepository.findByCheckInDate(date);
        bookings.forEach(this::forceLoadCollections);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getCheckOutsForDate(LocalDate date) {
        List<Booking> bookings = bookingRepository.findByCheckOutDate(date);
        bookings.forEach(this::forceLoadCollections);
        return bookings.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private void validateStatusTransition(Booking.BookingStatus current, Booking.BookingStatus next) {
        if (current == Booking.BookingStatus.CANCELLED) {
            throw new ValidationException("Impossible de modifier une réservation annulée");
        }
        if (current == Booking.BookingStatus.CHECKED_OUT && next != Booking.BookingStatus.CHECKED_OUT) {
            throw new ValidationException("Impossible de modifier une réservation terminée");
        }
        if (next == Booking.BookingStatus.CHECKED_IN && current != Booking.BookingStatus.CONFIRMED) {
            throw new ValidationException("Un check-in n'est possible que sur une réservation confirmée");
        }
    }

    private void forceLoadCollections(Booking booking) {
        if (booking.getBeds() != null) {
            booking.getBeds().size();
            booking.getBeds().forEach(bed -> {
                if (bed.getRoom() != null) bed.getRoom().getRoomNumber();
            });
        }
        if (booking.getServices() != null) booking.getServices().size();
        if (booking.getPack() != null) {
            booking.getPack().getName();
            if (booking.getPack().getPhotos() != null) booking.getPack().getPhotos().size();
            if (booking.getPack().getIncludedFeatures() != null) booking.getPack().getIncludedFeatures().size();
        }
    }

    private BookingResponse mapToResponse(Booking booking) {
        List<BookingResponse.BedInfo> bedInfos = new ArrayList<>();
        if (booking.getBeds() != null) {
            bedInfos = booking.getBeds().stream()
                    .map(bed -> BookingResponse.BedInfo.builder()
                            .bedId(bed.getId())
                            .bedNumber(bed.getBedNumber())
                            .roomNumber(bed.getRoom() != null ? bed.getRoom().getRoomNumber() : "N/A")
                            .build())
                    .collect(Collectors.toList());
        }

        List<BookingResponse.ServiceInfo> serviceInfos = new ArrayList<>();
        if (booking.getServices() != null) {
            serviceInfos = booking.getServices().stream()
                    .map(service -> BookingResponse.ServiceInfo.builder()
                            .serviceId(service.getId())
                            .name(service.getName())
                            .price(service.getPrice())
                            .pricingType(service.getPricingType() != null   // ✅ NEW
                                    ? service.getPricingType().name() : null)
                            .build())
                    .collect(Collectors.toList());
        }

        BookingResponse.PackInfo packInfo = null;
        if (booking.getPack() != null) {
            Pack p = booking.getPack();
            BigDecimal promoPrice = BigDecimal.ZERO;
            Integer durationDays = null;

            if (booking.getBeds() != null && !booking.getBeds().isEmpty()) {
                Room.RoomType roomType = booking.getBeds().get(0).getRoom().getRoomType();
                long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
                promoPrice = p.getPromoPrice(roomType, (int) nights);
                durationDays = (int) nights;
            }

            packInfo = BookingResponse.PackInfo.builder()
                    .packId(p.getId())
                    .name(p.getName())
                    .promoPrice(promoPrice)
                    .durationDays(durationDays)
                    .build();
        }

        String roomType = null;
        if (booking.getBeds() != null && !booking.getBeds().isEmpty()
                && booking.getBeds().get(0).getRoom() != null) {
            roomType = booking.getBeds().get(0).getRoom().getRoomType().name();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .accessCode(booking.getAccessCode())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .guestPhone(booking.getGuestPhone())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .totalPrice(booking.getTotalPrice())
                .numberOfPersons(booking.getNumberOfPersons())   // ✅ NEW
                .roomType(roomType)
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .beds(bedInfos)
                .services(serviceInfos)
                .pack(packInfo)
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}