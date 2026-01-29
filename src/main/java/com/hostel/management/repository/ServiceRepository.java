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

    // ✅ NOUVEAU : Services actifs par catégorie
    List<Service> findByCategoryAndIsActiveTrue(Service.ServiceCategory category);

    // ✅ NOUVEAU : Services triés par prix
    @Query("SELECT s FROM Service s WHERE s.isActive = true ORDER BY s.price ASC")
    List<Service> findActiveServicesOrderByPrice();

    // ✅ NOUVEAU : Services par type de prix
    List<Service> findByPriceTypeAndIsActiveTrue(Service.PriceType priceType);
}
