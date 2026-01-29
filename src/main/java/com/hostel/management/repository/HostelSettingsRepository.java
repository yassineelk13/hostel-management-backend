package com.hostel.management.repository;

import com.hostel.management.entity.HostelSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HostelSettingsRepository extends JpaRepository<HostelSettings, Long> {
}
