package com.hostel.management.repository;

import com.hostel.management.entity.Room;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // ✅ CHARGER LES LITS AUTOMATIQUEMENT
    @EntityGraph(attributePaths = {"beds"})
    @Override
    Optional<Room> findById(Long id);

    @EntityGraph(attributePaths = {"beds"})
    @Override
    List<Room> findAll();

    @EntityGraph(attributePaths = {"beds"})
    List<Room> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"beds"})
    List<Room> findByRoomType(Room.RoomType roomType);

    // ✅ NOUVEAU : Trouver par numéro de chambre
    @EntityGraph(attributePaths = {"beds"})
    Optional<Room> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    // ✅ Requête de disponibilité (déjà bonne)
    @EntityGraph(attributePaths = {"beds"})
    @Query("SELECT DISTINCT r FROM Room r " +
            "JOIN r.beds b " +
            "WHERE r.isActive = true " +
            "AND b.id NOT IN (" +
            "  SELECT bed.id FROM Booking bk " +
            "  JOIN bk.beds bed " +
            "  WHERE bk.status != 'CANCELLED' " +
            "  AND bk.checkInDate < :checkOut " +
            "  AND bk.checkOutDate > :checkIn" +
            ")")
    List<Room> findAvailableRooms(@Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut);

    // ✅ NOUVEAU : Chambres avec au moins X lits disponibles
    @EntityGraph(attributePaths = {"beds"})
    @Query("SELECT DISTINCT r FROM Room r " +
            "JOIN r.beds b " +
            "WHERE r.isActive = true " +
            "AND r.roomType = :roomType " +
            "AND b.id NOT IN (" +
            "  SELECT bed.id FROM Booking bk " +
            "  JOIN bk.beds bed " +
            "  WHERE bk.status != 'CANCELLED' " +
            "  AND bk.checkInDate < :checkOut " +
            "  AND bk.checkOutDate > :checkIn" +
            ")")
    List<Room> findAvailableRoomsByType(
            @Param("roomType") Room.RoomType roomType,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    // ✅ NOUVEAU : Statistiques des chambres
    @Query("SELECT r.roomType, COUNT(r) FROM Room r WHERE r.isActive = true GROUP BY r.roomType")
    List<Object[]> countRoomsByType();
}
