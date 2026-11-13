package com.acme.TubeMastery.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Data;

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
