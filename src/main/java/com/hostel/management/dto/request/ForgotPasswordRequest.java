package com.hostel.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;
}
