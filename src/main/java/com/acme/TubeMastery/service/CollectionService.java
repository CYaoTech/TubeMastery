package com.acme.TubeMastery.service;

import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.repository.CardRepository;
import com.acme.TubeMastery.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CollectionService {

    /** Youtubeurs que l'on peut obtenir (handles YouTube, sans le @). */
    private static final List<String> HANDLES = List.of(
        "mrbeast", "cyprien", "squeezie", "pewdiepie", "markiplier", "inoxtag",
        "amixem", "michou", "veritasium", "mkbhd", "kurzgesagt", "joueurdugrenier"
    );

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final YoutubeServiceFinal youtubeService;

    public CollectionService(
            UserRepository userRepository,
            CardRepository cardRepository,
            YoutubeServiceFinal youtubeService) {

        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.youtubeService = youtubeService;
    }

    @Transactional(readOnly = true)
    public User getUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.getCards().sort(Comparator.comparing(
            Card::getSubscriberCount,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return user;
    }

    /**
     * Ajoute à la collection du joueur une carte tirée au hasard parmi celles
     * qu'il ne possède pas encore. Vide si la collection est déjà complète.
     */
    @Transactional
    public Optional<Card> drawCard(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();

        List<String> missing = new ArrayList<>(HANDLES);
        user.getCards().forEach(card -> missing.remove(card.getHandle()));
        if (missing.isEmpty()) {
            return Optional.empty();
        }

        String handle = missing.get(ThreadLocalRandom.current().nextInt(missing.size()));

        // L'API YouTube n'est appelée que la première fois qu'une carte est tirée
        Card card = cardRepository.findByHandle(handle)
            .orElseGet(() -> cardRepository.save(youtubeService.getYoutuber(handle)));

        user.getCards().add(card);
        return Optional.of(card);
    }

    public int totalCards() {
        return HANDLES.size();
    }
}
