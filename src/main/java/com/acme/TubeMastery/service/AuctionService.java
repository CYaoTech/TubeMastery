package com.acme.TubeMastery.service;

import com.acme.TubeMastery.dto.AuctionDto;
import com.acme.TubeMastery.model.Auction;
import com.acme.TubeMastery.model.Bid;
import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.repository.AuctionRepository;
import com.acme.TubeMastery.repository.CardRepository;
import com.acme.TubeMastery.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    public static final Set<Integer> ALLOWED_HOURS = Set.of(1, 3, 6, 12, 24);
    /** Une mise passée sous ce seuil remet le compteur à ce seuil. */
    public static final Duration SNIPING_WINDOW = Duration.ofMinutes(1);

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public AuctionService(AuctionRepository auctionRepository,
                          UserRepository userRepository,
                          CardRepository cardRepository) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    // ---------- Lecture ----------

    @Transactional(readOnly = true)
    public List<AuctionDto> listActive() {
        return auctionRepository.findByStatusOrderByEndsAtAsc(Auction.Status.ACTIVE)
            .stream().map(AuctionDto::from).toList();
    }

    @Transactional(readOnly = true)
    public AuctionDto get(Long id) {
        return AuctionDto.from(auctionRepository.findById(id)
            .orElseThrow(() -> new AuctionException(HttpStatus.NOT_FOUND, "Enchère introuvable")));
    }

    // ---------- Création / modification / annulation ----------

    @Transactional
    public AuctionDto create(String username, Long cardId, int startPrice, int hours) {
        checkParams(startPrice, hours);
        User seller = findUser(username);

        if (!removeOneCard(seller, cardId)) {
            throw new AuctionException(HttpStatus.NOT_FOUND, "Tu ne possèdes pas cette carte");
        }

        Auction auction = new Auction();
        auction.setSeller(seller);
        auction.setCard(cardRepository.getReferenceById(cardId));
        auction.setStartPrice(startPrice);
        auction.setCurrentPrice(startPrice);
        auction.setEndsAt(Instant.now().plus(Duration.ofHours(hours)));
        return AuctionDto.from(auctionRepository.save(auction));
    }

    /** Modification possible tant qu'aucune mise n'a été posée (réservée au vendeur). */
    @Transactional
    public AuctionDto update(String username, Long id, int startPrice, int hours) {
        checkParams(startPrice, hours);
        Auction auction = lockActive(id);
        checkSellerWithoutBids(username, auction);

        auction.setStartPrice(startPrice);
        auction.setCurrentPrice(startPrice);
        auction.setEndsAt(Instant.now().plus(Duration.ofHours(hours)));
        return AuctionDto.from(auction);
    }

    /** Annulation possible tant qu'aucune mise n'a été posée : la carte retourne au vendeur. */
    @Transactional
    public void cancel(String username, Long id) {
        Auction auction = lockActive(id);
        checkSellerWithoutBids(username, auction);

        auction.getSeller().getCards().add(auction.getCard());
        auction.setStatus(Auction.Status.CANCELLED);
    }

    // ---------- Mises ----------

    @Transactional
    public AuctionDto bid(String username, Long id, int amount) {
        Auction auction = lockActive(id);
        Instant now = Instant.now();
        if (!auction.getEndsAt().isAfter(now)) {
            throw new AuctionException(HttpStatus.CONFLICT, "Cette enchère est terminée");
        }

        User bidder = findUser(username);
        if (auction.getSeller().getIdUser().equals(bidder.getIdUser())) {
            throw new AuctionException(HttpStatus.FORBIDDEN, "Tu ne peux pas miser sur ta propre carte");
        }

        boolean firstBid = auction.getHighestBidder() == null;
        int minimum = firstBid ? auction.getStartPrice() : auction.getCurrentPrice() + 1;
        if (amount < minimum) {
            throw new AuctionException(HttpStatus.BAD_REQUEST, "La mise minimale est de " + minimum + " TubeCoin(s)");
        }

        User previous = auction.getHighestBidder();
        boolean sameBidder = previous != null && previous.getIdUser().equals(bidder.getIdUser());
        // Les coins de la meilleure mise précédente sont déjà bloqués : un même joueur ne paie que la différence
        int toPay = sameBidder ? amount - auction.getCurrentPrice() : amount;
        if (toPay > bidder.getTubecoin()) {
            throw new AuctionException(HttpStatus.BAD_REQUEST, "Tu n'as pas assez de TubeCoins");
        }

        if (previous != null && !sameBidder) {
            previous.setTubecoin(previous.getTubecoin() + auction.getCurrentPrice());
        }
        bidder.setTubecoin(bidder.getTubecoin() - toPay);

        auction.setCurrentPrice(amount);
        auction.setHighestBidder(bidder);

        Bid bid = new Bid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        auction.getBids().add(bid);

        // Anti-sniping : mise dans la dernière minute → le compteur remonte à 1 minute
        if (Duration.between(now, auction.getEndsAt()).compareTo(SNIPING_WINDOW) < 0) {
            auction.setEndsAt(now.plus(SNIPING_WINDOW));
        }
        return AuctionDto.from(auction);
    }

    // ---------- Clôture ----------

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void closeExpired() {
        List<Auction> expired =
            auctionRepository.findByStatusAndEndsAtBefore(Auction.Status.ACTIVE, Instant.now());
        for (Auction candidate : expired) {
            // Relecture sous verrou : une mise de dernière seconde a pu repousser la fin
            auctionRepository.findByIdForUpdate(candidate.getId())
                .filter(a -> a.getStatus() == Auction.Status.ACTIVE && !a.getEndsAt().isAfter(Instant.now()))
                .ifPresent(this::close);
        }
    }

    private void close(Auction auction) {
        User winner = auction.getHighestBidder();
        if (winner == null) {
            auction.getSeller().getCards().add(auction.getCard());
            auction.setStatus(Auction.Status.EXPIRED);
        } else {
            winner.getCards().add(auction.getCard());
            User seller = auction.getSeller();
            seller.setTubecoin(seller.getTubecoin() + auction.getCurrentPrice());
            auction.setStatus(Auction.Status.SOLD);
        }
        log.info("Enchère {} clôturée : {}", auction.getId(), auction.getStatus());
    }

    // ---------- Utilitaires ----------

    private void checkParams(int startPrice, int hours) {
        if (startPrice < 1) {
            throw new AuctionException(HttpStatus.BAD_REQUEST, "Le prix de départ est de 1 TubeCoin minimum");
        }
        if (!ALLOWED_HOURS.contains(hours)) {
            throw new AuctionException(HttpStatus.BAD_REQUEST, "Durée autorisée : 1, 3, 6, 12 ou 24 heures");
        }
    }

    private void checkSellerWithoutBids(String username, Auction auction) {
        if (!auction.getSeller().getUsername().equals(username)) {
            throw new AuctionException(HttpStatus.FORBIDDEN, "Cette enchère ne t'appartient pas");
        }
        if (auction.getHighestBidder() != null) {
            throw new AuctionException(HttpStatus.CONFLICT, "Une mise a déjà été posée");
        }
    }

    private Auction lockActive(Long id) {
        Auction auction = auctionRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new AuctionException(HttpStatus.NOT_FOUND, "Enchère introuvable"));
        if (auction.getStatus() != Auction.Status.ACTIVE) {
            throw new AuctionException(HttpStatus.CONFLICT, "Cette enchère est terminée");
        }
        return auction;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new AuctionException(HttpStatus.UNAUTHORIZED, "Utilisateur inconnu"));
    }

    /** Retire une seule occurrence de la carte (la collection peut contenir des doublons). */
    private boolean removeOneCard(User user, Long cardId) {
        Iterator<Card> it = user.getCards().iterator();
        while (it.hasNext()) {
            if (cardId.equals(it.next().getId())) {
                it.remove();
                return true;
            }
        }
        return false;
    }
}
