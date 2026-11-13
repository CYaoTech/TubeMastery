package com.acme.TubeMastery.service;

import com.acme.TubeMastery.model.ChannelPoolEntry;
import com.acme.TubeMastery.repository.ChannelPoolRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Remplit le vivier avec les chaînes YouTube référencées sur Wikidata (propriété P11245, abonnés via P8687). */
@Service
public class ChannelSeedService {

    private static final Logger log = LoggerFactory.getLogger(ChannelSeedService.class);
    private static final String SPARQL = """
        SELECT ?handle (MAX(?n) AS ?subs) WHERE {
          ?item wdt:P11245 ?handle .
          OPTIONAL { ?item p:P8687 ?st . ?st ps:P8687 ?n . ?st pq:P2397 ?c . }
        } GROUP BY ?handle""";

    private final ChannelPoolRepository poolRepository;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public ChannelSeedService(ChannelPoolRepository poolRepository) {
        this.poolRepository = poolRepository;
    }

    /** Importe les chaînes absentes du vivier ; renvoie le nombre d'ajouts. */
    public int seedFromWikidata() throws IOException, InterruptedException {
        String url = "https://query.wikidata.org/sparql?query=" + URLEncoder.encode(SPARQL, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofMinutes(5))
            .header("Accept", "text/csv")
            .header("User-Agent", "TubeMastery/0.1 (projet étudiant)")
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Wikidata a répondu " + response.statusCode());
        }

        List<ChannelPoolEntry> toSave = new ArrayList<>();
        String[] lines = response.body().split("\r?\n");
        for (int i = 1; i < lines.length; i++) { // ligne 0 = en-tête
            int comma = lines[i].lastIndexOf(',');
            if (comma <= 0) continue;
            String handle = lines[i].substring(0, comma).strip();
            if (!handle.matches("[A-Za-z0-9._-]{3,30}") || poolRepository.existsById(handle)) continue;

            ChannelPoolEntry entry = new ChannelPoolEntry();
            entry.setHandle(handle);
            entry.setSource("wikidata");
            String subs = lines[i].substring(comma + 1).strip();
            if (!subs.isEmpty()) {
                try {
                    entry.setSubscriberCount((long) Double.parseDouble(subs));
                } catch (NumberFormatException ignored) {
                    // abonnés inconnus
                }
            }
            toSave.add(entry);
        }
        poolRepository.saveAll(toSave);
        log.info("Vivier : {} chaînes importées depuis Wikidata", toSave.size());
        return toSave.size();
    }
}
