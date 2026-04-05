package com.hostel.management.repository;

import com.hostel.management.entity.Pack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackRepository extends JpaRepository<Pack, Long> {

    List<Pack> findByIsActiveTrue();

    // ✅ Trier par nom (priceDortoir supprimé)
    List<Pack> findByIsActiveTrueOrderByNameAsc();
}