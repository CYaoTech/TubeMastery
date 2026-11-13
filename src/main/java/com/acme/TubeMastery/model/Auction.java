package com.acme.TubeMastery.model;

import com.acme.TubeMastery.service.Card;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Enchère d'une carte. La carte est retirée de la collection du vendeur tant que l'enchère est ACTIVE. */
@Entity
@Table(name = "auctions")
@Getter
@Setter
@NoArgsConstructor
public class Auction {

    public enum Status { ACTIVE, SOLD, EXPIRED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne(optional = false)
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(nullable = false)
    private int startPrice;

    /** Prix de départ tant qu'il n'y a aucune mise, puis montant de la meilleure mise. */
    @Column(nullable = false)
    private int currentPrice;

    @ManyToOne
    @JoinColumn(name = "highest_bidder_id")
    private User highestBidder;

    @Column(nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("amount DESC")
    private List<Bid> bids = new ArrayList<>();
}
