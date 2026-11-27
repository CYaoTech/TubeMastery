package com.acme.TubeMastery.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
public class Card {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @Column(unique = true)
        private String handle;
        private String name;
        private String description;
        private String thumbnailUrl;
        private Long subscriberCount;

        /** Catégories de la chaîne (N-N) : une carte peut en avoir plusieurs. */
        @ManyToMany
        @JoinTable(
                name = "card_categories",
                joinColumns = @JoinColumn(name = "card_id"),
                inverseJoinColumns = @JoinColumn(name = "category_id")
        )
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        private List<Category> categories = new ArrayList<>();

        @Transient
        public String getRarityCode() {
                long subs = subscriberCount == null ? 0 : subscriberCount;
                if (subs >= 100_000_000L) return "M";
                if (subs >= 10_000_000L) return "L";
                if (subs >= 1_000_000L) return "UR";
                if (subs >= 100_000L) return "R";
                if (subs >= 10_000L) return "C";
                return "B";
        }

        @Transient
        public String getRarityLabel() {
                return switch (getRarityCode()) {
                        case "M" -> "Mythique";
                        case "L" -> "Légendaire";
                        case "UR" -> "Ultra Rare";
                        case "R" -> "Rare";
                        case "C" -> "Commun";
                        default -> "Basique";
                };
        }
}
