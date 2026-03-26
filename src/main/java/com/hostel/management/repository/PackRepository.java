package com.hostel.management.repository;

import com.hostel.management.entity.Pack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackRepository extends JpaRepository<Pack, Long> {

    List<Pack> findByIsActiveTrue();

    @Query("SELECT p FROM Pack p WHERE p.isActive = true ORDER BY p.priceDortoir ASC")
    List<Pack> findActivePacksOrderByPrice();
}