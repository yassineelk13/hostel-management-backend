package com.hostel.management.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload une image depuis un fichier MultipartFile
     */
    /**
     * Upload une image depuis un fichier MultipartFile avec optimisation automatique
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        log.info("Uploading image to Cloudinary folder: {}", folder);

        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto",
                        "quality", "auto:best",           // Qualité automatique (meilleure)
                        "fetch_format", "auto",           // Format optimal (WebP si supporté)
                        "responsive", true                // Génère plusieurs tailles
                )
        );

        String url = (String) uploadResult.get("secure_url");
        log.info("Image uploaded successfully: {}", url);
        return url;
    }

    /**
     * Upload une image depuis une chaîne Base64
     */
    public String uploadBase64Image(String base64Image, String folder) throws IOException {
        log.info("Uploading Base64 image to Cloudinary folder: {}", folder);

        Map uploadResult = cloudinary.uploader().upload(base64Image,
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto",
                        "transformation", new com.cloudinary.Transformation()
                                .width(1200)
                                .height(900)
                                .crop("limit")
                                .quality("auto")
                )
        );

        String url = (String) uploadResult.get("secure_url");
        log.info("Base64 image uploaded successfully: {}", url);
        return url;
    }

    /**
     * Supprimer une image de Cloudinary
     */
    public void deleteImage(String imageUrl) {
        try {
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted successfully: {}", publicId);
        } catch (Exception e) {
            log.error("Error deleting image: {}", imageUrl, e);
        }
    }

    /**
     * Extraire le public_id depuis une URL Cloudinary
     */
    private String extractPublicId(String imageUrl) {
        try {
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) return imageUrl;

            String afterUpload = imageUrl.substring(uploadIndex + 8);

            if (afterUpload.matches("v\\d+/.*")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

            int dotIndex = afterUpload.lastIndexOf(".");
            return dotIndex != -1 ? afterUpload.substring(0, dotIndex) : afterUpload;

        } catch (Exception e) {
            log.error("Error extracting public_id from URL: {}", imageUrl, e);
            return imageUrl;
        }
    }
}
