package com.medihelp.user.dto;

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
public class FamilyMemberResponse {
    private UUID id;
    private UUID familyGroupId;
    private UUID userId;
    private String role;
    private Instant addedAt;
}
