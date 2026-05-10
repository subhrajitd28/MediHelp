package com.medihelp.health.service;

import ca.uhn.fhir.context.FhirContext;
import com.medihelp.health.document.MoodEntry;
import com.medihelp.health.entity.HealthRecord;
import com.medihelp.health.entity.Vital;
import com.medihelp.health.repository.HealthRecordRepository;
import com.medihelp.health.repository.MoodEntryRepository;
import com.medihelp.health.repository.VitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Builds an HL7 FHIR R4 Bundle that any compliant clinical system (Epic,
 * Cerner, OpenMRS, etc.) can ingest. Aggregates everything health-service
 * holds for the user:
 *
 *   • Patient                 — anchor identity
 *   • Observation (vitals)    — heart rate, BP, glucose, etc. with LOINC codes
 *   • Observation (mood)      — self-reported mental state (LOINC 75275-8)
 *   • DocumentReference       — uploaded test reports, doctor notes, scans
 *
 * Cross-service resources (MedicationStatement from prescription-service,
 * AllergyIntolerance from user-service) are NOT included here — adding them
 * would require service-to-service REST calls, which the project's
 * architecture explicitly forbids in favour of gateway-only sync paths and
 * RabbitMQ events. A federated FHIR aggregator is the production answer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FhirExportService {

    private final VitalRepository vitalRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final FhirContext fhirContext = FhirContext.forR4();

    private static final String LOINC_SYSTEM    = "http://loinc.org";
    private static final String UCUM_SYSTEM     = "http://unitsofmeasure.org";
    private static final String CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";

    private static final Map<String, String> LOINC_CODES = Map.of(
            "HEART_RATE",                "8867-4",
            "BLOOD_PRESSURE_SYSTOLIC",   "8480-6",
            "BLOOD_PRESSURE_DIASTOLIC",  "8462-4",
            "BLOOD_SUGAR",               "2339-0",
            "TEMPERATURE",               "8310-5",
            "OXYGEN_SATURATION",         "2708-6",
            "WEIGHT",                    "29463-7",
            "HEIGHT",                    "8302-2",
            "STEPS",                     "55423-8",
            "SLEEP_HOURS",               "93832-4"
    );

    private static final Map<String, String> LOINC_DISPLAY = Map.of(
            "HEART_RATE",               "Heart rate",
            "BLOOD_PRESSURE_SYSTOLIC",  "Systolic blood pressure",
            "BLOOD_PRESSURE_DIASTOLIC", "Diastolic blood pressure",
            "BLOOD_SUGAR",              "Glucose [Mass/volume] in Blood",
            "TEMPERATURE",              "Body temperature",
            "OXYGEN_SATURATION",        "Oxygen saturation in Arterial blood",
            "WEIGHT",                   "Body weight",
            "HEIGHT",                   "Body height"
    );

    public String exportAsBundle(UUID userId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTimestamp(new Date());
        bundle.setId(UUID.randomUUID().toString());

        Reference patientRef = new Reference("Patient/" + userId);

        // Patient
        Patient patient = new Patient();
        patient.setId(userId.toString());
        patient.addIdentifier()
                .setSystem("urn:medihelp:user")
                .setValue(userId.toString());
        bundle.addEntry()
                .setFullUrl("urn:uuid:" + userId)
                .setResource(patient);

        // Observations: vitals
        List<Vital> vitals = vitalRepository
                .findByUserIdOrderByRecordedAtDesc(userId, PageRequest.of(0, 200))
                .getContent();
        for (Vital v : vitals) {
            bundle.addEntry()
                    .setFullUrl("urn:uuid:" + v.getId())
                    .setResource(buildVitalObservation(v, patientRef));
        }

        // Observations: mood entries (encrypted journal text omitted; only the
        // 1-5 scale + sleep + exercise — the parts safe to export to a
        // hospital EMR).
        List<MoodEntry> moods = moodEntryRepository
                .findByUserIdOrderByRecordedAtDesc(userId.toString());
        for (MoodEntry m : moods) {
            bundle.addEntry()
                    .setFullUrl("urn:uuid:mood-" + m.getId())
                    .setResource(buildMoodObservation(m, patientRef));
        }

        // DocumentReference: uploaded test reports, doctor notes, etc.
        List<HealthRecord> records = healthRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (HealthRecord r : records) {
            bundle.addEntry()
                    .setFullUrl("urn:uuid:doc-" + r.getId())
                    .setResource(buildDocumentReference(r, patientRef));
        }

        log.info("FHIR export for user {}: {} resources in bundle (vitals={}, mood={}, docs={})",
                userId, bundle.getEntry().size(), vitals.size(), moods.size(), records.size());
        return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
    }

    private Observation buildVitalObservation(Vital vital, Reference patientRef) {
        Observation obs = new Observation();
        obs.setId(vital.getId().toString());
        obs.setStatus(Observation.ObservationStatus.FINAL);

        obs.addCategory().addCoding()
                .setSystem(CATEGORY_SYSTEM)
                .setCode("vital-signs")
                .setDisplay("Vital Signs");

        String code    = LOINC_CODES.getOrDefault(vital.getType(), "unknown");
        String display = LOINC_DISPLAY.getOrDefault(vital.getType(), vital.getType());
        obs.getCode().addCoding()
                .setSystem(LOINC_SYSTEM)
                .setCode(code)
                .setDisplay(display);

        obs.setValue(new Quantity()
                .setValue(BigDecimal.valueOf(vital.getValue()))
                .setUnit(vital.getUnit())
                .setSystem(UCUM_SYSTEM));

        if (vital.getRecordedAt() != null) {
            obs.setEffective(new DateTimeType(Date.from(vital.getRecordedAt())));
        }
        obs.setSubject(patientRef);
        return obs;
    }

    /**
     * LOINC 75275-8 ("Mental status assessment panel") is the closest standard
     * code for a self-reported mood scale. We don't export the encrypted
     * journalText — the patient consented to AES-256 storage on our side, not
     * to plaintext export. Numeric mood + sleep/exercise are fine.
     */
    private Observation buildMoodObservation(MoodEntry m, Reference patientRef) {
        Observation obs = new Observation();
        obs.setId("mood-" + m.getId());
        obs.setStatus(Observation.ObservationStatus.FINAL);

        obs.addCategory().addCoding()
                .setSystem(CATEGORY_SYSTEM)
                .setCode("survey")
                .setDisplay("Survey");

        obs.getCode().addCoding()
                .setSystem(LOINC_SYSTEM)
                .setCode("75275-8")
                .setDisplay("Mental status assessment panel");

        obs.setValue(new Quantity()
                .setValue(BigDecimal.valueOf(m.getMood()))
                .setUnit("/5")
                .setSystem(UCUM_SYSTEM));

        if (m.getSleepHours() != null) {
            obs.addComponent()
                    .setCode(new CodeableConcept().addCoding(
                            new Coding(LOINC_SYSTEM, "93832-4", "Sleep duration")))
                    .setValue(new Quantity()
                            .setValue(BigDecimal.valueOf(m.getSleepHours()))
                            .setUnit("h").setSystem(UCUM_SYSTEM));
        }
        if (m.getExerciseMinutes() != null) {
            obs.addComponent()
                    .setCode(new CodeableConcept().addCoding(
                            new Coding(LOINC_SYSTEM, "82290-8", "Total daily exercise duration")))
                    .setValue(new Quantity()
                            .setValue(BigDecimal.valueOf(m.getExerciseMinutes()))
                            .setUnit("min").setSystem(UCUM_SYSTEM));
        }

        if (m.getRecordedAt() != null) {
            obs.setEffective(new DateTimeType(Date.from(m.getRecordedAt())));
        }
        obs.setSubject(patientRef);
        return obs;
    }

    private DocumentReference buildDocumentReference(HealthRecord r, Reference patientRef) {
        DocumentReference doc = new DocumentReference();
        doc.setId(r.getId().toString());
        doc.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);

        // Map our categories to LOINC document type codes
        String typeCode = switch (r.getCategory() == null ? "OTHER" : r.getCategory()) {
            case "TEST_REPORT"  -> "11502-2";  // Laboratory report
            case "DOCTOR_NOTE"  -> "34109-9";  // Note
            case "PRESCRIPTION" -> "57833-6";  // Prescription for medication
            case "IMAGING"      -> "18748-4";  // Diagnostic imaging study
            default              -> "34108-1"; // Outpatient note (generic clinical document)
        };
        doc.setType(new CodeableConcept().addCoding(
                new Coding(LOINC_SYSTEM, typeCode, r.getCategory())));

        doc.setSubject(patientRef);
        if (r.getCreatedAt() != null) doc.setDate(Date.from(r.getCreatedAt()));
        if (r.getDescription() != null) doc.setDescription(r.getDescription());

        // Inline the file content if the record has a file attached.
        if (r.getFileContentBase64() != null && !r.getFileContentBase64().isEmpty()) {
            DocumentReference.DocumentReferenceContentComponent content = doc.addContent();
            Attachment att = new Attachment()
                    .setContentType(r.getFileType())
                    .setTitle(r.getFileName() != null ? r.getFileName() : r.getTitle());
            try {
                att.setData(Base64.getDecoder().decode(r.getFileContentBase64()));
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping file attachment for record {} — not valid base64", r.getId());
            }
            if (r.getFileSize() != null) att.setSize(r.getFileSize().intValue());
            content.setAttachment(att);
        }

        // Author = the doctor who created the record (if known)
        if (r.getDoctorName() != null && !r.getDoctorName().isBlank()) {
            doc.addAuthor(new Reference().setDisplay(r.getDoctorName()));
        }

        return doc;
    }
}
