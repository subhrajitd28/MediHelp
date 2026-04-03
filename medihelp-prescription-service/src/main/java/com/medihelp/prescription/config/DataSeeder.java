package com.medihelp.prescription.config;

import com.medihelp.prescription.entity.DrugInteractionCache;
import com.medihelp.prescription.repository.DrugInteractionCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final DrugInteractionCacheRepository cacheRepository;

    @Override
    public void run(String... args) {
        if (cacheRepository.count() > 0) {
            log.info("Drug interaction cache already seeded, skipping...");
            return;
        }

        log.info("Seeding drug interaction cache with common interactions...");

        List<DrugInteractionCache> interactions = List.of(
                DrugInteractionCache.builder()
                        .drug1("aspirin")
                        .drug2("warfarin")
                        .severity("SEVERE")
                        .description("Aspirin increases the anticoagulant effect of warfarin, significantly raising the risk of bleeding.")
                        .source("OpenFDA")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("ibuprofen")
                        .drug2("warfarin")
                        .severity("SEVERE")
                        .description("Ibuprofen may increase the anticoagulant effect of warfarin and increase risk of gastrointestinal bleeding.")
                        .source("OpenFDA")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("lisinopril")
                        .drug2("potassium")
                        .severity("MODERATE")
                        .description("ACE inhibitors like lisinopril can increase potassium levels, which may lead to hyperkalemia when combined with potassium supplements.")
                        .source("RxNorm")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("metformin")
                        .drug2("alcohol")
                        .severity("MODERATE")
                        .description("Alcohol can increase the risk of lactic acidosis when taken with metformin and may cause hypoglycemia.")
                        .source("OpenFDA")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("amoxicillin")
                        .drug2("methotrexate")
                        .severity("SEVERE")
                        .description("Amoxicillin may reduce the renal clearance of methotrexate, increasing the risk of methotrexate toxicity.")
                        .source("RxNorm")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("ciprofloxacin")
                        .drug2("theophylline")
                        .severity("SEVERE")
                        .description("Ciprofloxacin inhibits the metabolism of theophylline, which can lead to theophylline toxicity.")
                        .source("OpenFDA")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("fluoxetine")
                        .drug2("tramadol")
                        .severity("CONTRAINDICATED")
                        .description("Concurrent use increases the risk of serotonin syndrome, seizures, and respiratory depression.")
                        .source("OpenFDA")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("atorvastatin")
                        .drug2("grapefruit")
                        .severity("MODERATE")
                        .description("Grapefruit can increase atorvastatin blood levels, raising the risk of muscle-related side effects including rhabdomyolysis.")
                        .source("RxNorm")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("omeprazole")
                        .drug2("clopidogrel")
                        .severity("SEVERE")
                        .description("Omeprazole can reduce the antiplatelet effect of clopidogrel by inhibiting CYP2C19-mediated activation.")
                        .source("OpenFDA")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("digoxin")
                        .drug2("amiodarone")
                        .severity("SEVERE")
                        .description("Amiodarone can increase digoxin levels by 70-100%, leading to potential digoxin toxicity.")
                        .source("RxNorm")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("acetaminophen")
                        .drug2("warfarin")
                        .severity("MODERATE")
                        .description("Regular use of acetaminophen may increase the anticoagulant effect of warfarin, particularly at doses exceeding 2g/day.")
                        .source("OpenFDA")
                        .lastCheckedAt(Instant.now())
                        .build(),
                DrugInteractionCache.builder()
                        .drug1("aspirin")
                        .drug2("ibuprofen")
                        .severity("MODERATE")
                        .description("Ibuprofen may interfere with the antiplatelet effect of low-dose aspirin and increase the risk of GI bleeding.")
                        .source("OpenFDA")
                        .lastCheckedAt(Instant.now())
                        .build()
        );

        cacheRepository.saveAll(interactions);
        log.info("Seeded {} drug interactions into cache", interactions.size());
    }
}
