package com.hostel.management.service;

import com.hostel.management.entity.HostelSettings;
import com.hostel.management.repository.HostelSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HostelSettingsService {

    private final HostelSettingsRepository hostelSettingsRepository;

    public HostelSettings getSettings() {
        // ✅ CRÉER SI N'EXISTE PAS
        return hostelSettingsRepository.findAll().stream()
                .findFirst()
                .orElseGet(this::createDefaultSettings);
    }

    @Transactional
    public HostelSettings updateSettings(HostelSettings settings) {
        HostelSettings existing = getSettings();

        existing.setHostelName(settings.getHostelName());
        existing.setAddress(settings.getAddress());
        existing.setEmail(settings.getEmail());
        existing.setPhone(settings.getPhone());
        existing.setDoorCode(settings.getDoorCode());
        existing.setWifiPassword(settings.getWifiPassword());
        existing.setCheckIn24h(settings.isCheckIn24h());
        existing.setCheckInInstructions(settings.getCheckInInstructions());
        existing.setCheckOutTime(settings.getCheckOutTime());

        return hostelSettingsRepository.save(existing);
    }

    @Transactional
    public HostelSettings updateDoorCode(String newCode) {
        HostelSettings settings = getSettings();
        settings.setDoorCode(newCode);
        return hostelSettingsRepository.save(settings);
    }

    // ✅ PAS DE @Transactional ICI CAR MÉTHODE PRIVÉE
    private HostelSettings createDefaultSettings() {
        HostelSettings defaultSettings = HostelSettings.builder()
                .hostelName("ShamsHouse")
                .address("Agadir, Maroc")
                .email("contact@shamshouse.com")
                .phone("+212 6 12 34 56 78")
                .doorCode("123456")
                .wifiPassword("wifi2024")
                .checkIn24h(true)
                .checkInInstructions("Bienvenue à ShamsHouse ! Check-in disponible 24h/24. Utilisez votre code d'accès pour entrer.")
                .checkOutTime("12:00")
                .build();

        return hostelSettingsRepository.save(defaultSettings);
    }
}
