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

    // ✅ MODIFIÉ : Lits non supprimés d'une chambre
    @Query("SELECT b FROM Bed b WHERE b.room.id = :roomId AND b.deleted = false")
    List<Bed> findByRoomIdAndNotDeleted(@Param("roomId") Long roomId);

    // ✅ MODIFIÉ : Trouver un lit spécifique non supprimé
    @Query("SELECT b FROM Bed b WHERE b.room.id = :roomId AND b.bedNumber = :bedNumber AND b.deleted = false")
    Optional<Bed> findByRoomIdAndBedNumberAndNotDeleted(
            @Param("roomId") Long roomId,
            @Param("bedNumber") String bedNumber
    );

    // ✅ MODIFIÉ : Lits disponibles (non supprimés)
    @Query("SELECT b FROM Bed b " +
            "WHERE b.room.id = :roomId " +
            "AND b.deleted = false " +
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

    // ✅ MODIFIÉ : Tous les lits disponibles (non supprimés)
    @Query("SELECT b FROM Bed b " +
            "WHERE b.room.isActive = true " +
            "AND b.room.deleted = false " +
            "AND b.deleted = false " +
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

    // ✅ MODIFIÉ : Compter les lits disponibles (non supprimés)
    @Query("SELECT COUNT(b) FROM Bed b " +
            "WHERE b.room.isActive = true " +
            "AND b.room.deleted = false " +
            "AND b.deleted = false " +
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

    // ✅ GARDÉ pour usage interne
    List<Bed> findByRoomId(Long roomId);

    Optional<Bed> findByRoomIdAndBedNumber(Long roomId, String bedNumber);
}
