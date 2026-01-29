package com.hostel.management.repository;

import com.hostel.management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ===================================
    // REQUÊTES DE BASE
    // ===================================

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByResetCode(String resetCode);

    // ===================================
    // REQUÊTES DE LECTURE (SELECT)
    // ===================================

    /**
     * Trouve les utilisateurs avec code de réinitialisation expiré
     * Utile pour le logging détaillé
     */
    @Query("SELECT u FROM User u WHERE u.resetCodeExpiry < :now AND u.resetCode IS NOT NULL")
    List<User> findUsersWithExpiredResetCode(@Param("now") LocalDateTime now);

    /**
     * Trouve les comptes verrouillés expirés
     * Utile pour le logging détaillé
     */
    @Query("SELECT u FROM User u WHERE u.lockedUntil < :now AND u.lockedUntil IS NOT NULL")
    List<User> findUsersWithExpiredLock(@Param("now") LocalDateTime now);

    // ===================================
    // REQUÊTES D'ÉCRITURE OPTIMISÉES (UPDATE)
    // ===================================

    /**
     * ✅ OPTIMISATION : Nettoie les codes expirés en une seule requête UPDATE
     * Utilisé par la tâche planifiée de nettoyage
     * @param now Date/heure actuelle
     * @return Nombre de codes nettoyés
     */
    @Modifying
    @Query("UPDATE User u SET u.resetCode = NULL, u.resetCodeExpiry = NULL " +
            "WHERE u.resetCodeExpiry < :now AND u.resetCodeExpiry IS NOT NULL")
    int cleanupExpiredResetCodes(@Param("now") LocalDateTime now);

    /**
     * ✅ OPTIMISATION : Débloque les comptes expirés en une seule requête UPDATE
     * Utilisé par la tâche planifiée de déblocage
     * @param now Date/heure actuelle
     * @return Nombre de comptes débloqués
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = NULL " +
            "WHERE u.lockedUntil < :now AND u.lockedUntil IS NOT NULL")
    int unlockExpiredAccounts(@Param("now") LocalDateTime now);

    // ===================================
    // REQUÊTES SUPPLÉMENTAIRES (BONUS)
    // ===================================

    /**
     * Compte les utilisateurs avec tentatives de connexion échouées
     * Utile pour le monitoring de sécurité
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.failedLoginAttempts > 0")
    long countUsersWithFailedAttempts();

    /**
     * Compte les comptes actuellement verrouillés
     * Utile pour le dashboard admin
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lockedUntil > :now")
    long countLockedAccounts(@Param("now") LocalDateTime now);

    /**
     * Trouve les utilisateurs créés dans les dernières N heures
     * Utile pour le monitoring des inscriptions
     */
    @Query("SELECT u FROM User u WHERE u.createdAt > :since ORDER BY u.createdAt DESC")
    List<User> findRecentUsers(@Param("since") LocalDateTime since);
}
