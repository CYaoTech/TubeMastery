package com.acme.TubeMastery.service;

import com.acme.TubeMastery.model.ChannelPoolEntry;
import com.acme.TubeMastery.repository.CardRepository;
import com.acme.TubeMastery.repository.ChannelPoolRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tirage d'une carte dans le vivier de chaînes : on tire d'abord une rareté (pondérée),
 * puis une chaîne de cette rareté. La fiche n'est scrapée et stockée que la première fois qu'elle sort dans un paquet.
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
    /** Les scrapings sont du pur I/O : un thread virtuel par fiche. */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

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

    /**
     * Tire {@code count} cartes distinctes. Une chaîne déjà passée dans un paquet est lue en base ;
     * les autres sont scrapées ici, en parallèle, puis stockées (premier tirage uniquement).
     */
    public List<Card> drawCards(int count) {
        List<Card> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int attempt = 0; attempt < MAX_ATTEMPTS && result.size() < count; attempt++) {
            List<ChannelPoolEntry> entries = new ArrayList<>();
            for (int i = result.size(); i < count; i++) {
                Optional<ChannelPoolEntry> entry = pickEntry();
                if (entry.isEmpty()) {
                    throw new IllegalStateException("Le vivier de chaînes est vide");
                }
                if (seen.add(entry.get().getHandle())) {
                    entries.add(entry.get());
                }
            }
            List<CompletableFuture<Optional<Card>>> futures = entries.stream()
                .map(e -> CompletableFuture.supplyAsync(() -> resolve(e), executor))
                .toList();
            futures.stream().map(CompletableFuture::join).flatMap(Optional::stream).forEach(result::add);
        }
        if (result.size() < count) {
            throw new IllegalStateException("Pas assez de chaînes utilisables trouvées après " + MAX_ATTEMPTS + " essais");
        }
        return result;
    }

    public Card drawCard() {
        return drawCards(1).get(0);
    }

    /** Carte déjà stockée si la chaîne a déjà été tirée, sinon scraping et stockage. */
    private Optional<Card> resolve(ChannelPoolEntry entry) {
        if (entry.getStatus() == ChannelPoolEntry.Status.OK) {
            Optional<Card> stored = cardRepository.findByHandle(entry.getHandle().toLowerCase(Locale.ROOT));
            if (stored.isPresent()) {
                return stored;
            }
        }
        return scrapeAndCache(entry);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
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

    /** Chaîne vivante au hasard, rareté tirée au sort (repli sur n'importe quelle chaîne si la tranche est vide). */
    public Optional<ChannelPoolEntry> pickEntry() {
        int roll = ThreadLocalRandom.current().nextInt(1000);
        int tier = 0;
        for (int acc = WEIGHTS[0]; roll >= acc && tier < WEIGHTS.length - 1; acc += WEIGHTS[++tier]) {
            // avance jusqu'à la tranche tirée
        }
        return poolRepository.findRandomAliveInRange(RANGES[tier][0], RANGES[tier][1])
            .or(() -> poolRepository.findRandomAlive());
    }
}
