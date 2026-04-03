package com.medihelp.prescription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.medihelp.prescription.dto.DrugInteractionResponse;
import com.medihelp.prescription.entity.DrugInteractionCache;
import com.medihelp.prescription.repository.DrugInteractionCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrugInteractionService {

    private final DrugInteractionCacheRepository cacheRepository;
    private static final String OPENFDA_URL = "https://api.fda.gov/drug/label.json?search=drug_interactions:\"%s\"+AND+openfda.generic_name:\"%s\"&limit=1";

    public List<DrugInteractionResponse> checkInteractions(List<String> drugNames) {
        List<DrugInteractionResponse> interactions = new ArrayList<>();

        for (int i = 0; i < drugNames.size(); i++) {
            for (int j = i + 1; j < drugNames.size(); j++) {
                String drug1 = drugNames.get(i).toLowerCase().trim();
                String drug2 = drugNames.get(j).toLowerCase().trim();

                // Check cache first (both orderings)
                Optional<DrugInteractionCache> cached = cacheRepository.findByDrug1AndDrug2(drug1, drug2)
                        .or(() -> cacheRepository.findByDrug1AndDrug2(drug2, drug1));

                if (cached.isPresent()) {
                    DrugInteractionCache c = cached.get();
                    interactions.add(DrugInteractionResponse.builder()
                            .drug1(c.getDrug1()).drug2(c.getDrug2())
                            .severity(c.getSeverity()).description(c.getDescription())
                            .source(c.getSource()).build());
                } else {
                    // Query OpenFDA for uncached pairs
                    checkOpenFda(drug1, drug2).ifPresent(interactions::add);
                }
            }
        }
        return interactions;
    }

    private Optional<DrugInteractionResponse> checkOpenFda(String drug1, String drug2) {
        try {
            RestTemplate rest = new RestTemplate();
            String url = String.format(OPENFDA_URL, drug2, drug1);
            JsonNode response = rest.getForObject(url, JsonNode.class);

            if (response != null && response.has("results") && response.get("results").size() > 0) {
                JsonNode result = response.get("results").get(0);
                String interactionText = "";
                if (result.has("drug_interactions")) {
                    JsonNode interactions = result.get("drug_interactions");
                    interactionText = interactions.isArray() && interactions.size() > 0
                            ? interactions.get(0).asText().substring(0, Math.min(500, interactions.get(0).asText().length()))
                            : "Interaction found between " + drug1 + " and " + drug2;
                }

                // Cache the result
                DrugInteractionCache cache = DrugInteractionCache.builder()
                        .drug1(drug1).drug2(drug2)
                        .severity("MODERATE")
                        .description(interactionText)
                        .source("OpenFDA")
                        .lastCheckedAt(Instant.now())
                        .build();
                cacheRepository.save(cache);

                log.info("OpenFDA interaction found: {} <-> {}", drug1, drug2);
                return Optional.of(DrugInteractionResponse.builder()
                        .drug1(drug1).drug2(drug2).severity("MODERATE")
                        .description(interactionText).source("OpenFDA").build());
            }
        } catch (Exception e) {
            log.debug("No OpenFDA interaction found for {} <-> {}: {}", drug1, drug2, e.getMessage());
        }
        return Optional.empty();
    }
}
