package com.acme.TubeMastery.service;

import com.acme.TubeMastery.model.ChannelPoolEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * Remplit en tâche de fond le cache de cartes (fiches déjà scrapées), pour que l'ouverture
 * d'un paquet n'ait plus à lancer Chrome. S'arrête quand la taille cible est atteinte.
 */
@Component
public class CardCacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(CardCacheWarmer.class);

    private final ChannelPoolService poolService;
    private final int targetSize;
    private final int parallelism;

    public CardCacheWarmer(
            ChannelPoolService poolService,
            @Value("${pool.cache.target-size:300}") int targetSize,
            @Value("${pool.cache.parallelism:2}") int parallelism) {
        this.poolService = poolService;
        this.targetSize = targetSize;
        this.parallelism = Math.max(1, parallelism);
    }

    /** fixedDelay : jamais deux passes en même temps. */
    @Scheduled(initialDelayString = "${pool.cache.initial-delay-ms:15000}",
               fixedDelayString = "${pool.cache.delay-ms:3000}")
    public void warm() {
        if (poolService.cachedCount() >= targetSize) {
            return;
        }
        List<ChannelPoolEntry> batch = IntStream.range(0, parallelism)
            .mapToObj(i -> poolService.pickEntry(ChannelPoolEntry.Status.NEW))
            .flatMap(Optional::stream)
            .distinct()
            .toList();
        if (batch.isEmpty()) {
            return;
        }
        List<CompletableFuture<Optional<Card>>> futures = batch.stream()
            .map(entry -> CompletableFuture.supplyAsync(() -> poolService.scrapeAndCache(entry)))
            .toList();
        long ok = futures.stream().map(CompletableFuture::join).filter(Optional::isPresent).count();
        log.info("Cache de cartes : +{} (total {}/{})", ok, poolService.cachedCount(), targetSize);
    }
}
