package com.medihelp.health.service;

import com.medihelp.health.document.MoodEntry;
import com.medihelp.health.dto.MoodEntryRequest;
import com.medihelp.health.dto.MoodEntryResponse;
import com.medihelp.health.repository.MoodEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoodService {

    private final MoodEntryRepository moodEntryRepository;
    private final StreakService streakService;

    public MoodEntryResponse addMoodEntry(UUID userId, MoodEntryRequest request) {
        MoodEntry entry = MoodEntry.builder()
                .userId(userId.toString())
                .mood(request.getMood())
                .journalText(request.getJournalText())
                .tags(request.getTags())
                .sleepHours(request.getSleepHours())
                .exerciseMinutes(request.getExerciseMinutes())
                .recordedAt(Instant.now())
                .build();
        entry = moodEntryRepository.save(entry);

        streakService.updateStreak(userId, "MOOD");

        return toResponse(entry);
    }

    public List<MoodEntryResponse> getMoodEntries(UUID userId, Instant from, Instant to) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();
        return moodEntryRepository.findByUserIdAndRecordedAtBetween(userId.toString(), from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    private MoodEntryResponse toResponse(MoodEntry m) {
        return MoodEntryResponse.builder()
                .id(m.getId())
                .mood(m.getMood())
                .journalText(m.getJournalText())
                .tags(m.getTags())
                .sleepHours(m.getSleepHours())
                .exerciseMinutes(m.getExerciseMinutes())
                .recordedAt(m.getRecordedAt())
                .build();
    }
}
