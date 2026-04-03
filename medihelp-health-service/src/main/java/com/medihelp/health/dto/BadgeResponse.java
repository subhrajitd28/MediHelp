package com.medihelp.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeResponse {
    private UUID badgeId;
    private String name;
    private String description;
    private String category;
    private String iconUrl;
    private Instant earnedAt;
}
