package com.medihelp.health.service;

import com.medihelp.common.dto.PagedResponse;
import com.medihelp.health.dto.HealthScoreResponse;
import com.medihelp.health.entity.HealthScore;
import com.medihelp.health.entity.Vital;
import com.medihelp.health.repository.HealthScoreRepository;
import com.medihelp.health.repository.MoodEntryRepository;
import com.medihelp.health.repository.VitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthScoreService {

    private final HealthScoreRepository healthScoreRepository;
    private final VitalRepository vitalRepository;
    private final MoodEntryRepository moodEntryRepository;

    @Transactional
    public HealthScoreResponse calculateScore(UUID userId) {
        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayEnd = todayStart.plus(1, ChronoUnit.DAYS);

        // Vitals score (0-20): did user log any vitals today?
        List<Vital> todayVitals = vitalRepository.findByUserIdAndRecordedAtBetween(userId, todayStart, todayEnd);
        int vitalsScore = Math.min(20, todayVitals.size() * 5);

        // Mood score (0-10): did user journal today?
        long moodCount = moodEntryRepository.findByUserIdAndRecordedAtBetween(userId.toString(), todayStart, todayEnd).size();
        int moodScore = moodCount > 0 ? 10 : 0;

        // Placeholder scores (will be populated when prescription service integration is done)
        int medicationScore = 0;
        int exerciseScore = 0;
        int dietScore = 0;
        int appointmentScore = 0;

        int totalScore = vitalsScore + moodScore + medicationScore + exerciseScore + dietScore + appointmentScore;

        HealthScore score = HealthScore.builder()
                .userId(userId)
                .totalScore(totalScore)
                .medicationScore(medicationScore)
                .vitalsScore(vitalsScore)
                .exerciseScore(exerciseScore)
                .dietScore(dietScore)
                .appointmentScore(appointmentScore)
                .moodScore(moodScore)
                .calculatedAt(Instant.now())
                .build();
        score = healthScoreRepository.save(score);
        return toResponse(score);
    }

    public HealthScoreResponse getLatestScore(UUID userId) {
        return healthScoreRepository.findFirstByUserIdOrderByCalculatedAtDesc(userId)
                .map(this::toResponse)
                .orElse(HealthScoreResponse.builder().totalScore(0).calculatedAt(Instant.now()).build());
    }

    public PagedResponse<HealthScoreResponse> getScoreHistory(UUID userId, int page, int size) {
        Page<HealthScore> scoresPage = healthScoreRepository.findByUserIdOrderByCalculatedAtDesc(userId, PageRequest.of(page, size));
        List<HealthScoreResponse> content = scoresPage.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PagedResponse.<HealthScoreResponse>builder()
                .content(content)
                .page(scoresPage.getNumber())
                .size(scoresPage.getSize())
                .totalElements(scoresPage.getTotalElements())
                .totalPages(scoresPage.getTotalPages())
                .last(scoresPage.isLast())
                .build();
    }

    private HealthScoreResponse toResponse(HealthScore s) {
        return HealthScoreResponse.builder()
                .id(s.getId())
                .totalScore(s.getTotalScore())
                .medicationScore(s.getMedicationScore())
                .vitalsScore(s.getVitalsScore())
                .exerciseScore(s.getExerciseScore())
                .dietScore(s.getDietScore())
                .appointmentScore(s.getAppointmentScore())
                .moodScore(s.getMoodScore())
                .calculatedAt(s.getCalculatedAt())
                .build();
    }
}
