package com.hostel.management.service;

import com.hostel.management.entity.Booking;
import com.hostel.management.entity.HostelSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final HostelSettingsService hostelSettingsService;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:yassineelkrik13@gmail.com}")
    private String fromEmail;

    @Value("${hostel.name}")
    private String hostelName;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Async
    public void sendBookingConfirmation(Booking booking) {
        try {
            String emailContent = buildBookingConfirmationEmail(booking);
            sendEmail(
                    booking.getGuestEmail(),
                    booking.getGuestName(),
                    "Booking Confirmation - " + hostelName,  // ✅ CHANGÉ: En anglais (moins spam)
                    emailContent
            );
            log.info("✅ Email de confirmation envoyé à: {}", booking.getGuestEmail());
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi de l'email de confirmation: ", e);
        }
    }

    @Async
    public void sendPasswordResetCode(String toEmail, String code) {
        try {
            String emailContent = buildPasswordResetCodeEmail(code);
            sendEmail(
                    toEmail,
                    null,
                    "Code de réinitialisation - " + hostelName,
                    emailContent
            );
            log.info("✅ Code de réinitialisation envoyé à: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'envoi du code de réinitialisation: ", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }

    // ===== MÉTHODE GÉNÉRIQUE POUR ENVOYER VIA API BREVO =====
    private void sendEmail(String toEmail, String toName, String subject, String htmlContent) throws Exception {
        OkHttpClient client = new OkHttpClient();

        JSONObject emailJson = new JSONObject();

        JSONObject sender = new JSONObject();
        sender.put("name", hostelName);
        sender.put("email", fromEmail);
        emailJson.put("sender", sender);

        JSONArray to = new JSONArray();
        JSONObject recipient = new JSONObject();
        recipient.put("email", toEmail);
        if (toName != null && !toName.isEmpty()) {
            recipient.put("name", toName);
        }
        to.put(recipient);
        emailJson.put("to", to);

        emailJson.put("subject", subject);
        emailJson.put("htmlContent", htmlContent);

        RequestBody body = RequestBody.create(emailJson.toString(), JSON);

        Request request = new Request.Builder()
                .url(BREVO_API_URL)
                .addHeader("api-key", brevoApiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("accept", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("✅ Email envoyé avec succès via Brevo API à: {}", toEmail);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("❌ Erreur Brevo API: {} - {}", response.code(), errorBody);
                throw new RuntimeException("Erreur Brevo API: " + response.code());
            }
        }
    }

    // ===== TEMPLATE EMAIL CODE DE RÉINITIALISATION (INCHANGÉ) =====
    private String buildPasswordResetCodeEmail(String code) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head>");
        html.append("<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>");

        html.append("<div style='background: linear-gradient(135deg, #d97339 0%, #c75a2a 100%); color: white; padding: 30px; text-align: center;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>").append(hostelName).append("</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 14px; opacity: 0.9;'>Administration</p>");
        html.append("</div>");

        html.append("<div style='padding: 40px 30px;'>");
        html.append("<h2 style='color: #2c3e50; margin-top: 0;'>Code de réinitialisation</h2>");
        html.append("<p style='color: #555; line-height: 1.6;'>Vous avez demandé à réinitialiser votre mot de passe.</p>");
        html.append("<p style='color: #555; line-height: 1.6;'>Voici votre code de vérification :</p>");

        html.append("<div style='background-color: #f8f9fa; border: 2px dashed #d97339; border-radius: 10px; padding: 20px; margin: 30px 0; text-align: center;'>");
        html.append("<p style='margin: 0; color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: 1px;'>Votre code</p>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 36px; font-weight: bold; color: #d97339; letter-spacing: 8px;'>");
        html.append(code);
        html.append("</p>");
        html.append("</div>");

        html.append("<div style='background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0;'>");
        html.append("<p style='margin: 0; color: #e65100; font-size: 14px;'>");
        html.append("<strong>⏱️ Ce code expire dans 15 minutes.</strong>");
        html.append("</p>");
        html.append("</div>");

        html.append("<p style='color: #888; font-size: 13px; line-height: 1.6;'>");
        html.append("Si vous n'avez pas demandé cette réinitialisation, ignorez cet email. ");
        html.append("Votre mot de passe restera inchangé.");
        html.append("</p>");

        html.append("</div>");

        html.append("<div style='background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #dee2e6;'>");
        html.append("<p style='color: #6c757d; font-size: 12px; margin: 5px 0;'>&copy; 2026 ").append(hostelName).append("</p>");
        html.append("<p style='color: #6c757d; font-size: 12px; margin: 5px 0;'>Tous droits réservés</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    // ===== 🆕 NOUVEAU TEMPLATE EMAIL CONFIRMATION SIMPLIFIÉ (STYLE RÉINITIALISATION) =====
    // ===== 🆕 TEMPLATE EMAIL CONFIRMATION AVEC SERVICES ET PRIX (ANTI-SPAM) =====
    private String buildBookingConfirmationEmail(Booking booking) {
        HostelSettings settings = hostelSettingsService.getSettings();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head>");
        html.append("<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>");

        // ✅ Header simple (même style qui marche)
        html.append("<div style='background: linear-gradient(135deg, #d97339 0%, #c75a2a 100%); color: white; padding: 30px; text-align: center;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>").append(hostelName).append("</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 14px; opacity: 0.9;'>Booking Confirmation</p>");
        html.append("</div>");

        // ✅ Content simple
        html.append("<div style='padding: 40px 30px;'>");

        // Message de confirmation
        html.append("<h2 style='color: #2c3e50; margin-top: 0;'>Your booking is confirmed</h2>");
        html.append("<p style='color: #555; line-height: 1.6;'>Hello <strong>").append(booking.getGuestName()).append("</strong>,</p>");
        html.append("<p style='color: #555; line-height: 1.6;'>Thank you for your reservation. We look forward to welcoming you.</p>");

        // ✅ Informations de séjour (format simple)
        html.append("<div style='background-color: #f8f9fa; border-radius: 10px; padding: 20px; margin: 20px 0;'>");

        html.append("<p style='margin: 10px 0; color: #555; line-height: 1.8;'>");
        html.append("<strong>Check-in:</strong> ").append(booking.getCheckInDate().format(formatter));
        html.append("</p>");

        html.append("<p style='margin: 10px 0; color: #555; line-height: 1.8;'>");
        html.append("<strong>Check-out:</strong> ").append(booking.getCheckOutDate().format(formatter));
        html.append("</p>");

        // ✅ Lits (simple)
        if (!booking.getBeds().isEmpty()) {
            html.append("<p style='margin: 10px 0; color: #555; line-height: 1.8;'>");
            html.append("<strong>Room and bed:</strong> ");
            boolean first = true;
            for (var bed : booking.getBeds()) {
                if (!first) html.append(", ");
                html.append("Room ").append(bed.getRoom().getRoomNumber());
                html.append(" - Bed ").append(bed.getBedNumber());
                first = false;
            }
            html.append("</p>");
        }

        // ✅ NOUVEAU : Services (format texte simple, pas de liste à puces)
        // ✅ Services (avec liste à puces)
        if (!booking.getServices().isEmpty()) {
            html.append("<p style='margin: 15px 0 5px 0; color: #555; line-height: 1.8;'>");
            html.append("<strong>Included services:</strong>");
            html.append("</p>");

            html.append("<ul style='margin: 5px 0 10px 20px; padding-left: 20px; color: #666;'>");
            for (var service : booking.getServices()) {
                html.append("<li style='margin: 5px 0; line-height: 1.6;'>");
                html.append(service.getName());
                html.append("</li>");
            }
            html.append("</ul>");
        }


        // ✅ Pack si présent (simple)
        if (booking.getPack() != null) {
            html.append("<p style='margin: 15px 0 5px 0; color: #555; line-height: 1.8;'>");
            html.append("<strong>Package:</strong> ").append(booking.getPack().getName());
            html.append("</p>");
        }

        // ✅ NOUVEAU : Prix (discret, pas en gros)
        html.append("<p style='margin: 15px 0 5px 0; color: #555; line-height: 1.8;'>");
        html.append("<strong>Amount:</strong> ").append(booking.getTotalPrice()).append(" MAD");
        html.append("</p>");

        html.append("</div>");

        // ✅ Code d'accès (style qui marche)
        html.append("<div style='background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0;'>");
        html.append("<p style='margin: 0; color: #e65100; font-size: 14px;'>");
        html.append("<strong>Door access code: ").append(settings.getDoorCode()).append("</strong>");
        html.append("</p>");
        html.append("</div>");

        // ✅ Infos contact (minimaliste)
        html.append("<p style='color: #555; font-size: 14px; line-height: 1.6; margin: 20px 0;'>");
        html.append("Address : ").append(settings.getAddress()).append("<br>");
        html.append("Phone : ").append(settings.getPhone()).append("<br>");
        html.append("Email : ").append(settings.getEmail()).append("<br>");
        html.append("WiFi : ").append(settings.getWifiPassword());
        html.append("</p>");


        // ✅ Message de fin
        html.append("<p style='color: #888; font-size: 13px; line-height: 1.6;'>");
        html.append("If you have any questions, please contact us directly.");
        html.append("</p>");

        html.append("</div>");

        // ✅ Footer simple (style qui marche)
        html.append("<div style='background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #dee2e6;'>");
        html.append("<p style='color: #6c757d; font-size: 12px; margin: 5px 0;'>&copy; 2026 ").append(hostelName).append("</p>");
        html.append("<p style='color: #6c757d; font-size: 12px; margin: 5px 0;'>All rights reserved</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

}
