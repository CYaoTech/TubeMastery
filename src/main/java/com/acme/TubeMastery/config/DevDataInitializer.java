package com.acme.TubeMastery.config;

import com.acme.TubeMastery.model.User;
import com.acme.TubeMastery.repository.CardRepository;
import com.acme.TubeMastery.repository.UserRepository;
import com.acme.TubeMastery.service.Card;
import com.acme.TubeMastery.service.YoutubeServiceFinal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Données de test (profil dev uniquement) : BerretMan possède le nombre de cartes
 * indiqué ci-dessous pour chaque youtubeur (pour tester le layout de la collection).
 * Les infos des cartes sont récupérées par scraping YouTube la première fois,
 * puis réutilisées depuis la base.
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    /** Handle YouTube -> nombre de cartes de test. */
    private static final Map<String, Integer> TEST_CARDS = new LinkedHashMap<>();
    static {
        TEST_CARDS.put("cyprien", 5);
        TEST_CARDS.put("mrbeast", 5);
        TEST_CARDS.put("inoxtag", 5);
        TEST_CARDS.put("squeezie", 5);
        TEST_CARDS.put("michou", 5);
        TEST_CARDS.put("NormanFaitDesVideos", 5);
    }

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final YoutubeServiceFinal youtubeService;

    public DevDataInitializer(
            UserRepository userRepository,
            CardRepository cardRepository,
            YoutubeServiceFinal youtubeService) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.youtubeService = youtubeService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findByUsername("BerretMan").ifPresent(user -> {
            TEST_CARDS.forEach((handle, copies) -> {
                try {
                    setCopies(user, handle, copies);
                } catch (RuntimeException e) {
                    log.warn("Cartes de test @{} non ajoutées : {}", handle, e.getMessage());
                }
            });
            userRepository.save(user);
            log.info("Cartes de test de BerretMan : {}", TEST_CARDS);
        });
    }

    /** Ajuste la collection pour qu'elle contienne exactement `copies` cartes de ce youtubeur. */
    private void setCopies(User user, String handle, int copies) {
        Card card = cardRepository.findByHandle(handle)
            .orElseGet(() -> cardRepository.save(youtubeService.getYoutuber(handle)));

        long count = user.getCards().stream()
            .filter(c -> handle.equals(c.getHandle()))
            .count();
        for (long i = count; i < copies; i++) {
            user.getCards().add(card);
        }
        for (long i = count; i > copies; i--) {
            user.getCards().remove(card);
        }
    }
}
