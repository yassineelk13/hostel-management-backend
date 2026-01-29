package com.hostel.management.repository;

import com.hostel.management.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ========== RECHERCHES DE BASE ==========

    Optional<Booking> findByBookingReference(String bookingReference);

    Optional<Booking> findByAccessCode(String accessCode);

    List<Booking> findByGuestEmail(String guestEmail);

    List<Booking> findByStatus(Booking.BookingStatus status);

    // ========== CHECK-INS / CHECK-OUTS DU JOUR ==========

    @Query("SELECT b FROM Booking b WHERE b.checkInDate = :date AND b.status = 'CONFIRMED'")
    List<Booking> findCheckInsForDate(@Param("date") LocalDate date);

    @Query("SELECT b FROM Booking b WHERE b.checkOutDate = :date AND b.status = 'CHECKED_IN'")
    List<Booking> findCheckOutsForDate(@Param("date") LocalDate date);

    // ✅ NOUVEAU : Check-ins du jour (tous statuts actifs)
    @Query("SELECT b FROM Booking b WHERE b.checkInDate = :date AND b.status IN ('PENDING', 'CONFIRMED')")
    List<Booking> findTodayCheckIns(@Param("date") LocalDate date);

    // ✅ NOUVEAU : Check-outs du jour
    @Query("SELECT b FROM Booking b WHERE b.checkOutDate = :date AND b.status = 'CHECKED_IN'")
    List<Booking> findTodayCheckOuts(@Param("date") LocalDate date);

    // ========== VÉRIFICATION DISPONIBILITÉ (CRITIQUE) ==========

    /**
     * ✅ CRITIQUE : Trouve les réservations qui chevauchent les dates pour un lit spécifique
     * Utilisé pour éviter les doubles réservations
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b 
        JOIN b.beds bed 
        WHERE bed.id = :bedId 
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND NOT (b.checkOutDate <= :checkIn OR b.checkInDate >= :checkOut)
        """)
    List<Booking> findOverlappingBookingsForBed(
            @Param("bedId") Long bedId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /**
     * ✅ CRITIQUE : Trouve les réservations qui chevauchent les dates pour une chambre
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b 
        JOIN b.beds bed 
        JOIN bed.room r
        WHERE r.id = :roomId 
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND NOT (b.checkOutDate <= :checkIn OR b.checkInDate >= :checkOut)
        """)
    List<Booking> findOverlappingBookingsForRoom(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /**
     * ✅ NOUVEAU : Vérifie si plusieurs lits sont disponibles en même temps
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b 
        JOIN b.beds bed 
        WHERE bed.id IN :bedIds 
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND NOT (b.checkOutDate <= :checkIn OR b.checkInDate >= :checkOut)
        """)
    List<Booking> findOverlappingBookingsForBeds(
            @Param("bedIds") List<Long> bedIds,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    // ========== STATISTIQUES & RAPPORTS ==========

    /**
     * Compte les réservations actives dans une période donnée
     */
    @Query("""
        SELECT COUNT(DISTINCT b) FROM Booking b 
        WHERE b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND NOT (b.checkOutDate <= :checkIn OR b.checkInDate >= :checkOut)
        """)
    long countActiveBookingsInPeriod(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /**
     * ✅ NOUVEAU : Compte le nombre de lits occupés dans une période
     */
    @Query("""
        SELECT COUNT(DISTINCT bed.id) FROM Booking b 
        JOIN b.beds bed
        WHERE b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND NOT (b.checkOutDate <= :checkIn OR b.checkInDate >= :checkOut)
        """)
    long countOccupiedBedsInPeriod(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /**
     * ✅ NOUVEAU : Trouve toutes les réservations en cours (checked-in)
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = 'CHECKED_IN' 
        AND :today >= b.checkInDate 
        AND :today < b.checkOutDate
        ORDER BY b.checkOutDate ASC
        """)
    List<Booking> findCurrentStays(@Param("today") LocalDate today);

    /**
     * ✅ NOUVEAU : Trouve les réservations qui arrivent bientôt
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status IN ('PENDING', 'CONFIRMED')
        AND b.checkInDate BETWEEN :startDate AND :endDate
        ORDER BY b.checkInDate ASC
        """)
    List<Booking> findUpcomingArrivals(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * ✅ NOUVEAU : Trouve les réservations avec paiement incomplet
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.paymentStatus IN ('UNPAID', 'PARTIAL')
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        ORDER BY b.checkInDate ASC
        """)
    List<Booking> findBookingsWithUnpaidBalance();

    /**
     * ✅ NOUVEAU : Statistiques par mois
     */
    @Query("""
        SELECT COUNT(b), SUM(b.totalPrice) FROM Booking b 
        WHERE b.status NOT IN ('CANCELLED')
        AND YEAR(b.checkInDate) = :year 
        AND MONTH(b.checkInDate) = :month
        """)
    Object[] getMonthlyStats(
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * ✅ NOUVEAU : Réservations d'un client
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE LOWER(b.guestEmail) = LOWER(:email)
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findByGuestEmailIgnoreCase(@Param("email") String email);

    /**
     * ✅ NOUVEAU : Chercher par téléphone
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.guestPhone LIKE %:phone%
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findByGuestPhoneContaining(@Param("phone") String phone);

    /**
     * ✅ NOUVEAU : Chercher par nom
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE LOWER(b.guestName) LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findByGuestNameContainingIgnoreCase(@Param("name") String name);

    // ========== NETTOYAGE & MAINTENANCE ==========

    /**
     * ✅ NOUVEAU : Trouver les anciennes réservations à archiver
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = 'CHECKED_OUT'
        AND b.checkOutDate < :beforeDate
        """)
    List<Booking> findOldBookingsForArchive(@Param("beforeDate") LocalDate beforeDate);

    /**
     * ✅ NOUVEAU : Réservations pending expirées (plus de 24h sans confirmation)
     */
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = 'PENDING'
        AND b.createdAt < :expirationTime
        """)
    List<Booking> findExpiredPendingBookings(@Param("expirationTime") java.time.LocalDateTime expirationTime);

    List<Booking> findByCheckInDate(LocalDate checkInDate);

    List<Booking> findByCheckOutDate(LocalDate checkOutDate);


}
