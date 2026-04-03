package com.medihelp.health.service;

import com.medihelp.health.dto.BadgeResponse;
import com.medihelp.health.entity.Streak;
import com.medihelp.health.entity.UserBadge;
import com.medihelp.health.entity.Badge;
import com.medihelp.health.repository.BadgeRepository;
import com.medihelp.health.repository.StreakRepository;
import com.medihelp.health.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final StreakRepository streakRepository;

    @Transactional
    public List<BadgeResponse> checkAndAwardBadges(UUID userId) {
        List<BadgeResponse> newBadges = new ArrayList<>();
        List<Streak> streaks = streakRepository.findByUserId(userId);

        for (Streak streak : streaks) {
            if (streak.getLongestCount() >= 7) {
                awardBadge(userId, "First Week Warrior").ifPresent(newBadges::add);
            }
            if (streak.getLongestCount() >= 30) {
                awardBadge(userId, "30-Day Streak").ifPresent(newBadges::add);
            }
        }

        boolean hasVitalsStreak = streaks.stream()
                .anyMatch(s -> "VITALS".equals(s.getType()) && s.getLongestCount() >= 14);
        if (hasVitalsStreak) {
            awardBadge(userId, "Vitals Veteran").ifPresent(newBadges::add);
        }

        return newBadges;
    }

    private java.util.Optional<BadgeResponse> awardBadge(UUID userId, String badgeName) {
        return badgeRepository.findByName(badgeName)
                .filter(badge -> !userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId()))
                .map(badge -> {
                    UserBadge userBadge = UserBadge.builder()
                            .userId(userId)
                            .badge(badge)
                            .build();
                    userBadge = userBadgeRepository.save(userBadge);
                    log.info("Awarded badge '{}' to user {}", badgeName, userId);
                    return toBadgeResponse(badge, userBadge.getEarnedAt());
                });
    }

    public List<BadgeResponse> getUserBadges(UUID userId) {
        return userBadgeRepository.findByUserId(userId).stream()
                .map(ub -> toBadgeResponse(ub.getBadge(), ub.getEarnedAt()))
                .toList();
    }

    private BadgeResponse toBadgeResponse(Badge b, java.time.Instant earnedAt) {
        return BadgeResponse.builder()
                .badgeId(b.getId())
                .name(b.getName())
                .description(b.getDescription())
                .category(b.getCategory())
                .iconUrl(b.getIconUrl())
                .earnedAt(earnedAt)
                .build();
    }
}
