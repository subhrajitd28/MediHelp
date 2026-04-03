package com.medihelp.prescription.service;

import com.medihelp.common.exception.ResourceNotFoundException;
import com.medihelp.prescription.dto.AdherenceResponse;
import com.medihelp.prescription.dto.MedicationLogRequest;
import com.medihelp.prescription.dto.MedicationLogResponse;
import com.medihelp.prescription.entity.Medication;
import com.medihelp.prescription.entity.MedicationLog;
import com.medihelp.prescription.repository.MedicationLogRepository;
import com.medihelp.prescription.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicationLogService {

    private final MedicationLogRepository logRepository;
    private final MedicationRepository medicationRepository;

    @Transactional
    public MedicationLogResponse logMedication(UUID userId, MedicationLogRequest request) {
        Medication medication = medicationRepository.findByIdAndUserId(request.getMedicationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", "id", request.getMedicationId()));

        MedicationLog log = MedicationLog.builder()
                .medication(medication)
                .userId(userId)
                .scheduledTime(Instant.now())
                .status(request.getStatus())
                .takenAt("TAKEN".equals(request.getStatus()) ? (request.getTakenAt() != null ? request.getTakenAt() : Instant.now()) : null)
                .notes(request.getNotes())
                .build();
        log = logRepository.save(log);
        return toResponse(log);
    }

    public AdherenceResponse getAdherence(UUID userId, Instant from, Instant to) {
        if (from == null) from = Instant.now().minus(7, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();

        long taken = logRepository.countByUserIdAndStatusAndScheduledTimeBetween(userId, "TAKEN", from, to);
        long missed = logRepository.countByUserIdAndStatusAndScheduledTimeBetween(userId, "MISSED", from, to);
        long skipped = logRepository.countByUserIdAndStatusAndScheduledTimeBetween(userId, "SKIPPED", from, to);
        long total = taken + missed + skipped;

        return AdherenceResponse.builder()
                .totalDoses(total)
                .takenCount(taken)
                .missedCount(missed)
                .skippedCount(skipped)
                .adherencePercentage(total > 0 ? Math.round(taken * 10000.0 / total) / 100.0 : 0)
                .period(from + " to " + to)
                .build();
    }

    public List<MedicationLogResponse> getLogs(UUID userId, Instant from, Instant to) {
        if (from == null) from = Instant.now().minus(7, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();
        return logRepository.findByUserIdAndScheduledTimeBetween(userId, from, to).stream()
                .map(this::toResponse).toList();
    }

    private MedicationLogResponse toResponse(MedicationLog l) {
        return MedicationLogResponse.builder()
                .id(l.getId()).medicationId(l.getMedication().getId())
                .drugName(l.getMedication().getDrugName()).status(l.getStatus())
                .scheduledTime(l.getScheduledTime()).takenAt(l.getTakenAt())
                .notes(l.getNotes()).createdAt(l.getCreatedAt()).build();
    }
}
