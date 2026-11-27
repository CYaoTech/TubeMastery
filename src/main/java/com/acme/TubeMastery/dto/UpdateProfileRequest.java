package com.acme.TubeMastery.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** Formulaire de modification du profil (bio et avatar, tous deux facultatifs). */
@Data
public class UpdateProfileRequest {

    @Size(max = 500, message = "La bio doit faire 500 caractères maximum")
    private String bio;

    @Size(max = 255, message = "L'adresse de l'image est trop longue")
    @Pattern(regexp = "^$|^https?://\\S+$",
             message = "L'avatar doit être une adresse commençant par http:// ou https://")
    private String avatarUrl;
}
