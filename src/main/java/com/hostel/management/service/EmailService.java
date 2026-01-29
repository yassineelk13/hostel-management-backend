package com.hostel.management.service;

import com.hostel.management.entity.Booking;
import com.hostel.management.entity.HostelSettings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final HostelSettingsService hostelSettingsService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${hostel.name}")
    private String hostelName;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    public void sendBookingConfirmation(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(booking.getGuestEmail());
            helper.setSubject("Confirmation de réservation - " + hostelName);

            String emailContent = buildBookingConfirmationEmail(booking);
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("Email de confirmation envoyé à: {}", booking.getGuestEmail());

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email: ", e);
        }
    }

    // ✅ REMPLACER sendPasswordResetEmail PAR CECI :
    @Async
    public void sendPasswordResetCode(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Code de réinitialisation - " + hostelName);

            String emailContent = buildPasswordResetCodeEmail(code);
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("Code de réinitialisation envoyé à: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email de réinitialisation: ", e);
            throw new RuntimeException("Erreur lors de l'envoi de l'email");
        }
    }

    // ✅ NOUVEAU TEMPLATE EMAIL AVEC CODE
    private String buildPasswordResetCodeEmail(String code) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head>");
        html.append("<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>");

        // Header
        html.append("<div style='background: linear-gradient(135deg, #d97339 0%, #c75a2a 100%); color: white; padding: 30px; text-align: center;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>").append(hostelName).append("</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 14px; opacity: 0.9;'>Administration</p>");
        html.append("</div>");

        // Content
        html.append("<div style='padding: 40px 30px;'>");
        html.append("<h2 style='color: #2c3e50; margin-top: 0;'>Code de réinitialisation</h2>");
        html.append("<p style='color: #555; line-height: 1.6;'>Vous avez demandé à réinitialiser votre mot de passe.</p>");
        html.append("<p style='color: #555; line-height: 1.6;'>Voici votre code de vérification :</p>");

        // Code Box
        html.append("<div style='background-color: #f8f9fa; border: 2px dashed #d97339; border-radius: 10px; padding: 20px; margin: 30px 0; text-align: center;'>");
        html.append("<p style='margin: 0; color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: 1px;'>Votre code</p>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 36px; font-weight: bold; color: #d97339; letter-spacing: 8px;'>");
        html.append(code);
        html.append("</p>");
        html.append("</div>");

        // Info
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

        // Footer
        html.append("<div style='background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #dee2e6;'>");
        html.append("<p style='color: #6c757d; font-size: 12px; margin: 5px 0;'>&copy; 2026 ").append(hostelName).append("</p>");
        html.append("<p style='color: #6c757d; font-size: 12px; margin: 5px 0;'>Tous droits réservés</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }


    // ✅ NOUVEAU : TEMPLATE EMAIL RÉINITIALISATION
    private String buildPasswordResetEmail(String token) {
        String resetUrl = baseUrl + "/admin/reset-password?token=" + token;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head>");
        html.append("<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>");

        // Header
        html.append("<div style='background: linear-gradient(135deg, #d97339 0%, #c75a2a 100%); color: white; padding: 30px; text-align: center;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>").append(hostelName).append("</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 14px; opacity: 0.9;'>Administration</p>");
        html.append("</div>");

        // Content
        html.append("<div style='padding: 40px 30px;'>");
        html.append("<h2 style='color: #2c3e50; margin-top: 0;'>Réinitialisation de mot de passe</h2>");
        html.append("<p style='color: #555; line-height: 1.6;'>Vous avez demandé à réinitialiser votre mot de passe.</p>");
        html.append("<p style='color: #555; line-height: 1.6;'>Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe :</p>");

        // Button
        html.append("<div style='text-align: center; margin: 30px 0;'>");
        html.append("<a href='").append(resetUrl).append("' ");
        html.append("style='display: inline-block; padding: 15px 40px; background-color: #d97339; color: white; ");
        html.append("text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px;'>");
        html.append("Réinitialiser mon mot de passe");
        html.append("</a>");
        html.append("</div>");

        // Info
        html.append("<div style='background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0;'>");
        html.append("<p style='margin: 0; color: #e65100; font-size: 14px;'>");
        html.append("<strong>⏱️ Ce lien expire dans 1 heure.</strong>");
        html.append("</p>");
        html.append("</div>");

        html.append("<p style='color: #888; font-size: 13px; line-height: 1.6;'>");
        html.append("Si vous n'avez pas demandé cette réinitialisation, ignorez cet email. ");
        html.append("Votre mot de passe restera inchangé.");
        html.append("</p>");

        // Link alternatif
        html.append("<div style='margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;'>");
        html.append("<p style='color: #888; font-size: 12px; margin: 5px 0;'>Si le bouton ne fonctionne pas, copiez ce lien :</p>");
        html.append("<p style='color: #3498db; font-size: 12px; word-break: break-all;'>").append(resetUrl).append("</p>");
        html.append("</div>");

        html.append("</div>");

        // Footer
        html.append("<div style='background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #dee2e6;'>");
        html.append("<p style='color: #6c757d; font-size: 12px; margin: 5px 0;'>&copy; 2026 ").append(hostelName).append("</p>");
        html.append("<p style='color: #6c757d; font-size: 12px; margin: 5px 0;'>Tous droits réservés</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    private String buildBookingConfirmationEmail(Booking booking) {
        HostelSettings settings = hostelSettingsService.getSettings();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'></head>");
        html.append("<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>");

        // ===== HEADER =====
        html.append("<div style='background: linear-gradient(135deg, #d97339 0%, #c75a2a 100%); color: white; padding: 30px; text-align: center;'>");
        html.append("<h1 style='margin: 0; font-size: 28px;'>").append(hostelName).append("</h1>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 14px; opacity: 0.9;'>").append(settings.getAddress()).append("</p>");
        html.append("</div>");

        // ===== CONFIRMATION =====
        html.append("<div style='padding: 30px;'>");
        html.append("<div style='text-align: center; margin-bottom: 30px;'>");
        html.append("<div style='display: inline-block; background-color: #e8f5e9; color: #27ae60; padding: 15px 30px; border-radius: 50px; font-size: 18px; font-weight: bold;'>");
        html.append("✓ Réservation Confirmée");
        html.append("</div>");
        html.append("</div>");

        html.append("<p style='color: #2c3e50; font-size: 16px;'>Bonjour <strong>").append(booking.getGuestName()).append("</strong>,</p>");
        html.append("<p style='color: #555; line-height: 1.6;'>Merci pour votre réservation ! Nous avons hâte de vous accueillir.</p>");

        // ===== RÉFÉRENCE =====
        html.append("<div style='background-color: #f8f9fa; border-left: 4px solid #3498db; padding: 15px; margin: 20px 0;'>");
        html.append("<p style='margin: 0; color: #555;'><strong>Numéro de réservation :</strong></p>");
        html.append("<p style='margin: 5px 0 0 0; color: #3498db; font-size: 20px; font-weight: bold;'>");
        html.append(booking.getBookingReference());
        html.append("</p>");
        html.append("</div>");

        // ===== PACK (SI PRÉSENT) =====
        if (booking.getPack() != null) {
            html.append("<div style='background-color: #e3f2fd; border: 2px solid #2196f3; border-radius: 10px; padding: 20px; margin: 20px 0;'>");
            html.append("<h3 style='margin: 0 0 15px 0; color: #1976d2;'>📦 Votre Pack</h3>");
            html.append("<p style='margin: 5px 0; font-size: 20px; font-weight: bold; color: #2c3e50;'>");
            html.append(booking.getPack().getName());
            html.append("</p>");
            html.append("<p style='margin: 5px 0; color: #555;'>Durée : <strong>").append(booking.getPack().getDurationDays()).append(" jours</strong></p>");
            html.append("<p style='margin: 10px 0 0 0; color: #1976d2; font-size: 22px; font-weight: bold;'>");
            html.append(booking.getPack().getPromoPrice()).append(" MAD");
            html.append("</p>");
            html.append("</div>");
        }

        // ===== DATES ET PRIX =====
        html.append("<div style='background-color: #f8f9fa; border-radius: 10px; padding: 20px; margin: 20px 0;'>");
        html.append("<h3 style='margin-top: 0; color: #2c3e50;'>📅 Détails de votre séjour</h3>");

        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<tr><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6;'>");
        html.append("<strong style='color: #27ae60;'>✅ Arrivée</strong>");
        html.append("</td><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6; text-align: right;'>");
        html.append(booking.getCheckInDate().format(formatter));
        html.append("</td></tr>");

        html.append("<tr><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6;'>");
        html.append("<strong style='color: #e74c3c;'>❌ Départ</strong>");
        html.append("</td><td style='padding: 10px 0; border-bottom: 1px solid #dee2e6; text-align: right;'>");
        html.append(booking.getCheckOutDate().format(formatter));
        html.append(" <span style='color: #e74c3c; font-weight: bold;'>(avant ").append(settings.getCheckOutTime()).append(")</span>");
        html.append("</td></tr>");

        html.append("<tr><td style='padding: 15px 0;'>");
        html.append("<strong style='font-size: 18px; color: #2c3e50;'>💰 Prix Total</strong>");
        html.append("</td><td style='padding: 15px 0; text-align: right;'>");
        html.append("<span style='font-size: 24px; color: #d97339; font-weight: bold;'>");
        html.append(booking.getTotalPrice()).append(" MAD");
        html.append("</span>");
        html.append("</td></tr>");
        html.append("</table>");

        html.append("<p style='color: #e74c3c; margin: 15px 0 0 0; font-weight: bold;'>");
        html.append("💳 Paiement à l'arrivée (espèces ou carte)");
        html.append("</p>");
        html.append("</div>");

        // ===== LITS RÉSERVÉS =====
        if (!booking.getBeds().isEmpty()) {
            html.append("<div style='margin: 20px 0;'>");
            html.append("<h3 style='color: #2c3e50; margin-bottom: 10px;'>🛏️ Votre/Vos Lit(s)</h3>");
            booking.getBeds().forEach(bed -> {
                html.append("<div style='background-color: #f8f9fa; padding: 12px 15px; margin: 8px 0; border-left: 4px solid #3498db; border-radius: 5px;'>");
                html.append("Chambre <strong>").append(bed.getRoom().getRoomNumber()).append("</strong>");
                html.append(" - Lit <strong>").append(bed.getBedNumber()).append("</strong>");
                html.append("</div>");
            });
            html.append("</div>");
        }

        // ===== SERVICES ADDITIONNELS =====
        if (!booking.getServices().isEmpty()) {
            html.append("<div style='margin: 20px 0;'>");
            html.append("<h3 style='color: #2c3e50; margin-bottom: 10px;'>🎯 Services Inclus</h3>");
            html.append("<ul style='margin: 0; padding-left: 20px; color: #555;'>");
            booking.getServices().forEach(service ->
                    html.append("<li style='padding: 5px 0;'>")
                            .append(service.getName())
                            .append(" - <strong>").append(service.getPrice()).append(" MAD</strong></li>")
            );
            html.append("</ul>");
            html.append("</div>");
        }

        // ===== 🔑 CODE DE PORTE (NOUVEAU) =====
        html.append("<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 10px; padding: 25px; margin: 25px 0; text-align: center; color: white;'>");
        html.append("<h3 style='margin: 0 0 15px 0; font-size: 20px;'>🔑 Code d'accès à la porte</h3>");
        html.append("<div style='background-color: rgba(255,255,255,0.2); border: 2px dashed white; border-radius: 10px; padding: 20px; margin: 10px 0;'>");
        html.append("<p style='margin: 0; font-size: 14px; opacity: 0.9;'>Votre code pour entrer</p>");
        html.append("<p style='margin: 10px 0 0 0; font-size: 48px; font-weight: bold; letter-spacing: 8px;'>");
        html.append(settings.getDoorCode());
        html.append("</p>");
        html.append("</div>");
        html.append("<p style='margin: 15px 0 0 0; font-size: 13px; opacity: 0.9;'>");
        html.append("✨ Check-in disponible 24h/24");
        html.append("</p>");
        html.append("</div>");

        // ===== INFORMATIONS PRATIQUES =====
        html.append("<div style='background-color: #e8f5e9; border-radius: 10px; padding: 20px; margin: 20px 0;'>");
        html.append("<h3 style='margin-top: 0; color: #2c3e50;'>ℹ️ Informations Pratiques</h3>");

        html.append("<table style='width: 100%;'>");
        html.append("<tr><td style='padding: 8px 0; color: #555;'><strong>📍 Adresse</strong></td>");
        html.append("<td style='padding: 8px 0; color: #555; text-align: right;'>").append(settings.getAddress()).append("</td></tr>");

        html.append("<tr><td style='padding: 8px 0; color: #555;'><strong>📞 Téléphone</strong></td>");
        html.append("<td style='padding: 8px 0; color: #555; text-align: right;'>").append(settings.getPhone()).append("</td></tr>");

        html.append("<tr><td style='padding: 8px 0; color: #555;'><strong>📧 Email</strong></td>");
        html.append("<td style='padding: 8px 0; color: #555; text-align: right;'>").append(settings.getEmail()).append("</td></tr>");

        html.append("<tr><td style='padding: 8px 0; color: #555;'><strong>📶 WiFi</strong></td>");
        html.append("<td style='padding: 8px 0; color: #555; text-align: right;'>").append(settings.getWifiPassword()).append("</td></tr>");
        html.append("</table>");

        html.append("</div>");

        // ===== INSTRUCTIONS CHECK-IN =====
        if (settings.getCheckInInstructions() != null && !settings.getCheckInInstructions().isEmpty()) {
            html.append("<div style='background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;'>");
            html.append("<p style='margin: 0; color: #856404; line-height: 1.6;'>");
            html.append(settings.getCheckInInstructions());
            html.append("</p>");
            html.append("</div>");
        }

        html.append("</div>");

        // ===== FOOTER =====
        html.append("<div style='background-color: #2c3e50; color: white; padding: 25px; text-align: center;'>");
        html.append("<p style='margin: 0 0 10px 0; font-size: 18px; font-weight: bold;'>Bon séjour chez ").append(hostelName).append(" ! 🌴</p>");
        html.append("<p style='margin: 5px 0; font-size: 12px; opacity: 0.7;'>Réf : ").append(booking.getBookingReference()).append("</p>");
        html.append("<p style='margin: 15px 0 0 0; font-size: 11px; opacity: 0.6;'>&copy; 2026 ").append(hostelName).append(" - Tous droits réservés</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

}
