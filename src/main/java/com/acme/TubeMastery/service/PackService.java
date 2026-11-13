package com.acme.TubeMastery.service;

import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.repository.CardRepository;
import com.acme.TubeMastery.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PackService {

    public static final Duration PACK_INTERVAL = Duration.ofMinutes(10);
    public static final int CARDS_PER_PACK = 4;

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final CollectionService collectionService;
    private final YoutubeServiceFinal youtubeService;
    private final ChannelPoolService poolService;

    public PackService(
            UserRepository userRepository,
            CardRepository cardRepository,
            CollectionService collectionService,
            YoutubeServiceFinal youtubeService,
            ChannelPoolService poolService) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.collectionService = collectionService;
        this.youtubeService = youtubeService;
        this.poolService = poolService;
    }

    public record PackStatus(int packs, long secondsUntilNext) {}

    private void accrue(User user) {
        Instant now = Instant.now();
        if (user.getPacks() == null || user.getPacksUpdatedAt() == null) {
            user.setPacks(1);
            user.setPacksUpdatedAt(now);
            return;
        }
        long gained = Duration.between(user.getPacksUpdatedAt(), now).dividedBy(PACK_INTERVAL);
        if (gained > 0) {
            user.setPacks(user.getPacks() + (int) gained);
            user.setPacksUpdatedAt(user.getPacksUpdatedAt().plus(PACK_INTERVAL.multipliedBy(gained)));
        }
    }

    @Transactional
    public PackStatus getStatus(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        accrue(user);
        long remaining = Duration.between(
            Instant.now(), user.getPacksUpdatedAt().plus(PACK_INTERVAL)).toSeconds();
        return new PackStatus(user.getPacks(), Math.max(remaining, 0));
    }

    @Transactional
    public Optional<List<Card>> openPack(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        accrue(user);
        if (user.getPacks() <= 0) {
            return Optional.empty();
        }

        // Vivier Wikidata si déjà importé, sinon liste de départ (import en cours au démarrage)
        boolean usePool = poolService.size() > 0;
        List<String> handles = collectionService.handles();
        List<Card> opened = new ArrayList<>();
        for (int i = 0; i < CARDS_PER_PACK; i++) {
            if (usePool) {
                opened.add(poolService.drawCard());
            } else {
                String handle = handles.get(ThreadLocalRandom.current().nextInt(handles.size()));
                opened.add(cardRepository.findByHandle(handle)
                    .orElseGet(() -> cardRepository.save(youtubeService.getYoutuber(handle))));
            }
        }

        user.setPacks(user.getPacks() - 1);
        user.getCards().addAll(opened);
        return Optional.of(opened);
    }
}
