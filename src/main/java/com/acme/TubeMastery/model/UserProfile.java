package com.acme.TubeMastery.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Profil d'un joueur : informations complémentaires liées à un seul User (1-1). */
@Entity
@Table(name = "user_profiles")
@Data
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String bio;

    private String avatarUrl;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;
}
