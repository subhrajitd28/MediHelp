package com.medihelp.health.service;

import com.medihelp.health.dto.StreakResponse;
import com.medihelp.health.entity.Streak;
import com.medihelp.health.repository.StreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private final StreakRepository streakRepository;

    @Transactional
    public StreakResponse updateStreak(UUID userId, String type) {
        Streak streak = streakRepository.findByUserIdAndType(userId, type)
                .orElse(Streak.builder()
                        .userId(userId)
                        .type(type)
                        .currentCount(0)
                        .longestCount(0)
                        .build());

        LocalDate today = LocalDate.now();
        LocalDate lastActivity = streak.getLastActivityDate();

        if (lastActivity == null) {
            streak.setCurrentCount(1);
        } else if (lastActivity.equals(today)) {
            // Already recorded today, no change
            return toResponse(streak);
        } else if (lastActivity.equals(today.minusDays(1))) {
            streak.setCurrentCount(streak.getCurrentCount() + 1);
        } else if (lastActivity.equals(today.minusDays(2)) && !streak.isStreakFreezeUsed()) {
            // Use streak freeze
            streak.setCurrentCount(streak.getCurrentCount() + 1);
            streak.setStreakFreezeUsed(true);
        } else {
            streak.setCurrentCount(1);
            streak.setStreakFreezeUsed(false);
        }

        streak.setLastActivityDate(today);
        if (streak.getCurrentCount() > streak.getLongestCount()) {
            streak.setLongestCount(streak.getCurrentCount());
        }

        streak = streakRepository.save(streak);
        return toResponse(streak);
    }

    public List<StreakResponse> getStreaks(UUID userId) {
        return streakRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private StreakResponse toResponse(Streak s) {
        return StreakResponse.builder()
                .type(s.getType())
                .currentCount(s.getCurrentCount())
                .longestCount(s.getLongestCount())
                .lastActivityDate(s.getLastActivityDate())
                .streakFreezeUsed(s.isStreakFreezeUsed())
                .build();
    }
}
