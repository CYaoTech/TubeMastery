package com.acme.TubeMastery.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

import com.acme.TubeMastery.service.Card;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUser;

    @NotBlank(message = "Le pseudo ne peut pas être vide")
    @Size(min = 3, max = 50,
          message = "Le pseudo doit faire entre 3 et 50 caractères")
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "L'adresse e-mail ne peut pas être vide")
    @Email(message = "L'adresse e-mail doit être valide")
    @Size(max = 254)
    @Column(unique = true, nullable = false, length = 254)
    private String email;

    @NotBlank(message = "Le mot de passe ne peut pas être vide")
    @Size(min = 6,
          message = "Le mot de passe doit faire au moins 6 caractères")
    @Column(nullable = false)
    @ToString.Exclude
    private String password;

    @NotNull
    @Min(value = 0, message = "Les TubeCoins ne peuvent pas être négatifs")
    @Column(nullable = false)
    private Integer tubecoin = 0;

    @ManyToMany
    @JoinTable(
        name = "user_cards",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Card> cards = new ArrayList<>();
}
