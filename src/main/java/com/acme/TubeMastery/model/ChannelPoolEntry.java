package com.acme.TubeMastery.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/** Une chaîne YouTube qui peut être tirée dans un paquet (seule la fiche complète est scrapée au premier tirage). */
@Entity
@Data
@Table(name = "channel_pool")
public class ChannelPoolEntry {

    public enum Status { NEW, OK, DEAD }

    @Id
    private String handle;
    /** Connu à l'import (Wikidata) puis mis à jour au scraping ; null si inconnu. */
    private Long subscriberCount;
    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;
    private String source;
}
