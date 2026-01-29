package com.hostel.management.config;

import com.hostel.management.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksConfig {

    private final AuthService authService;

    /**
     * Nettoyage quotidien des codes expirés (3h du matin)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void dailyCleanup() {
        log.info("🧹 Nettoyage quotidien : codes de réinitialisation expirés");
        authService.cleanupExpiredResetCodes();
    }

    /**
     * Déblocage des comptes toutes les 30 minutes
     * Démarre 1 minute après le lancement de l'application
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000)
    public void unlockAccounts() {
        log.debug("🔓 Vérification des comptes verrouillés expirés");
        authService.unlockExpiredAccounts();
    }
}
