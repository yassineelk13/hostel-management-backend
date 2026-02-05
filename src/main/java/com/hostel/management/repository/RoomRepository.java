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

    // ✅ MODIFIÉ : Charge seulement les chambres actives et non supprimées
    @EntityGraph(attributePaths = {"beds"})
    @Query("SELECT r FROM Room r WHERE r.id = :id AND r.deleted = false")
    Optional<Room> findByIdAndNotDeleted(@Param("id") Long id);

    @EntityGraph(attributePaths = {"beds"})
    @Query("SELECT r FROM Room r WHERE r.deleted = false")
    List<Room> findAllNotDeleted();

    @EntityGraph(attributePaths = {"beds"})
    @Query("SELECT r FROM Room r WHERE r.isActive = true AND r.deleted = false")
    List<Room> findByIsActiveTrueAndNotDeleted();

    @EntityGraph(attributePaths = {"beds"})
    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType AND r.deleted = false")
    List<Room> findByRoomTypeAndNotDeleted(@Param("roomType") Room.RoomType roomType);

    // ✅ MODIFIÉ : Trouver par numéro parmi les chambres actives
    @EntityGraph(attributePaths = {"beds"})
    @Query("SELECT r FROM Room r WHERE r.roomNumber = :roomNumber AND r.deleted = false")
    Optional<Room> findByRoomNumberAndNotDeleted(@Param("roomNumber") String roomNumber);

    // ✅ MODIFIÉ : Vérifier l'existence parmi les chambres actives
    @Query("SELECT COUNT(r) > 0 FROM Room r WHERE r.roomNumber = :roomNumber AND r.deleted = false")
    boolean existsByRoomNumberAndNotDeleted(@Param("roomNumber") String roomNumber);

    // ✅ MODIFIÉ : Chambres disponibles (non supprimées)
    @EntityGraph(attributePaths = {"beds"})
    @Query("SELECT DISTINCT r FROM Room r " +
            "JOIN r.beds b " +
            "WHERE r.isActive = true " +
            "AND r.deleted = false " +
            "AND b.deleted = false " +
            "AND b.id NOT IN (" +
            "  SELECT bed.id FROM Booking bk " +
            "  JOIN bk.beds bed " +
            "  WHERE bk.status != 'CANCELLED' " +
            "  AND bk.checkInDate < :checkOut " +
            "  AND bk.checkOutDate > :checkIn" +
            ")")
    List<Room> findAvailableRooms(@Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut);

    // ✅ MODIFIÉ : Chambres disponibles par type (non supprimées)
    @EntityGraph(attributePaths = {"beds"})
    @Query("SELECT DISTINCT r FROM Room r " +
            "JOIN r.beds b " +
            "WHERE r.isActive = true " +
            "AND r.deleted = false " +
            "AND r.roomType = :roomType " +
            "AND b.deleted = false " +
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

    // ✅ MODIFIÉ : Statistiques (non supprimées)
    @Query("SELECT r.roomType, COUNT(r) FROM Room r WHERE r.isActive = true AND r.deleted = false GROUP BY r.roomType")
    List<Object[]> countRoomsByType();

    // ✅ GARDÉ pour usage interne (findById original)
    @EntityGraph(attributePaths = {"beds"})
    @Override
    Optional<Room> findById(Long id);

    @EntityGraph(attributePaths = {"beds"})
    @Override
    List<Room> findAll();

    // ✅ DEPRECATED : Utilise findByIsActiveTrueAndNotDeleted() à la place
    @Deprecated
    @EntityGraph(attributePaths = {"beds"})
    List<Room> findByIsActiveTrue();

    // ✅ DEPRECATED : Utilise findByRoomTypeAndNotDeleted() à la place
    @Deprecated
    @EntityGraph(attributePaths = {"beds"})
    List<Room> findByRoomType(Room.RoomType roomType);

    // ✅ DEPRECATED : Utilise findByRoomNumberAndNotDeleted() à la place
    @Deprecated
    @EntityGraph(attributePaths = {"beds"})
    Optional<Room> findByRoomNumber(String roomNumber);

    // ✅ DEPRECATED : Utilise existsByRoomNumberAndNotDeleted() à la place
    @Deprecated
    boolean existsByRoomNumber(String roomNumber);
}
