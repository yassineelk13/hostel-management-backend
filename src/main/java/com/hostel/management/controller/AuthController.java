package com.hostel.management.controller;

import com.hostel.management.dto.request.*;
import com.hostel.management.dto.response.ApiResponse;
import com.hostel.management.dto.response.AuthResponse;
import com.hostel.management.entity.User;
import com.hostel.management.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Inscription réussie", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(
                ApiResponse.success("Un code de vérification a été envoyé à votre email", null)
        );
    }

    // ✅ ÉTAPE 2 : Vérifier le code et changer le mot de passe
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody VerifyResetCodeRequest request) {
        authService.resetPasswordWithCode(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(
                ApiResponse.success("Mot de passe réinitialisé avec succès", null)
        );
    }

    // getCurrentUser, changeEmail restent identiques...
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = authService.getCurrentUser(email);
        return ResponseEntity.ok(ApiResponse.success("Informations récupérées", user));
    }

    @PostMapping("/change-email")
    public ResponseEntity<ApiResponse<Void>> changeEmail(
            @Valid @RequestBody ChangeEmailRequest request,
            Authentication authentication) {
        String currentEmail = authentication.getName();
        authService.changeEmail(currentEmail, request.getNewEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Email modifié avec succès", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        authService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Mot de passe changé avec succès", null));
    }
}
