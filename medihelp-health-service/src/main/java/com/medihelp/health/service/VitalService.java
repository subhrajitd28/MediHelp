package com.medihelp.health.service;

import com.medihelp.common.dto.PagedResponse;
import com.medihelp.health.dto.VitalRequest;
import com.medihelp.health.dto.VitalResponse;
import com.medihelp.health.dto.VitalTrendResponse;
import com.medihelp.health.entity.Vital;
import com.medihelp.health.repository.VitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VitalService {

    private final VitalRepository vitalRepository;
    private final AnomalyDetectionService anomalyDetectionService;
    private final StreakService streakService;

    @Transactional
    public VitalResponse recordVital(UUID userId, VitalRequest request) {
        Vital vital = Vital.builder()
                .userId(userId)
                .type(request.getType())
                .value(request.getValue())
                .unit(request.getUnit())
                .source(request.getSource() != null ? request.getSource() : "MANUAL")
                .notes(request.getNotes())
                .recordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now())
                .build();
        vital = vitalRepository.save(vital);

        // Check for anomaly
        anomalyDetectionService.checkAnomaly(userId, request.getType(), request.getValue());

        // Update vitals streak
        streakService.updateStreak(userId, "VITALS");

        return toResponse(vital);
    }

    public PagedResponse<VitalResponse> getVitals(UUID userId, String type, Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Vital> vitalsPage;

        if (type != null) {
            vitalsPage = vitalRepository.findByUserIdAndTypeOrderByRecordedAtDesc(userId, type, pageable);
        } else {
            vitalsPage = vitalRepository.findByUserIdOrderByRecordedAtDesc(userId, pageable);
        }

        List<VitalResponse> content = vitalsPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PagedResponse.<VitalResponse>builder()
                .content(content)
                .page(vitalsPage.getNumber())
                .size(vitalsPage.getSize())
                .totalElements(vitalsPage.getTotalElements())
                .totalPages(vitalsPage.getTotalPages())
                .last(vitalsPage.isLast())
                .build();
    }

    public Map<String, VitalResponse> getLatestVitals(UUID userId) {
        String[] types = {"HEART_RATE", "BLOOD_PRESSURE_SYSTOLIC", "BLOOD_PRESSURE_DIASTOLIC",
                "BLOOD_SUGAR", "TEMPERATURE", "OXYGEN_SATURATION", "WEIGHT", "STEPS", "SLEEP_HOURS"};

        Map<String, VitalResponse> latest = new LinkedHashMap<>();
        for (String type : types) {
            vitalRepository.findFirstByUserIdAndTypeOrderByRecordedAtDesc(userId, type)
                    .ifPresent(v -> latest.put(type, toResponse(v)));
        }
        return latest;
    }

    public VitalTrendResponse getVitalTrends(UUID userId, String type, int days) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant to = Instant.now();
        List<Vital> vitals = vitalRepository.findByUserIdAndTypeAndRecordedAtBetween(userId, type, from, to);

        if (vitals.isEmpty()) {
            return VitalTrendResponse.builder()
                    .type(type)
                    .readingsCount(0L)
                    .periodDays(days)
                    .build();
        }

        DoubleSummaryStatistics stats = vitals.stream()
                .mapToDouble(Vital::getValue)
                .summaryStatistics();

        return VitalTrendResponse.builder()
                .type(type)
                .averageValue(Math.round(stats.getAverage() * 100.0) / 100.0)
                .minValue(stats.getMin())
                .maxValue(stats.getMax())
                .readingsCount(stats.getCount())
                .periodDays(days)
                .build();
    }

    private VitalResponse toResponse(Vital v) {
        return VitalResponse.builder()
                .id(v.getId())
                .type(v.getType())
                .value(v.getValue())
                .unit(v.getUnit())
                .source(v.getSource())
                .notes(v.getNotes())
                .recordedAt(v.getRecordedAt())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
