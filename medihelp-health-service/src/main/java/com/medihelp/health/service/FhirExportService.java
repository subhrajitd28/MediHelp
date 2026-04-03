package com.medihelp.health.service;

import ca.uhn.fhir.context.FhirContext;
import com.medihelp.health.entity.Vital;
import com.medihelp.health.repository.VitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirExportService {

    private final VitalRepository vitalRepository;
    private final FhirContext fhirContext = FhirContext.forR4();

    private static final Map<String, String> LOINC_CODES = Map.of(
            "HEART_RATE", "8867-4",
            "BLOOD_PRESSURE_SYSTOLIC", "8480-6",
            "BLOOD_PRESSURE_DIASTOLIC", "8462-4",
            "BLOOD_SUGAR", "2339-0",
            "TEMPERATURE", "8310-5",
            "OXYGEN_SATURATION", "2708-6",
            "WEIGHT", "29463-7",
            "HEIGHT", "8302-2",
            "STEPS", "55423-8",
            "SLEEP_HOURS", "93832-4"
    );

    private static final Map<String, String> LOINC_DISPLAY = Map.of(
            "HEART_RATE", "Heart rate",
            "BLOOD_PRESSURE_SYSTOLIC", "Systolic blood pressure",
            "BLOOD_PRESSURE_DIASTOLIC", "Diastolic blood pressure",
            "BLOOD_SUGAR", "Glucose [Mass/volume] in Blood",
            "TEMPERATURE", "Body temperature",
            "OXYGEN_SATURATION", "Oxygen saturation in Arterial blood",
            "WEIGHT", "Body weight",
            "HEIGHT", "Body height"
    );

    public String exportAsBundle(UUID userId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTimestamp(new Date());
        bundle.setId(UUID.randomUUID().toString());

        // Add Patient resource
        Patient patient = new Patient();
        patient.setId(userId.toString());
        patient.addIdentifier()
                .setSystem("urn:medihelp:user")
                .setValue(userId.toString());
        bundle.addEntry()
                .setFullUrl("urn:uuid:" + userId)
                .setResource(patient);

        // Add Observation resources for each vital
        List<Vital> vitals = vitalRepository.findByUserIdOrderByRecordedAtDesc(userId, PageRequest.of(0, 100)).getContent();

        for (Vital vital : vitals) {
            Observation obs = createObservation(vital, userId);
            bundle.addEntry()
                    .setFullUrl("urn:uuid:" + vital.getId())
                    .setResource(obs);
        }

        log.info("FHIR export for user {}: {} resources in bundle", userId, bundle.getEntry().size());
        return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
    }

    private Observation createObservation(Vital vital, UUID userId) {
        Observation obs = new Observation();
        obs.setId(vital.getId().toString());
        obs.setStatus(Observation.ObservationStatus.FINAL);

        // Category: vital-signs
        obs.addCategory()
                .addCoding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("Vital Signs");

        // LOINC code
        String loincCode = LOINC_CODES.getOrDefault(vital.getType(), "unknown");
        String loincDisplay = LOINC_DISPLAY.getOrDefault(vital.getType(), vital.getType());
        obs.getCode()
                .addCoding()
                .setSystem("http://loinc.org")
                .setCode(loincCode)
                .setDisplay(loincDisplay);

        // Value
        obs.setValue(new Quantity()
                .setValue(BigDecimal.valueOf(vital.getValue()))
                .setUnit(vital.getUnit())
                .setSystem("http://unitsofmeasure.org"));

        // Effective date
        obs.setEffective(new DateTimeType(Date.from(vital.getRecordedAt())));

        // Subject reference
        obs.setSubject(new Reference("Patient/" + userId));

        return obs;
    }
}
