package com.acme.TubeMastery.service;

import com.acme.TubeMastery.model.ChannelPoolEntry;
import com.acme.TubeMastery.repository.CardRepository;
import com.acme.TubeMastery.repository.ChannelPoolRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tirage d'une carte dans le vivier de chaînes : on tire d'abord une rareté (pondérée),
 * puis une chaîne de cette rareté. La fiche n'est scrapée que la première fois.
 */
@Service
public class ChannelPoolService {

    private static final Logger log = LoggerFactory.getLogger(ChannelPoolService.class);
    private static final int MAX_ATTEMPTS = 8;

    /** Bornes d'abonnés [min, max[ par rareté, dans l'ordre B, C, R, UR, L, M. */
    private static final long[][] RANGES = {
        {0, 10_000L}, {10_000L, 100_000L}, {100_000L, 1_000_000L},
        {1_000_000L, 10_000_000L}, {10_000_000L, 100_000_000L}, {100_000_000L, Long.MAX_VALUE}};
    /** Poids en ‰ : B 50 %, C 28 %, R 14 %, UR 6 %, L 1,8 %, M 0,2 %. */
    private static final int[] WEIGHTS = {500, 280, 140, 60, 18, 2};

    private final ChannelPoolRepository poolRepository;
    private final CardRepository cardRepository;
    private final YoutubeServiceFinal youtubeService;

    public ChannelPoolService(
            ChannelPoolRepository poolRepository,
            CardRepository cardRepository,
            YoutubeServiceFinal youtubeService) {
        this.poolRepository = poolRepository;
        this.cardRepository = cardRepository;
        this.youtubeService = youtubeService;
    }

    public long size() {
        return poolRepository.countByStatusNot(ChannelPoolEntry.Status.DEAD);
    }

    public long cachedCount() {
        return poolRepository.countByStatus(ChannelPoolEntry.Status.OK);
    }

    /**
     * Tire une carte. On privilégie les cartes déjà en cache (instantané) ; le scraping
     * à la demande n'a lieu que si le cache est vide (les chaînes introuvables sont marquées DEAD).
     */
    public Card drawCard() {
        Optional<Card> cached = drawCached();
        if (cached.isPresent()) {
            return cached.get();
        }
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            ChannelPoolEntry entry = pickEntry(ChannelPoolEntry.Status.NEW)
                .orElseThrow(() -> new IllegalStateException("Le vivier de chaînes est vide"));
            Optional<Card> card = scrapeAndCache(entry);
            if (card.isPresent()) {
                return card.get();
            }
        }
        throw new IllegalStateException("Aucune chaîne utilisable trouvée après " + MAX_ATTEMPTS + " essais");
    }

    private Optional<Card> drawCached() {
        for (int attempt = 0; attempt < 3; attempt++) {
            Optional<Card> card = pickEntry(ChannelPoolEntry.Status.OK)
                .flatMap(e -> cardRepository.findByHandle(e.getHandle().toLowerCase(Locale.ROOT)));
            if (card.isPresent()) {
                return card;
            }
        }
        return Optional.empty();
    }

    /** Scrape la fiche d'une chaîne du vivier et la met en cache ; vide (et DEAD) si introuvable. */
    public Optional<Card> scrapeAndCache(ChannelPoolEntry entry) {
        Card scraped;
        try {
            scraped = youtubeService.getYoutuber(entry.getHandle());
        } catch (RuntimeException e) {
            log.warn("Chaîne @{} inutilisable, marquée DEAD : {}", entry.getHandle(), e.getMessage());
            entry.setStatus(ChannelPoolEntry.Status.DEAD);
            poolRepository.save(entry);
            return Optional.empty();
        }
        Card card;
        try {
            card = cardRepository.save(scraped);
        } catch (RuntimeException e) { // déjà mis en cache en parallèle
            card = cardRepository.findByHandle(scraped.getHandle()).orElse(null);
            if (card == null) {
                return Optional.empty();
            }
        }
        entry.setStatus(ChannelPoolEntry.Status.OK);
        entry.setSubscriberCount(card.getSubscriberCount());
        poolRepository.save(entry);
        return Optional.of(card);
    }

    /** Chaîne du statut donné, rareté tirée au sort (repli sur n'importe quelle chaîne de ce statut). */
    public Optional<ChannelPoolEntry> pickEntry(ChannelPoolEntry.Status status) {
        int roll = ThreadLocalRandom.current().nextInt(1000);
        int tier = 0;
        for (int acc = WEIGHTS[0]; roll >= acc && tier < WEIGHTS.length - 1; acc += WEIGHTS[++tier]) {
            // avance jusqu'à la tranche tirée
        }
        return poolRepository.findRandomInRange(status.name(), RANGES[tier][0], RANGES[tier][1])
            .or(() -> poolRepository.findRandomByStatus(status.name()));
    }
}
