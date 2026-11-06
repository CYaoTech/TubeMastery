package com.acme.TubeMastery.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUser;

    @NotBlank(message = "Le pseudo ne peut pas être vide")
    @Size(min = 3, max = 50, message = "Le pseudo doit faire entre 3 et 50 caractères")
    @Column(unique = true)
    private String username;
    
    @NotBlank(message = "L'adresse email ne peut pas être vide")
    @Email(message = "L'adresse email doit être valide")
    @Column(unique = true)
    private String email;
    
    @NotBlank(message = "Le mot de passe ne peut pas être vide")
    @Size(min = 6, message = "Le mot de passe doit faire au moins 6 caractères")
    private String password;

    @NotNull
    @Min(value = 0, message = "Le solde de tubecoins ne peut pas être négatif")
    private Integer tubecoin = 0;
}
