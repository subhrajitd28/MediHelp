package com.medihelp.health.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "mood_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodEntry {

    @Id
    private String id;

    @Indexed
    private String userId;

    private Integer mood; // 1-5

    private String journalText;

    private List<String> tags;

    private Double sleepHours;
    private Integer exerciseMinutes;

    @Indexed
    private Instant recordedAt;
}
