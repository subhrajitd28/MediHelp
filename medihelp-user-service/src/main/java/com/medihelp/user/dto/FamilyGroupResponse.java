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
public class FamilyGroupResponse {
    private UUID id;
    private String name;
    private UUID createdByUserId;
    private Instant createdAt;
}
