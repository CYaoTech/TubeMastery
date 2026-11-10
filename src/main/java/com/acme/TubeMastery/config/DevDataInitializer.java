package com.acme.TubeMastery.config;

import com.acme.TubeMastery.repository.CardRepository;
import com.acme.TubeMastery.repository.UserRepository;
import com.acme.TubeMastery.service.Card;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Données de test (profil dev uniquement) : BerretMan possède 20 cartes Cyprien
 * (pour tester le layout de la collection).
 * Les infos sont écrites en dur pour ne pas dépendre de l'API YouTube.
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private static final int TEST_CARD_COUNT = 20;

    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public DevDataInitializer(UserRepository userRepository, CardRepository cardRepository) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findByUsername("BerretMan").ifPresent(user -> {
            long cyprienCount = user.getCards().stream()
                .filter(card -> "cyprien".equals(card.getHandle()))
                .count();
            if (cyprienCount >= TEST_CARD_COUNT) {
                return;
            }
            Card cyprien = cardRepository.findByHandle("cyprien").orElseGet(() -> {
                Card card = new Card();
                card.setHandle("cyprien");
                card.setName("Cyprien");
                card.setDescription("Je fais des vidéos, en plus tu peux même les regarder ! La nature fait bien les choses...");
                card.setThumbnailUrl("https://yt3.ggpht.com/ytc/AIdro_kKiE1Vpd2RZMv057AzKdHBtqkL7ksZhZ4Huwfbr9ngUyU=s800-c-k-c0x00ffffff-no-rj");
                card.setSubscriberCount(14_500_000L);
                return cardRepository.save(card);
            });
            for (long i = cyprienCount; i < TEST_CARD_COUNT; i++) {
                user.getCards().add(cyprien);
            }
            userRepository.save(user);
            log.info("BerretMan a maintenant {} cartes de test Cyprien", TEST_CARD_COUNT);
        });
    }
}
