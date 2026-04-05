package com.hostel.management.repository;

import com.hostel.management.entity.Pack;
import com.hostel.management.entity.PackNightPrice;
import com.hostel.management.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PackNightPriceRepository extends JpaRepository<PackNightPrice, Long> {

    List<PackNightPrice> findByPack(Pack pack);

    Optional<PackNightPrice> findByPackAndNightsAndRoomType(
            Pack pack, int nights, Room.RoomType roomType
    );

    void deleteByPack(Pack pack);
}