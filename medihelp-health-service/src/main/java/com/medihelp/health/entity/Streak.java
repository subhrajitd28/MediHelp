package com.medihelp.health.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "streaks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "type"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Streak {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;

    @Builder.Default
    private Integer currentCount = 0;

    @Builder.Default
    private Integer longestCount = 0;

    private LocalDate lastActivityDate;

    @Builder.Default
    private boolean streakFreezeUsed = false;
}
