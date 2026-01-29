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
        // Ex: https://res.cloudinary.com/xxx/image/upload/v123/shamshouse/rooms/abc.jpg
        // Retourne: shamshouse/rooms/abc
        try {
            String[] parts = imageUrl.split("/");
            int uploadIndex = -1;

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("upload")) {
                    uploadIndex = i;
                    break;
                }
            }

            if (uploadIndex != -1 && uploadIndex + 2 < parts.length) {
                // Construire le public_id
                StringBuilder publicId = new StringBuilder();
                for (int i = uploadIndex + 2; i < parts.length; i++) {
                    if (i > uploadIndex + 2) publicId.append("/");
                    publicId.append(parts[i].split("\\.")[0]); // Enlever l'extension
                }
                return publicId.toString();
            }
        } catch (Exception e) {
            log.error("Error extracting public_id from URL: {}", imageUrl, e);
        }
        return imageUrl;
    }
}
