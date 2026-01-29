package com.hostel.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Mot de passe actuel est obligatoire")
    private String currentPassword;

    @NotBlank(message = "Nouveau mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    // ✅ OPTIONNEL : Validation stricte (à adapter selon tes besoins)
    // @Pattern(
    //     regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
    //     message = "Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre"
    // )
    private String newPassword;
}
