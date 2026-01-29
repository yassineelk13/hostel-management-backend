package com.hostel.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;  // ✅ AJOUTER
import lombok.Data;
import lombok.NoArgsConstructor;  // ✅ AJOUTER

@Data
@NoArgsConstructor   // ✅ AJOUTER
@AllArgsConstructor  // ✅ AJOUTER
public class RegisterRequest {

    @NotBlank(message = "Email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Mot de passe est obligatoire")
    @Size(min = 8, message = "Mot de passe doit contenir au moins 8 caractères")
    private String password;  // ✅ VÉRIFIER QUE C'EST "password" (minuscule)

    @NotBlank(message = "Nom complet est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String fullName;

    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Format de téléphone invalide"
    )
    private String phone;
}
