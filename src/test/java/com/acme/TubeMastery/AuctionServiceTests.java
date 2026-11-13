package com.acme.TubeMastery;

import com.acme.TubeMastery.dto.AuctionDto;
import com.acme.TubeMastery.model.Auction;
import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.repository.AuctionRepository;
import com.acme.TubeMastery.repository.CardRepository;
import com.acme.TubeMastery.repository.UserRepository;
import com.acme.TubeMastery.service.AuctionException;
import com.acme.TubeMastery.service.AuctionService;
import com.acme.TubeMastery.service.Card;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuctionServiceTests {

    @Autowired AuctionService service;
    @Autowired AuctionRepository auctions;
    @Autowired UserRepository users;
    @Autowired CardRepository cards;

    private User user(int coins) {
        User u = new User();
        String n = "u" + UUID.randomUUID().toString().substring(0, 8);
        u.setUsername(n);
        u.setEmail(n + "@t.fr");
        u.setPassword("secret1");
        u.setTubecoin(coins);
        return users.save(u);
    }

    private Card card() {
        Card c = new Card();
        c.setHandle("h" + UUID.randomUUID());
        c.setName("Test");
        return cards.save(c);
    }

    @Test
    void fullFlow() {
        User seller = user(0), a = user(20), b = user(20);
        Card card = card();
        seller.getCards().add(card);

        AuctionDto dto = service.create(seller.getUsername(), card.getId(), 5, 1);
        assertTrue(seller.getCards().isEmpty());

        assertThrows(AuctionException.class, () -> service.bid(seller.getUsername(), dto.id(), 6)); // vendeur
        assertThrows(AuctionException.class, () -> service.bid(a.getUsername(), dto.id(), 4));      // < départ
        assertThrows(AuctionException.class, () -> service.bid(a.getUsername(), dto.id(), 25));     // > solde

        service.bid(a.getUsername(), dto.id(), 5);
        assertEquals(15, a.getTubecoin());
        assertThrows(AuctionException.class, () -> service.bid(b.getUsername(), dto.id(), 5));      // < +1

        service.bid(b.getUsername(), dto.id(), 6);
        assertEquals(20, a.getTubecoin());   // remboursé
        assertEquals(14, b.getTubecoin());

        // Anti-sniping : fin dans 10 s, une mise la remet à ~1 min
        Auction auction = auctions.findById(dto.id()).orElseThrow();
        auction.setEndsAt(Instant.now().plusSeconds(10));
        service.bid(a.getUsername(), dto.id(), 7);
        assertTrue(auction.getEndsAt().isAfter(Instant.now().plusSeconds(55)));

        // Clôture : a gagne, vendeur crédité
        auction.setEndsAt(Instant.now().minusSeconds(1));
        service.closeExpired();
        assertEquals(Auction.Status.SOLD, auction.getStatus());
        assertEquals(1, a.getCards().size());
        assertEquals(7, seller.getTubecoin());
    }

    @Test
    void noBidReturnsCard() {
        User seller = user(0);
        Card card = card();
        seller.getCards().add(card);
        AuctionDto dto = service.create(seller.getUsername(), card.getId(), 1, 3);
        Auction auction = auctions.findById(dto.id()).orElseThrow();
        auction.setEndsAt(Instant.now().minusSeconds(1));
        service.closeExpired();
        assertEquals(Auction.Status.EXPIRED, auction.getStatus());
        assertEquals(1, seller.getCards().size());
    }

    @Test
    void invalidParams() {
        User seller = user(0);
        Card card = card();
        seller.getCards().add(card);
        assertThrows(AuctionException.class, () -> service.create(seller.getUsername(), card.getId(), 0, 1));
        assertThrows(AuctionException.class, () -> service.create(seller.getUsername(), card.getId(), 1, 2));
    }
}
