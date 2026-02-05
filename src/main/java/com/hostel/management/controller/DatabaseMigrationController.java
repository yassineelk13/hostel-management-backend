package com.hostel.management.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/migration")
@Slf4j
public class DatabaseMigrationController {

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/add-soft-delete")
    @Transactional
    public ResponseEntity<String> addSoftDeleteColumns() {
        try {
            log.info("🔧 Début de la migration soft delete...");

            // 1. Ajoute colonne deleted à rooms
            try {
                entityManager.createNativeQuery(
                        "ALTER TABLE rooms ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false NOT NULL"
                ).executeUpdate();
                log.info("✅ Colonne deleted ajoutée à rooms");
            } catch (Exception e) {
                log.warn("⚠️ Colonne deleted existe déjà dans rooms: {}", e.getMessage());
            }

            // 2. Ajoute colonne deleted à beds
            try {
                entityManager.createNativeQuery(
                        "ALTER TABLE beds ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false NOT NULL"
                ).executeUpdate();
                log.info("✅ Colonne deleted ajoutée à beds");
            } catch (Exception e) {
                log.warn("⚠️ Colonne deleted existe déjà dans beds: {}", e.getMessage());
            }

            // 3. Initialise les valeurs
            int roomsUpdated = entityManager.createNativeQuery(
                    "UPDATE rooms SET deleted = false WHERE deleted IS NULL"
            ).executeUpdate();
            log.info("✅ {} chambres initialisées", roomsUpdated);

            int bedsUpdated = entityManager.createNativeQuery(
                    "UPDATE beds SET deleted = false WHERE deleted IS NULL"
            ).executeUpdate();
            log.info("✅ {} lits initialisés", bedsUpdated);

            // 4. Crée les index
            try {
                entityManager.createNativeQuery(
                        "CREATE INDEX IF NOT EXISTS idx_room_deleted ON rooms(deleted)"
                ).executeUpdate();
                log.info("✅ Index idx_room_deleted créé");
            } catch (Exception e) {
                log.warn("⚠️ Index existe déjà: {}", e.getMessage());
            }

            try {
                entityManager.createNativeQuery(
                        "CREATE INDEX IF NOT EXISTS idx_bed_deleted ON beds(deleted)"
                ).executeUpdate();
                log.info("✅ Index idx_bed_deleted créé");
            } catch (Exception e) {
                log.warn("⚠️ Index existe déjà: {}", e.getMessage());
            }

            // 5. Supprime la contrainte UNIQUE
            try {
                entityManager.createNativeQuery(
                        "ALTER TABLE rooms DROP CONSTRAINT IF EXISTS rooms_room_number_key"
                ).executeUpdate();
                log.info("✅ Contrainte UNIQUE supprimée");
            } catch (Exception e) {
                log.warn("⚠️ Contrainte n'existe pas: {}", e.getMessage());
            }

            log.info("🎉 Migration terminée avec succès !");

            return ResponseEntity.ok(
                    "✅ Migration réussie !\n" +
                            "- Colonne deleted ajoutée à rooms et beds\n" +
                            "- " + roomsUpdated + " chambres initialisées\n" +
                            "- " + bedsUpdated + " lits initialisés\n" +
                            "- Index créés\n" +
                            "- Contrainte UNIQUE supprimée"
            );

        } catch (Exception e) {
            log.error("❌ Erreur lors de la migration: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body("❌ Erreur: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    @Transactional(readOnly = true)
    public ResponseEntity<String> verifyMigration() {
        try {
            Object roomsCount = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM rooms WHERE deleted = false"
            ).getSingleResult();

            Object bedsCount = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM beds WHERE deleted = false"
            ).getSingleResult();

            return ResponseEntity.ok(
                    "✅ Vérification réussie !\n" +
                            "- Chambres actives: " + roomsCount + "\n" +
                            "- Lits actifs: " + bedsCount
            );

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("❌ Colonnes deleted n'existent pas encore: " + e.getMessage());
        }
    }
}
