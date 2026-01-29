package com.hostel.management.repository;

import com.hostel.management.entity.Bed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {

    List<Bed> findByRoomId(Long roomId);

    // ✅ NOUVEAU : Trouver un lit spécifique dans une chambre
    Optional<Bed> findByRoomIdAndBedNumber(Long roomId, String bedNumber);

    // ✅ Requête de disponibilité (déjà bonne)
    @Query("SELECT b FROM Bed b " +
            "WHERE b.room.id = :roomId " +
            "AND b.id NOT IN (" +
            "  SELECT bed.id FROM Booking bk " +
            "  JOIN bk.beds bed " +
            "  WHERE bk.status != 'CANCELLED' " +
            "  AND bk.checkInDate < :checkOut " +
            "  AND bk.checkOutDate > :checkIn" +
            ")")
    List<Bed> findAvailableBedsByRoomAndDates(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    // ✅ NOUVEAU : Tous les lits disponibles pour des dates (toutes chambres)
    @Query("SELECT b FROM Bed b " +
            "WHERE b.room.isActive = true " +
            "AND b.id NOT IN (" +
            "  SELECT bed.id FROM Booking bk " +
            "  JOIN bk.beds bed " +
            "  WHERE bk.status != 'CANCELLED' " +
            "  AND bk.checkInDate < :checkOut " +
            "  AND bk.checkOutDate > :checkIn" +
            ")")
    List<Bed> findAllAvailableBeds(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    // ✅ NOUVEAU : Compter les lits disponibles
    @Query("SELECT COUNT(b) FROM Bed b " +
            "WHERE b.room.isActive = true " +
            "AND b.id NOT IN (" +
            "  SELECT bed.id FROM Booking bk " +
            "  JOIN bk.beds bed " +
            "  WHERE bk.status != 'CANCELLED' " +
            "  AND bk.checkInDate < :checkOut " +
            "  AND bk.checkOutDate > :checkIn" +
            ")")
    long countAvailableBeds(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );
}
