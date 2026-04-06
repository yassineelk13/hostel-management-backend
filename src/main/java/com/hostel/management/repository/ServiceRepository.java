package com.hostel.management.repository;

import com.hostel.management.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByIsActiveTrue();

    List<Service> findByCategory(Service.ServiceCategory category);

    List<Service> findByCategoryAndIsActiveTrue(Service.ServiceCategory category);

    @Query("SELECT s FROM Service s WHERE s.isActive = true ORDER BY s.price ASC")
    List<Service> findActiveServicesOrderByPrice();

    List<Service> findByPriceTypeAndIsActiveTrue(Service.PriceType priceType);

    // ✅ NEW: find services by pricingType (PER_PERSON / PER_ROOM)
    List<Service> findByPricingTypeAndIsActiveTrue(Service.PricingType pricingType);

    // ✅ NEW: find per-person active services (surf, yoga, breakfast...)
    @Query("SELECT s FROM Service s WHERE s.isActive = true AND s.pricingType = 'PER_PERSON' ORDER BY s.category, s.price ASC")
    List<Service> findActivePerPersonServices();

    // ✅ NEW: find per-room active services (transport, shuttle...)
    @Query("SELECT s FROM Service s WHERE s.isActive = true AND s.pricingType = 'PER_ROOM' ORDER BY s.price ASC")
    List<Service> findActivePerRoomServices();
}