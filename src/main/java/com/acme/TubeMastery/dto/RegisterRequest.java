package com.acme.TubeMastery.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Le pseudo ne peut pas être vide")
    @Size(min = 3, max = 50,
          message = "Le pseudo doit faire entre 3 et 50 caractères")
    private String username;

    @NotBlank(message = "L'adresse e-mail ne peut pas être vide")
    @Email(message = "L'adresse e-mail doit être valide")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Le mot de passe ne peut pas être vide")
    @Size(min = 6,
          message = "Le mot de passe doit faire au moins 6 caractères")
    private String password;

    @NotBlank(message = "Veuillez confirmer votre mot de passe")
    private String confirmPassword;
}