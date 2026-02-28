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

        Pack pack = null;
        if (request.getPackId() != null) {
            pack = packRepository.findById(request.getPackId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pack non trouvé"));

            if (!pack.isActive()) {
                throw new ValidationException("Ce pack n'est plus disponible");
            }

            // ✅ Force le chargement des services du pack
            if (pack.getIncludedServices() != null) {
                pack.getIncludedServices().size();
            }
        }

        List<com.hostel.management.entity.Service> services = new ArrayList<>();

        if (pack != null) {
            // Pack → services inclus dans le pack directement
            services = pack.getIncludedServices();
        } else if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            // Réservation normale → services choisis par le client
            services = serviceRepository.findAllById(request.getServiceIds());
            if (services.size() != request.getServiceIds().size()) {
                throw new ResourceNotFoundException("Un ou plusieurs services non trouvés");
            }
        }

        BigDecimal totalPrice = calculateTotalPrice(beds, services, pack, numberOfNights);

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
                .status(Booking.BookingStatus.CONFIRMED)
                .paymentStatus(Booking.PaymentStatus.UNPAID)
                .accessCode(generateAccessCode())
                .bookingReference(generateBookingReference())
                .notes(request.getNotes())
                .build();

        bookingRepository.save(booking);

        log.info("Réservation créée: {} pour {} du {} au {}",
                booking.getBookingReference(),
                booking.getGuestEmail(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        try {
            emailService.sendBookingConfirmation(booking);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de confirmation: {}",
                    booking.getBookingReference(), e);
        }

        return mapToResponse(booking);
    }

    private List<Bed> lockAvailableBeds(List<Long> bedIds, LocalDate checkIn, LocalDate checkOut) {
        if (bedIds == null || bedIds.isEmpty()) {
            throw new ValidationException("Au moins un lit doit être sélectionné");
        }

        List<Bed> beds = bedRepository.findAllById(bedIds);

        if (beds.size() != bedIds.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs lits non trouvés");
        }

        // ✅ Force le chargement de la room pour chaque bed
        beds.forEach(bed -> {
            if (bed.getRoom() != null) {
                bed.getRoom().getRoomNumber(); // Force le chargement
            }
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

    private BigDecimal calculateTotalPrice(
            List<Bed> beds,
            List<Service> services,
            Pack pack,
            long numberOfNights
    ) {
        BigDecimal total = BigDecimal.ZERO;

        if (pack != null) {
            // ✅ Pack = prix fixe tout compris, on s'arrête là
            return pack.getPromoPrice();
        }

        // Calcul normal sans pack
        for (Bed bed : beds) {
            BigDecimal bedPrice = bed.getRoom().getPricePerNight()
                    .multiply(BigDecimal.valueOf(numberOfNights));
            total = total.add(bedPrice);
        }

        for (Service service : services) {
            total = total.add(service.calculateTotalPrice((int) numberOfNights));
        }

        return total;
    }


    private String generateAccessCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    private String generateBookingReference() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String randomPart = generateRandomString(5);
        return "BK-" + datePart + "-" + randomPart;
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA_NUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHA_NUMERIC.length())));
        }
        return sb.toString();
    }

    // ✅ AJOUTER @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Réservation non trouvée avec la référence: " + reference
                ));

        // ✅ Force le chargement des collections
        forceLoadCollections(booking);

        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        log.info("📋 GET /api/bookings - Récupération de toutes les réservations");

        List<Booking> bookings = bookingRepository.findAll();
        log.info("✅ {} réservations trouvées dans la DB", bookings.size());

        return bookings.stream()
                .map(booking -> {
                    try {
                        log.debug("Traitement réservation: {}", booking.getBookingReference());

                        // ✅ Force le chargement des lits
                        if (booking.getBeds() != null) {
                            booking.getBeds().size();
                            log.debug("  - {} lits chargés", booking.getBeds().size());

                            // ✅ Force le chargement de la room pour chaque lit
                            booking.getBeds().forEach(bed -> {
                                if (bed.getRoom() != null) {
                                    bed.getRoom().getRoomNumber();

                                    // ✅ Force le chargement des photos de la room
                                    if (bed.getRoom().getPhotos() != null) {
                                        bed.getRoom().getPhotos().size();
                                    }
                                }
                            });
                        }

                        // ✅ Force le chargement des services
                        if (booking.getServices() != null) {
                            booking.getServices().size();
                            log.debug("  - {} services chargés", booking.getServices().size());
                        }

                        // ✅ Force le chargement du pack
                        if (booking.getPack() != null) {
                            booking.getPack().getName();
                            log.debug("  - Pack: {}", booking.getPack().getName());

                            // ✅ Force le chargement des services du pack
                            if (booking.getPack().getIncludedServices() != null) {
                                booking.getPack().getIncludedServices().size();
                            }

                            // ✅ Force le chargement des photos du pack
                            if (booking.getPack().getPhotos() != null) {
                                booking.getPack().getPhotos().size();
                            }
                        }

                        BookingResponse response = mapToResponse(booking);
                        log.debug("✅ Réservation {} mappée avec succès", booking.getBookingReference());
                        return response;

                    } catch (Exception e) {
                        log.error("❌ Erreur lors du mapping de la réservation {}: {}",
                                booking.getBookingReference(), e.getMessage(), e);
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
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepository.findByCheckInDate(today);

        bookings.forEach(this::forceLoadCollections);

        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getTodayCheckOuts() {
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepository.findByCheckOutDate(today);

        bookings.forEach(this::forceLoadCollections);

        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        validateStatusTransition(booking.getStatus(), status);

        booking.setStatus(status);
        bookingRepository.save(booking);

        log.info("Statut de la réservation {} changé à: {}",
                booking.getBookingReference(), status);

        forceLoadCollections(booking);

        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse updatePaymentStatus(Long id, Booking.PaymentStatus paymentStatus) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        booking.setPaymentStatus(paymentStatus);
        bookingRepository.save(booking);

        log.info("Statut de paiement de la réservation {} changé à: {}",
                booking.getBookingReference(), paymentStatus);

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

    private void validateStatusTransition(
            Booking.BookingStatus currentStatus,
            Booking.BookingStatus newStatus) {

        if (currentStatus == Booking.BookingStatus.CANCELLED) {
            throw new ValidationException("Impossible de modifier une réservation annulée");
        }

        if (currentStatus == Booking.BookingStatus.CHECKED_OUT &&
                newStatus != Booking.BookingStatus.CHECKED_OUT) {
            throw new ValidationException("Impossible de modifier une réservation terminée");
        }

        if (newStatus == Booking.BookingStatus.CHECKED_IN &&
                currentStatus != Booking.BookingStatus.CONFIRMED) {
            throw new ValidationException(
                    "Un check-in n'est possible que sur une réservation confirmée"
            );
        }
    }

    // ✅ NOUVELLE MÉTHODE : Force le chargement des collections LAZY
    private void forceLoadCollections(Booking booking) {
        if (booking.getBeds() != null) {
            booking.getBeds().size();
            // Force aussi le chargement de la room pour chaque bed
            booking.getBeds().forEach(bed -> {
                if (bed.getRoom() != null) {
                    bed.getRoom().getRoomNumber();
                }
            });
        }

        if (booking.getServices() != null) {
            booking.getServices().size();
        }

        if (booking.getPack() != null) {
            booking.getPack().getName(); // Force le chargement du pack
            if (booking.getPack().getIncludedServices() != null) {
                booking.getPack().getIncludedServices().size();
            }
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
                            .build())
                    .collect(Collectors.toList());
        }

        BookingResponse.PackInfo packInfo = null;
        if (booking.getPack() != null) {
            packInfo = BookingResponse.PackInfo.builder()
                    .packId(booking.getPack().getId())
                    .name(booking.getPack().getName())
                    .durationDays(booking.getPack().getDurationDays())
                    .promoPrice(booking.getPack().getPromoPrice())
                    .build();
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
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .beds(bedInfos)
                .services(serviceInfos)
                .pack(packInfo)
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByAccessCode(String accessCode) {
        Booking booking = bookingRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Réservation non trouvée avec le code: " + accessCode
                ));

        forceLoadCollections(booking);

        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getCheckInsForDate(LocalDate date) {
        List<Booking> bookings = bookingRepository.findByCheckInDate(date);

        bookings.forEach(this::forceLoadCollections);

        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getCheckOutsForDate(LocalDate date) {
        List<Booking> bookings = bookingRepository.findByCheckOutDate(date);

        bookings.forEach(this::forceLoadCollections);

        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        log.info("Suppression de la réservation {} - {}", booking.getBookingReference(), booking.getGuestName());

        // Supprimer définitivement de la base de données
        bookingRepository.delete(booking);
    }

}
