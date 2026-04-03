package com.medihelp.health.repository;

import com.medihelp.health.document.MoodEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MoodEntryRepository extends MongoRepository<MoodEntry, String> {
    List<MoodEntry> findByUserIdOrderByRecordedAtDesc(String userId);
    List<MoodEntry> findByUserIdAndRecordedAtBetween(String userId, Instant from, Instant to);
}
