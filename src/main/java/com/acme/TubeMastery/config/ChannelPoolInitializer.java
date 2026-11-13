package com.acme.TubeMastery.config;

import com.acme.TubeMastery.repository.ChannelPoolRepository;
import com.acme.TubeMastery.service.ChannelSeedService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Au démarrage, si le vivier est vide, l'importe depuis Wikidata (en arrière-plan). */
@Component
public class ChannelPoolInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ChannelPoolInitializer.class);

    private final ChannelPoolRepository poolRepository;
    private final ChannelSeedService seedService;
    private final boolean enabled;

    public ChannelPoolInitializer(
            ChannelPoolRepository poolRepository,
            ChannelSeedService seedService,
            @Value("${pool.seed.enabled:true}") boolean enabled) {
        this.poolRepository = poolRepository;
        this.seedService = seedService;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) {
        if (!enabled || poolRepository.count() > 0) {
            return;
        }
        Thread.ofVirtual().name("pool-seed").start(() -> {
            try {
                seedService.seedFromWikidata();
            } catch (Exception e) {
                log.error("Import Wikidata impossible : {}", e.getMessage());
            }
        });
    }
}
