package com.hostel.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeEmailRequest {

    @NotBlank(message = "Le nouvel email est requis")
    @Email(message = "Format d'email invalide")
    private String newEmail;

    @NotBlank(message = "Le mot de passe actuel est requis")
    private String password;
}
