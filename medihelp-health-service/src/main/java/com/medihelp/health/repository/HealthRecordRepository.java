package com.medihelp.health.repository;

import com.medihelp.health.entity.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, UUID> {
    List<HealthRecord> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<HealthRecord> findByUserIdAndCategory(UUID userId, String category);
    Optional<HealthRecord> findByIdAndUserId(UUID id, UUID userId);
}
