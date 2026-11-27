package com.acme.TubeMastery.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Catégorie de chaînes (gaming, musique...). Liée aux cartes en N-N. */
@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "categories")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Card> cards = new ArrayList<>();
}
