// ✅ NOUVEAU : src/main/java/com/hostel/management/scheduling/SecurityCleanupTask.java
package com.hostel.management.scheduling;

import com.hostel.management.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityCleanupTask {

    private final AuthService authService;

    // ✅ Nettoyer les codes expirés toutes les heures
    @Scheduled(fixedRate = 3600000) // 1 heure
    public void cleanupExpiredResetCodes() {
        try {
            authService.cleanupExpiredResetCodes();
        } catch (Exception e) {
            log.error("Erreur lors du nettoyage des codes expirés", e);
        }
    }

    // ✅ Débloquer les comptes toutes les 10 minutes
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void unlockExpiredAccounts() {
        try {
            authService.unlockExpiredAccounts();
        } catch (Exception e) {
            log.error("Erreur lors du déblocage des comptes", e);
        }
    }
}
