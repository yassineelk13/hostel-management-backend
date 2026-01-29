package com.hostel.management.repository;

import com.hostel.management.entity.Pack;
import com.hostel.management.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackRepository extends JpaRepository<Pack, Long> {

    List<Pack> findByIsActiveTrue();

    // ✅ NOUVEAU : Packs par type de chambre
    List<Pack> findByRoomTypeAndIsActiveTrue(Room.RoomType roomType);

    // ✅ NOUVEAU : Packs triés par prix
    @Query("SELECT p FROM Pack p WHERE p.isActive = true ORDER BY p.promoPrice ASC")
    List<Pack> findActivePacksOrderByPrice();

    // ✅ NOUVEAU : Packs avec durée spécifique
    List<Pack> findByDurationDaysAndIsActiveTrue(Integer durationDays);
}
