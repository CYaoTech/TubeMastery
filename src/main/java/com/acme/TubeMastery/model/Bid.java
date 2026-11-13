package com.acme.TubeMastery.model;

import jakarta.persistence.*;

import java.time.Instant;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bids")
@Getter
@Setter
@NoArgsConstructor
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bidder_id")
    private User bidder;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
