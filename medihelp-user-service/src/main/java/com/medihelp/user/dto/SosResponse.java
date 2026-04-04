package com.medihelp.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SosResponse {
    private String message;
    private int contactsNotified;
    private List<String> contactNames;
    private String shareableLink;
    private Instant expiresAt;
    private Instant triggeredAt;
}
